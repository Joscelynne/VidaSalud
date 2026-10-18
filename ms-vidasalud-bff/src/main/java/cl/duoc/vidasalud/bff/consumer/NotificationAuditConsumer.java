package cl.duoc.vidasalud.bff.consumer;

import cl.duoc.vidasalud.bff.config.RabbitMQProperties;
import cl.duoc.vidasalud.bff.event.NotificationAuditEvent;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor para eventos de auditoría y notificaciones en 'notificaciones.auditoria.queue'.
 * Implementa confirmación manual ACK/NACK:
 * - basicAck ante procesamiento exitoso.
 * - basicNack(requeue=false) ante errores de validación o fallos para derivar a notificaciones.auditoria.dlq.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationAuditConsumer {

    private final RabbitMQProperties rabbitMQ;

    @RabbitListener(
            queues = "${vidasalud.rabbitmq.queues.notification-audit}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void receiveNotificationAudit(NotificationAuditEvent event,
                                          Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.debug("Mensaje recibido en [{}] con deliveryTag [{}]",
                rabbitMQ.getQueues().getNotificationAudit(), deliveryTag);

        try {
            validateEvent(event);
            processEvent(event);

            // Confirmación manual exitosa
            channel.basicAck(deliveryTag, false);
            log.info("ACK manual confirmado para auditoría [{}] con deliveryTag [{}]",
                    event.getEventId(), deliveryTag);

        } catch (IllegalArgumentException ex) {
            log.error("Validación fallida en auditoría. Ejecutando basicNack (requeue=false) hacia DLQ: {}",
                    ex.getMessage());
            channel.basicNack(deliveryTag, false, false);

        } catch (RuntimeException ex) {
            log.warn("Fallo recuperable al procesar auditoría; se aplicarán reintentos: {}",
                    ex.getMessage(), ex);
            throw ex;
        }
    }

    private void validateEvent(NotificationAuditEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("El evento NotificationAuditEvent no puede ser nulo");
        }
        if (event.getAction() == null || event.getAction().isBlank()) {
            throw new IllegalArgumentException("La acción de auditoría no puede estar vacía");
        }

        // Simulación controlada para pruebas DLQ
        if ("SIMULATE_FAIL".equalsIgnoreCase(event.getAction()) ||
                (event.getDetails() != null && event.getDetails().contains("SIMULATE_FAIL"))) {
            throw new IllegalArgumentException("Fallo simulado para comprobación controlada de Dead Letter Queue");
        }
    }

    private void processEvent(NotificationAuditEvent event) {
        log.info("Registrando evento de auditoría. Acción [{}], recurso [{}], usuario [{}], traza [{}]",
                event.getAction(), event.getResource(), event.getUser(), event.getTraceId());
    }
}
