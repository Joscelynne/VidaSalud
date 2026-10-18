package cl.duoc.vidasalud.bff.consumer;

import cl.duoc.vidasalud.bff.event.AppointmentConfirmedEvent;
import cl.duoc.vidasalud.bff.event.AppointmentCreatedEvent;
import cl.duoc.vidasalud.bff.event.NotificationAuditEvent;
import cl.duoc.vidasalud.bff.config.RabbitMQProperties;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias de Consumidores RabbitMQ (ACK / NACK a DLQ)")
class ConsumerAckNackUnitTest {

    @Mock
    private Channel channel;

    private AppointmentCreationConsumer creationConsumer;
    private AppointmentConfirmationConsumer confirmationConsumer;
    private NotificationAuditConsumer auditConsumer;

    @BeforeEach
    void setUp() {
        RabbitMQProperties properties = new RabbitMQProperties();
        RabbitMQProperties.Queues queues = new RabbitMQProperties.Queues();
        queues.setAppointmentCreation("citas.agendamiento.queue");
        queues.setAppointmentConfirmation("citas.confirmacion.queue");
        queues.setNotificationAudit("notificaciones.auditoria.queue");
        properties.setQueues(queues);
        creationConsumer = new AppointmentCreationConsumer(properties);
        confirmationConsumer = new AppointmentConfirmationConsumer(properties);
        auditConsumer = new NotificationAuditConsumer(properties);
    }

    // =========================================================================
    // CREATION CONSUMER
    // =========================================================================

    @Test
    @DisplayName("Verifica que un evento de creación válido ejecute basicAck(deliveryTag, false)")
    void shouldAckValidAppointmentCreatedEvent() throws IOException {
        long deliveryTag = 101L;
        AppointmentCreatedEvent event = AppointmentCreatedEvent.builder()
                .eventId("evt-1")
                .appointmentId("APT-001")
                .specialty("Medicina General")
                .dateTime("2026-10-20T10:00:00")
                .build();

        creationConsumer.receiveAppointmentCreated(event, channel, deliveryTag);

        verify(channel, times(1)).basicAck(deliveryTag, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("Verifica que un evento de creación inválido ejecute basicNack(deliveryTag, false, false) para DLQ")
    void shouldNackInvalidAppointmentCreatedEventWithoutRequeue() throws IOException {
        long deliveryTag = 102L;
        AppointmentCreatedEvent invalidEvent = AppointmentCreatedEvent.builder()
                .eventId("evt-2")
                .appointmentId("") // Inválido: ID vacío
                .build();

        creationConsumer.receiveAppointmentCreated(invalidEvent, channel, deliveryTag);

        verify(channel, times(1)).basicNack(deliveryTag, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("Verifica que fallo simulado (FAIL-) ejecute basicNack(deliveryTag, false, false) derivando a DLQ")
    void shouldNackSimulatedFailureAppointmentCreatedEvent() throws IOException {
        long deliveryTag = 103L;
        AppointmentCreatedEvent simulatedFailEvent = AppointmentCreatedEvent.builder()
                .eventId("evt-3")
                .appointmentId("FAIL-APT-999")
                .notes("SIMULATE_FAIL")
                .build();

        creationConsumer.receiveAppointmentCreated(simulatedFailEvent, channel, deliveryTag);

        verify(channel, times(1)).basicNack(deliveryTag, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    // =========================================================================
    // CONFIRMATION CONSUMER
    // =========================================================================

    @Test
    @DisplayName("Verifica que un evento de confirmación válido ejecute basicAck(deliveryTag, false)")
    void shouldAckValidAppointmentConfirmedEvent() throws IOException {
        long deliveryTag = 201L;
        AppointmentConfirmedEvent event = AppointmentConfirmedEvent.builder()
                .eventId("evt-4")
                .appointmentId("APT-002")
                .confirmedBy("recepcionista@vidasalud.cl")
                .status("CONFIRMED")
                .build();

        confirmationConsumer.receiveAppointmentConfirmed(event, channel, deliveryTag);

        verify(channel, times(1)).basicAck(deliveryTag, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("Verifica que un evento de confirmación nulo ejecute basicNack sin requeue")
    void shouldNackNullAppointmentConfirmedEvent() throws IOException {
        long deliveryTag = 202L;

        confirmationConsumer.receiveAppointmentConfirmed(null, channel, deliveryTag);

        verify(channel, times(1)).basicNack(deliveryTag, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    // =========================================================================
    // NOTIFICATION AUDIT CONSUMER
    // =========================================================================

    @Test
    @DisplayName("Verifica que un evento de auditoría válido ejecute basicAck(deliveryTag, false)")
    void shouldAckValidNotificationAuditEvent() throws IOException {
        long deliveryTag = 301L;
        NotificationAuditEvent event = NotificationAuditEvent.builder()
                .eventId("evt-5")
                .action("SECURITY_AUDIT")
                .resource("RabbitAdminController")
                .user("admin@vidasalud.cl")
                .status("SUCCESS")
                .build();

        auditConsumer.receiveNotificationAudit(event, channel, deliveryTag);

        verify(channel, times(1)).basicAck(deliveryTag, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("Verifica que un evento de auditoría con acción vacía ejecute basicNack sin requeue")
    void shouldNackInvalidNotificationAuditEvent() throws IOException {
        long deliveryTag = 302L;
        NotificationAuditEvent event = NotificationAuditEvent.builder()
                .eventId("evt-6")
                .action("") // Inválido: acción vacía
                .build();

        auditConsumer.receiveNotificationAudit(event, channel, deliveryTag);

        verify(channel, times(1)).basicNack(deliveryTag, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
