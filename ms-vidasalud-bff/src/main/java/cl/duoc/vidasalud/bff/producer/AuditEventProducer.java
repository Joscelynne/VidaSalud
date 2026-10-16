package cl.duoc.vidasalud.bff.producer;

import cl.duoc.vidasalud.bff.config.RabbitMQConstants;
import cl.duoc.vidasalud.bff.event.NotificationAuditEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Productor de eventos para notificaciones y auditoría transversal en VidaSalud.
 * Emite mensajes hacia vidasalud.topic.exchange con routing keys bajo el patrón vidasalud.notificacion.*.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publica un evento de auditoría con la routing key predeterminada: vidasalud.notificacion.audit.
     *
     * @param event Datos del evento de auditoría
     */
    public void publishNotificationAudit(NotificationAuditEvent event) {
        publishNotificationAudit(RabbitMQConstants.RK_NOTIFICATION_AUDIT_DEFAULT, event);
    }

    /**
     * Publica un evento de auditoría especificando una routing key del patrón topic vidasalud.notificacion.*.
     *
     * @param routingKey Clave de enrutamiento topic específica
     * @param event      Datos del evento de auditoría
     */
    public void publishNotificationAudit(String routingKey, NotificationAuditEvent event) {
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }

        String effectiveRoutingKey = (routingKey != null && !routingKey.isBlank())
                ? routingKey
                : RabbitMQConstants.RK_NOTIFICATION_AUDIT_DEFAULT;

        log.info("Publicando NotificationAuditEvent hacia [{}], routingKey [{}], eventId [{}]",
                RabbitMQConstants.EXCHANGE_TOPIC,
                effectiveRoutingKey,
                event.getEventId());

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.EXCHANGE_TOPIC,
                effectiveRoutingKey,
                event
        );
    }
}
