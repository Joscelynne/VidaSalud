package cl.duoc.vidasalud.bff.producer;

import cl.duoc.vidasalud.bff.config.RabbitMQConstants;
import cl.duoc.vidasalud.bff.event.AppointmentConfirmedEvent;
import cl.duoc.vidasalud.bff.event.AppointmentCreatedEvent;
import cl.duoc.vidasalud.bff.event.NotificationAuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias de Eventos y Productores RabbitMQ")
class EventAndProducerUnitTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private AppointmentEventProducer appointmentProducer;
    private AuditEventProducer auditProducer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        appointmentProducer = new AppointmentEventProducer(rabbitTemplate);
        auditProducer = new AuditEventProducer(rabbitTemplate);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Verifica publicación de AppointmentCreatedEvent en exchange direct con routing key citas.crear")
    void shouldPublishAppointmentCreatedEvent() {
        AppointmentCreatedEvent event = AppointmentCreatedEvent.builder()
                .appointmentId("APT-200")
                .doctorName("Dr. Carlos Valle")
                .specialty("Dermatología")
                .dateTime("2026-10-20T10:00:00")
                .roomNumber("Box 101")
                .status("PENDING")
                .build();

        appointmentProducer.publishAppointmentCreated(event);

        ArgumentCaptor<AppointmentCreatedEvent> captor = ArgumentCaptor.forClass(AppointmentCreatedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConstants.EXCHANGE_DIRECT),
                eq(RabbitMQConstants.RK_APPOINTMENT_CREATE),
                captor.capture()
        );

        AppointmentCreatedEvent sent = captor.getValue();
        assertThat(sent.getAppointmentId()).isEqualTo("APT-200");
        assertThat(sent.getEventId()).isNotBlank();
        assertThat(sent.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Verifica publicación de AppointmentConfirmedEvent en exchange direct con routing key citas.confirmar")
    void shouldPublishAppointmentConfirmedEvent() {
        AppointmentConfirmedEvent event = AppointmentConfirmedEvent.builder()
                .appointmentId("APT-200")
                .confirmedBy("recepcionista@vidasalud.cl")
                .dateTime("2026-10-20T10:00:00")
                .status("CONFIRMED")
                .build();

        appointmentProducer.publishAppointmentConfirmed(event);

        ArgumentCaptor<AppointmentConfirmedEvent> captor = ArgumentCaptor.forClass(AppointmentConfirmedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConstants.EXCHANGE_DIRECT),
                eq(RabbitMQConstants.RK_APPOINTMENT_CONFIRM),
                captor.capture()
        );

        AppointmentConfirmedEvent sent = captor.getValue();
        assertThat(sent.getAppointmentId()).isEqualTo("APT-200");
        assertThat(sent.getConfirmedBy()).isEqualTo("recepcionista@vidasalud.cl");
        assertThat(sent.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("Verifica publicación de NotificationAuditEvent en exchange topic con routing key vidasalud.notificacion.*")
    void shouldPublishNotificationAuditEvent() {
        NotificationAuditEvent event = NotificationAuditEvent.builder()
                .action("APPOINTMENT_CONFIRM")
                .resource("AppointmentController")
                .user("admin@vidasalud.cl")
                .status("SUCCESS")
                .traceId("tr-abc-123")
                .details("Confirmación procesada exitosamente")
                .build();

        auditProducer.publishNotificationAudit("vidasalud.notificacion.appointment", event);

        ArgumentCaptor<NotificationAuditEvent> captor = ArgumentCaptor.forClass(NotificationAuditEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConstants.EXCHANGE_TOPIC),
                eq("vidasalud.notificacion.appointment"),
                captor.capture()
        );

        NotificationAuditEvent sent = captor.getValue();
        assertThat(sent.getAction()).isEqualTo("APPOINTMENT_CONFIRM");
        assertThat(sent.getUser()).isEqualTo("admin@vidasalud.cl");
    }

    @Test
    @DisplayName("Verifica serialización y deserialización JSON bidireccional de los eventos")
    void shouldSerializeAndDeserializeEventsWithoutDataLoss() throws Exception {
        AppointmentCreatedEvent original = AppointmentCreatedEvent.builder()
                .eventId("evt-001")
                .eventType("APPOINTMENT_CREATED")
                .timestamp(Instant.parse("2026-10-15T15:00:00Z"))
                .appointmentId("APT-555")
                .patientName("Juan Pérez")
                .specialty("Traumatología")
                .status("PENDING")
                .build();

        String json = objectMapper.writeValueAsString(original);
        AppointmentCreatedEvent deserialized = objectMapper.readValue(json, AppointmentCreatedEvent.class);

        assertThat(deserialized.getEventId()).isEqualTo(original.getEventId());
        assertThat(deserialized.getAppointmentId()).isEqualTo(original.getAppointmentId());
        assertThat(deserialized.getTimestamp()).isEqualTo(original.getTimestamp());
    }
}
