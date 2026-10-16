package cl.duoc.vidasalud.bff.service;

import cl.duoc.vidasalud.bff.config.RabbitMQConstants;
import cl.duoc.vidasalud.bff.dto.amqp.*;
import cl.duoc.vidasalud.bff.exception.QueueNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Servicio de administración para RabbitMQ utilizando las APIs oficiales de Spring AMQP (AmqpAdmin y RabbitTemplate).
 * Permite gestionar colas, exchanges, bindings, purgar, eliminar y publicar mensajes de prueba.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Declara una nueva cola en RabbitMQ con soporte para configuración de Dead Letter Exchange.
     *
     * @param request Parámetros de creación de la cola
     * @return Nombre de la cola declarada
     */
    public String createQueue(CreateQueueRequest request) {
        validateName(request.getName(), "cola");

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
                request.getName().trim(),
                request.isDurable(),
                request.isExclusive(),
                request.isAutoDelete(),
                arguments
        );

        log.info("Declarando cola administrativamente: [{}] con argumentos DLQ: {}", queue.getName(), arguments);
        amqpAdmin.declareQueue(queue);
        return queue.getName();
    }

    /**
     * Obtiene estadísticas de una cola mediante el protocolo AMQP nativo.
     * Consulta cantidad de mensajes y consumidores activos.
     *
     * @param queueName Nombre de la cola
     * @return DTO con estadísticas
     */
    public QueueStatsResponse getQueueStats(String queueName) {
        validateName(queueName, "cola");

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
     *
     * @param queueName Nombre de la cola a eliminar
     * @return true si la cola fue eliminada
     */
    public boolean deleteQueue(String queueName) {
        validateName(queueName, "cola");

        QueueInformation queueInfo = amqpAdmin.getQueueInfo(queueName.trim());
        if (queueInfo == null) {
            throw new QueueNotFoundException(queueName);
        }

        log.warn("Eliminando cola administrativamente: [{}]", queueName);
        return amqpAdmin.deleteQueue(queueName.trim());
    }

    /**
     * Purga todos los mensajes encolados de una cola.
     *
     * @param queueName Nombre de la cola a purgar
     * @return Cantidad de mensajes purgados
     */
    public int purgeQueue(String queueName) {
        validateName(queueName, "cola");

        QueueInformation queueInfo = amqpAdmin.getQueueInfo(queueName.trim());
        if (queueInfo == null) {
            throw new QueueNotFoundException(queueName);
        }

        int messageCount = queueInfo.getMessageCount();
        log.warn("Purgando {} mensajes de la cola: [{}]", messageCount, queueName);
        amqpAdmin.purgeQueue(queueName.trim(), false);
        return messageCount;
    }

    /**
     * Declara un exchange de tipo direct, topic o fanout.
     *
     * @param request Datos del exchange
     */
    public void createExchange(CreateExchangeRequest request) {
        validateName(request.getName(), "exchange");

        String type = request.getType().trim().toLowerCase();
        Exchange exchange;

        switch (type) {
            case "direct" -> exchange = new DirectExchange(request.getName().trim(), request.isDurable(), request.isAutoDelete());
            case "topic" -> exchange = new TopicExchange(request.getName().trim(), request.isDurable(), request.isAutoDelete());
            case "fanout" -> exchange = new FanoutExchange(request.getName().trim(), request.isDurable(), request.isAutoDelete());
            default -> throw new IllegalArgumentException("Tipo de exchange no soportado: " + request.getType());
        }

        log.info("Declarando exchange administrativamente: [{}] de tipo [{}]", request.getName(), type);
        amqpAdmin.declareExchange(exchange);
    }

    /**
     * Declara un binding entre un exchange y una cola o entre dos exchanges.
     *
     * @param request Datos del binding
     */
    public void createBinding(CreateBindingRequest request) {
        validateName(request.getDestination(), "destino");
        validateName(request.getExchange(), "exchange");

        String destination = request.getDestination().trim();
        String exchange = request.getExchange().trim();
        String routingKey = request.getRoutingKey() != null ? request.getRoutingKey().trim() : "";
        Map<String, Object> arguments = request.getArguments() != null ? request.getArguments() : new HashMap<>();

        Binding.DestinationType destinationType = "EXCHANGE".equalsIgnoreCase(request.getDestinationType())
                ? Binding.DestinationType.EXCHANGE
                : Binding.DestinationType.QUEUE;

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
     * Publica un mensaje de prueba en el exchange y routing key solicitados.
     *
     * @param request Datos de la publicación
     */
    public void testPublish(TestPublishRequest request) {
        validateName(request.getExchange(), "exchange");

        String exchange = request.getExchange().trim();
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

    private void validateName(String name, String entityType) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de " + entityType + " no puede ser nulo ni vacío");
        }
    }
}
