package cl.duoc.vidasalud.bff.consumer;

import cl.duoc.vidasalud.bff.config.RabbitMQProperties;
import cl.duoc.vidasalud.bff.event.AppointmentCreatedEvent;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidor para eventos de agendamiento de citas en la cola 'citas.agendamiento.queue'.
 * Implementa confirmación manual ACK/NACK:
 * - basicAck ante procesamiento exitoso.
 * - basicNack(requeue=false) ante errores de validación o fallos definitivos,
 *   permitiendo que el broker redirija el mensaje al exchange DLX y a la DLQ correspondiente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentCreationConsumer {

    private final RabbitMQProperties rabbitMQ;

    @RabbitListener(
            queues = "${vidasalud.rabbitmq.queues.appointment-creation}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void receiveAppointmentCreated(AppointmentCreatedEvent event,
                                           Channel channel,
                                           @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.debug("Mensaje recibido en [{}] con deliveryTag [{}]",
                rabbitMQ.getQueues().getAppointmentCreation(), deliveryTag);

        try {
            validateEvent(event);
            processEvent(event);

            // Confirmación manual exitosa
            channel.basicAck(deliveryTag, false);
            log.info("ACK manual confirmado para cita agendada [{}] con deliveryTag [{}]",
                    event.getAppointmentId(), deliveryTag);

        } catch (IllegalArgumentException ex) {
            log.error("Validación fallida en cita agendada. Ejecutando basicNack (requeue=false) hacia DLQ: {}",
                    ex.getMessage());
            channel.basicNack(deliveryTag, false, false);

        } catch (RuntimeException ex) {
            // Deja que el RetryInterceptor configurado en application.yml haga los intentos acotados.
            // Tras agotarlos, el recoverer rechaza sin requeue y RabbitMQ enruta el mensaje a la DLQ.
            log.warn("Fallo recuperable al procesar cita agendada; se aplicarán reintentos: {}",
                    ex.getMessage(), ex);
            throw ex;
        }
    }

    private void validateEvent(AppointmentCreatedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("El evento AppointmentCreatedEvent no puede ser nulo");
        }
        if (event.getAppointmentId() == null || event.getAppointmentId().isBlank()) {
            throw new IllegalArgumentException("El appointmentId no puede estar vacío");
        }

        // Mecanismo controlado para pruebas de fallo y redirección a DLQ en desarrollo/local
        if (event.getAppointmentId().startsWith("FAIL-") ||
                (event.getNotes() != null && event.getNotes().contains("SIMULATE_FAIL"))) {
            throw new IllegalArgumentException("Fallo simulado para comprobación controlada de Dead Letter Queue");
        }
    }

    private void processEvent(AppointmentCreatedEvent event) {
        log.info("Procesando agendamiento de cita ID [{}], especialidad [{}], fecha [{}]",
                event.getAppointmentId(), event.getSpecialty(), event.getDateTime());
    }
}
