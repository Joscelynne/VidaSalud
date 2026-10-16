package cl.duoc.vidasalud.bff.producer;

import cl.duoc.vidasalud.bff.config.RabbitMQConstants;
import cl.duoc.vidasalud.bff.event.AppointmentConfirmedEvent;
import cl.duoc.vidasalud.bff.event.AppointmentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Productor de eventos de negocio para el flujo de atenciones médicas (citas).
 * Emite mensajes hacia vidasalud.direct.exchange utilizando las routing keys correspondientes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publica un evento de creación de cita en el Direct Exchange.
     *
     * @param event Datos del evento agendado
     */
    public void publishAppointmentCreated(AppointmentCreatedEvent event) {
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }

        log.info("Publicando AppointmentCreatedEvent hacia [{}], routingKey [{}], eventId [{}]",
                RabbitMQConstants.EXCHANGE_DIRECT,
                RabbitMQConstants.RK_APPOINTMENT_CREATE,
                event.getEventId());

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.EXCHANGE_DIRECT,
                RabbitMQConstants.RK_APPOINTMENT_CREATE,
                event
        );
    }

    /**
     * Publica un evento de confirmación de cita en el Direct Exchange.
     *
     * @param event Datos del evento confirmado
     */
    public void publishAppointmentConfirmed(AppointmentConfirmedEvent event) {
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(Instant.now());
        }

        log.info("Publicando AppointmentConfirmedEvent hacia [{}], routingKey [{}], eventId [{}]",
                RabbitMQConstants.EXCHANGE_DIRECT,
                RabbitMQConstants.RK_APPOINTMENT_CONFIRM,
                event.getEventId());

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.EXCHANGE_DIRECT,
                RabbitMQConstants.RK_APPOINTMENT_CONFIRM,
                event
        );
    }
}
