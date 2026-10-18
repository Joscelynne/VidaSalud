package cl.duoc.vidasalud.bff.service;

import cl.duoc.vidasalud.bff.config.RabbitMQConstants;
import cl.duoc.vidasalud.bff.config.VidaSaludTopologyConfig;
import cl.duoc.vidasalud.bff.dto.amqp.*;
import cl.duoc.vidasalud.bff.exception.AmqpValidationException;
import cl.duoc.vidasalud.bff.exception.QueueNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Servicio de administración para RabbitMQ utilizando las APIs oficiales de Spring AMQP.
 * Implementa validaciones robustas según la rúbrica EP3:
 * - Rechaza nombres vacíos o en blanco
 * - Rechaza prefijo reservado 'amq.' de RabbitMQ
 * - Protege la topología esencial de VidaSalud contra eliminación/modificación
 * - Proporciona mensajes de error claros y códigos HTTP apropiados
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitAdminService {

    private static final String AMQ_RESERVED_PREFIX = "amq.";
    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;
    private final VidaSaludTopologyConfig topologyConfig;

    /**
     * Declara una nueva cola en RabbitMQ con soporte para configuración de Dead Letter Exchange.
     * Valida que el nombre no use prefijo reservado y no sea parte de la topología esencial.
     *
     * @param request Parámetros de creación de la cola
     * @return Nombre de la cola declarada
     * @throws AmqpValidationException si el nombre es inválido o está protegido
     */
    public String createQueue(CreateQueueRequest request) {
        validateResourceName(request.getName(), "cola");
        
        String queueName = request.getName().trim();
        validateNotReservedPrefix(queueName);
        // Permitir crear si no existe, pero no sobrescribir colas protegidas
        // La creación en RabbitMQ es idempotente, pero logueamos advertencia si coincide
        if (topologyConfig.isProtectedQueue(queueName)) {
            log.warn("Intento de re-declarar cola protegida [{}], operación permitida pero sin cambios", queueName);
        }

        Map<String, Object> arguments = new HashMap<>();

        if (request.getDeadLetterExchange() != null && !request.getDeadLetterExchange().isBlank()) {
            arguments.put(RabbitMQConstants.X_DEAD_LETTER_EXCHANGE, request.getDeadLetterExchange().trim());
        }

        if (request.getDeadLetterRoutingKey() != null && !request.getDeadLetterRoutingKey().isBlank()) {
            arguments.put(RabbitMQConstants.X_DEAD_LETTER_ROUTING_KEY, request.getDeadLetterRoutingKey().trim());
        }

        if (request.getMessageTtl() != null && request.getMessageTtl() > 0) {
            arguments.put("x-message-ttl", request.getMessageTtl());
        }

        if (request.getMaxLength() != null && request.getMaxLength() > 0) {
            arguments.put("x-max-length", request.getMaxLength());
        }

        Queue queue = new Queue(
                queueName,
                request.isDurable(),
                request.isExclusive(),
                request.isAutoDelete(),
                arguments
        );

        log.info("Declarando cola administrativamente: [{}] con argumentos DLQ: {}", queue.getName(), arguments);
        String declaredName = amqpAdmin.declareQueue(queue);
        
        // Verificar que RabbitMQ realmente creó la cola (puede fallar silenciosamente con prefijo amq.)
        if (declaredName == null) {
            // Si el nombre empieza con amq., sabemos que fue por prefijo reservado
            if (queueName.toLowerCase().startsWith(AMQ_RESERVED_PREFIX)) {
                throw AmqpValidationException.reservedPrefix(queueName);
            }
            throw AmqpValidationException.brokerRejected(queueName, "cola");
        }
        
        // Verificación adicional: confirmar que la cola existe
        QueueInformation info = amqpAdmin.getQueueInfo(queueName);
        if (info == null) {
            log.error("RabbitMQ no creó la cola [{}] aunque no lanzó excepción", queueName);
            // Si el nombre empieza con amq., fue rechazado por prefijo reservado
            if (queueName.toLowerCase().startsWith(AMQ_RESERVED_PREFIX)) {
                throw AmqpValidationException.reservedPrefix(queueName);
            }
            throw AmqpValidationException.brokerRejected(queueName, "cola");
        }
        
        return declaredName;
    }

    /**
     * Obtiene estadísticas de una cola mediante el protocolo AMQP nativo.
     *
     * @param queueName Nombre de la cola
     * @return DTO con estadísticas
     * @throws AmqpValidationException si el nombre es inválido
     * @throws QueueNotFoundException si la cola no existe
     */
    public QueueStatsResponse getQueueStats(String queueName) {
        validateResourceName(queueName, "cola");

        QueueInformation queueInfo = amqpAdmin.getQueueInfo(queueName.trim());
        if (queueInfo == null) {
            throw new QueueNotFoundException(queueName);
        }

        return QueueStatsResponse.builder()
                .name(queueInfo.getName())
                .exists(true)
                .messageCount(queueInfo.getMessageCount())
                .consumerCount(queueInfo.getConsumerCount())
                .apiSource("Spring AMQP RabbitAdmin / queue.declare-passive")
                .details("Estadísticas obtenidas vía protocolo AMQP 0-9-1. Métricas de memoria y tasas requieren Management HTTP Plugin.")
                .build();
    }

    /**
     * Elimina una cola existente en el broker.
     * Rechaza eliminación de colas protegidas de la topología esencial.
     *
     * @param queueName Nombre de la cola a eliminar
     * @return true si la cola fue eliminada
     * @throws AmqpValidationException si la cola está protegida o el nombre es inválido
     * @throws QueueNotFoundException si la cola no existe
     */
    public boolean deleteQueue(String queueName) {
        validateResourceName(queueName, "cola");
        
        String trimmedName = queueName.trim();
        validateNotReservedPrefix(trimmedName);
        validateNotProtectedQueue(trimmedName, "eliminar");

        QueueInformation queueInfo = amqpAdmin.getQueueInfo(trimmedName);
        if (queueInfo == null) {
            throw new QueueNotFoundException(queueName);
        }

        log.warn("Eliminando cola administrativamente: [{}]", trimmedName);
        return amqpAdmin.deleteQueue(trimmedName);
    }

    /**
     * Elimina un exchange existente en el broker.
     * Rechaza eliminación de exchanges protegidos de la topología esencial.
     *
     * @param exchangeName Nombre del exchange a eliminar
     * @throws AmqpValidationException si el exchange está protegido o el nombre es inválido
     */
    public void deleteExchange(String exchangeName) {
        validateResourceName(exchangeName, "exchange");
        
        String trimmedName = exchangeName.trim();
        validateNotReservedPrefix(trimmedName);
        validateNotProtectedExchange(trimmedName, "eliminar");

        log.warn("Eliminando exchange administrativamente: [{}]", trimmedName);
        amqpAdmin.deleteExchange(trimmedName);
    }

    /**
     * Purga todos los mensajes encolados de una cola.
     * Permite purgar colas protegidas (operación de mantenimiento, no destructiva).
     *
     * @param queueName Nombre de la cola a purgar
     * @return Cantidad de mensajes purgados
     * @throws AmqpValidationException si el nombre es inválido
     * @throws QueueNotFoundException si la cola no existe
     */
    public int purgeQueue(String queueName) {
        String trimmedName = queueName.trim();
        validateResourceName(trimmedName, "cola");

        QueueInformation queueInfo = amqpAdmin.getQueueInfo(trimmedName);
        if (queueInfo == null) {
            throw new QueueNotFoundException(queueName);
        }

        int messageCount = queueInfo.getMessageCount();
        log.warn("Purgando {} mensajes de la cola: [{}]", messageCount, trimmedName);
        amqpAdmin.purgeQueue(trimmedName, false);
        return messageCount;
    }

    /**
     * Declara un exchange de tipo direct, topic o fanout.
     * Valida que el nombre no use prefijo reservado.
     *
     * @param request Datos del exchange
     * @throws AmqpValidationException si el nombre es inválido o el tipo no es soportado
     */
    public void createExchange(CreateExchangeRequest request) {
        validateResourceName(request.getName(), "exchange");
        
        String exchangeName = request.getName().trim();
        validateNotReservedPrefix(exchangeName);

        String type = request.getType().trim().toLowerCase();
        Exchange exchange = switch (type) {
            case "direct" -> new DirectExchange(exchangeName, request.isDurable(), request.isAutoDelete());
            case "topic" -> new TopicExchange(exchangeName, request.isDurable(), request.isAutoDelete());
            case "fanout" -> new FanoutExchange(exchangeName, request.isDurable(), request.isAutoDelete());
            default -> throw AmqpValidationException.unsupportedExchangeType(request.getType());
        };

        log.info("Declarando exchange administrativamente: [{}] de tipo [{}]", exchangeName, type);
        amqpAdmin.declareExchange(exchange);
    }

    /**
     * Declara un binding entre un exchange y una cola o entre dos exchanges.
     *
     * @param request Datos del binding
     * @throws AmqpValidationException si los nombres son inválidos
     */
    public void createBinding(CreateBindingRequest request) {
        String destination = request.getDestination().trim();
        String exchange = request.getExchange().trim();
        
        validateResourceName(destination, "destino");
        validateResourceName(exchange, "exchange");

        String routingKey = request.getRoutingKey() != null ? request.getRoutingKey().trim() : "";
        Map<String, Object> arguments = request.getArguments() != null ? request.getArguments() : new HashMap<>();

        Binding.DestinationType destinationType = parseDestinationType(request.getDestinationType());

        Binding binding = new Binding(
                destination,
                destinationType,
                exchange,
                routingKey,
                arguments
        );

        log.info("Declarando binding administrativamente: destino [{}] ({}) -> exchange [{}] con routingKey [{}]",
                destination, destinationType, exchange, routingKey);
        amqpAdmin.declareBinding(binding);
    }

    /**
     * Elimina un binding usando su identidad completa AMQP.
     *
     * @param request Datos del binding a eliminar
     * @throws AmqpValidationException si los nombres son inválidos
     */
    public void deleteBinding(CreateBindingRequest request) {
        String destination = request.getDestination().trim();
        String exchange = request.getExchange().trim();
        String routingKey = request.getRoutingKey();
        
        validateResourceName(destination, "destino");
        validateResourceName(exchange, "exchange");
        validateResourceName(routingKey, "routing key");

        Binding binding = new Binding(
                destination,
                parseDestinationType(request.getDestinationType()),
                exchange,
                routingKey.trim(),
                request.getArguments() != null ? request.getArguments() : new HashMap<>()
        );
        
        log.warn("Eliminando binding: destino [{}] -> exchange [{}] con routingKey [{}]",
                binding.getDestination(), binding.getExchange(), binding.getRoutingKey());
        amqpAdmin.removeBinding(binding);
    }

    /**
     * Publica un mensaje de prueba en el exchange y routing key solicitados.
     *
     * @param request Datos de la publicación
     * @throws AmqpValidationException si el nombre del exchange es inválido
     */
    public void testPublish(TestPublishRequest request) {
        String exchange = request.getExchange().trim();
        validateResourceName(exchange, "exchange");

        String routingKey = request.getRoutingKey() != null ? request.getRoutingKey().trim() : "";

        log.info("Publicando mensaje de prueba administrativamente hacia exchange [{}] con routingKey [{}]",
                exchange, routingKey);

        rabbitTemplate.convertAndSend(exchange, routingKey, request.getPayload(), message -> {
            if (request.getHeaders() != null) {
                request.getHeaders().forEach((k, v) -> message.getMessageProperties().setHeader(k, v));
            }
            return message;
        });
    }

    // ==================== Métodos de Validación ====================

    /**
     * Valida que el nombre del recurso no sea nulo, vacío, ni contenga solo espacios.
     * También valida que contenga solo caracteres permitidos.
     */
    private void validateResourceName(String name, String resourceType) {
        if (name == null || name.isBlank()) {
            throw AmqpValidationException.emptyName(resourceType);
        }
        
        String trimmed = name.trim();
        if (!VALID_NAME_PATTERN.matcher(trimmed).matches()) {
            throw AmqpValidationException.invalidCharacters(trimmed, resourceType);
        }
    }

    /**
     * Valida que el nombre no use el prefijo reservado 'amq.' de RabbitMQ.
     */
    private void validateNotReservedPrefix(String name) {
        if (name != null && name.toLowerCase().startsWith(AMQ_RESERVED_PREFIX)) {
            throw AmqpValidationException.reservedPrefix(name);
        }
    }

    /**
     * Valida que la cola no pertenezca a la topología esencial de VidaSalud.
     */
    private void validateNotProtectedQueue(String queueName, String operation) {
        if (topologyConfig.isProtectedQueue(queueName)) {
            throw AmqpValidationException.protectedResource(queueName, operation);
        }
    }

    /**
     * Valida que el exchange no pertenezca a la topología esencial de VidaSalud.
     */
    private void validateNotProtectedExchange(String exchangeName, String operation) {
        if (topologyConfig.isProtectedExchange(exchangeName)) {
            throw AmqpValidationException.protectedResource(exchangeName, operation);
        }
    }

    /**
     * Parsea el tipo de destino del binding.
     */
    private Binding.DestinationType parseDestinationType(String destinationType) {
        if (destinationType == null || destinationType.isBlank()) {
            throw AmqpValidationException.invalidDestinationType(destinationType);
        }
        return switch (destinationType.trim().toUpperCase()) {
            case "QUEUE" -> Binding.DestinationType.QUEUE;
            case "EXCHANGE" -> Binding.DestinationType.EXCHANGE;
            default -> throw AmqpValidationException.invalidDestinationType(destinationType);
        };
    }
}
