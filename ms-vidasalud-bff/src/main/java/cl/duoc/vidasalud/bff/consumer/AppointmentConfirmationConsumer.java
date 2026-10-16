package cl.duoc.vidasalud.bff.consumer;

import cl.duoc.vidasalud.bff.config.RabbitMQConstants;
import cl.duoc.vidasalud.bff.event.AppointmentConfirmedEvent;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor para eventos de confirmación de citas en la cola 'citas.confirmacion.queue'.
 * Implementa confirmación manual ACK/NACK:
 * - basicAck ante procesamiento exitoso.
 * - basicNack(requeue=false) ante errores para derivar a citas.confirmacion.dlq.
 */
@Slf4j
@Component
public class AppointmentConfirmationConsumer {

    @RabbitListener(
            queues = RabbitMQConstants.QUEUE_APPOINTMENT_CONFIRMATION,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void receiveAppointmentConfirmed(AppointmentConfirmedEvent event,
                                             Channel channel,
                                             @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.debug("Mensaje recibido en [{}] con deliveryTag [{}]",
                RabbitMQConstants.QUEUE_APPOINTMENT_CONFIRMATION, deliveryTag);

        try {
            validateEvent(event);
            processEvent(event);

            // Confirmación manual exitosa
            channel.basicAck(deliveryTag, false);
            log.info("ACK manual confirmado para cita confirmada [{}] con deliveryTag [{}]",
                    event.getAppointmentId(), deliveryTag);

        } catch (IllegalArgumentException ex) {
            log.error("Validación fallida en cita confirmada. Ejecutando basicNack (requeue=false) hacia DLQ: {}",
                    ex.getMessage());
            channel.basicNack(deliveryTag, false, false);

        } catch (Exception ex) {
            log.error("Fallo inesperado al procesar confirmación de cita. Ejecutando basicNack hacia DLQ: {}",
                    ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void validateEvent(AppointmentConfirmedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("El evento AppointmentConfirmedEvent no puede ser nulo");
        }
        if (event.getAppointmentId() == null || event.getAppointmentId().isBlank()) {
            throw new IllegalArgumentException("El appointmentId no puede estar vacío");
        }

        // Simulación controlada para pruebas DLQ
        if (event.getAppointmentId().startsWith("FAIL-") ||
                (event.getNotes() != null && event.getNotes().contains("SIMULATE_FAIL"))) {
            throw new IllegalArgumentException("Fallo simulado para comprobación controlada de Dead Letter Queue");
        }
    }

    private void processEvent(AppointmentConfirmedEvent event) {
        log.info("Procesando confirmación de cita ID [{}], confirmado por [{}], estado [{}]",
                event.getAppointmentId(), event.getConfirmedBy(), event.getStatus());
    }
}
