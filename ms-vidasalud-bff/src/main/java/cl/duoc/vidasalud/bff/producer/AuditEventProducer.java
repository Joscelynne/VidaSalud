package cl.duoc.vidasalud.bff.producer;

import cl.duoc.vidasalud.bff.config.RabbitMQProperties;
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
    private final RabbitMQProperties rabbitMQ;

    /**
     * Publica un evento de auditoría con la routing key predeterminada: vidasalud.notificacion.audit.
     *
     * @param event Datos del evento de auditoría
     */
    public void publishNotificationAudit(NotificationAuditEvent event) {
        publishNotificationAudit(rabbitMQ.getRoutingKeys().getNotificationAuditDefault(), event);
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
                : rabbitMQ.getRoutingKeys().getNotificationAuditDefault();

        log.info("Publicando NotificationAuditEvent hacia [{}], routingKey [{}], eventId [{}]",
                rabbitMQ.getExchanges().getTopic(),
                effectiveRoutingKey,
                event.getEventId());

        rabbitTemplate.convertAndSend(
                rabbitMQ.getExchanges().getTopic(),
                effectiveRoutingKey,
                event
        );
    }
}
