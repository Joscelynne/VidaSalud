package cl.duoc.vidasalud.bff.integration;

import cl.duoc.vidasalud.bff.event.AppointmentCreatedEvent;
import cl.duoc.vidasalud.bff.producer.AppointmentEventProducer;
import com.rabbitmq.client.LongString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Verifica el flujo completo contra un broker descartable. Nunca se conecta al RabbitMQ
 * configurado para desarrollo o producción y no purga colas compartidas.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Flujo aislado de rechazo y enrutamiento RabbitMQ a DLQ")
class RabbitMQDeadLetterIntegrationTest {

    @Container
    static final RabbitMQContainer rabbit = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.13-management-alpine"));

    @DynamicPropertySource
    static void configureRabbitMq(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        registry.add("spring.rabbitmq.virtual-host", () -> "/");
        registry.add("spring.rabbitmq.addresses", () -> "");
    }

    @Autowired
    private AppointmentEventProducer appointmentProducer;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("Rechaza un evento identificable y conserva su x-death en la DLQ aislada")
    void shouldRouteFailedMessageToDeadLetterQueue() {
        String testId = UUID.randomUUID().toString();
        String failureAppointmentId = "FAIL-" + testId;
        AppointmentCreatedEvent failedEvent = AppointmentCreatedEvent.builder()
                .eventId("dlq-test-" + testId)
                .appointmentId(failureAppointmentId)
                .notes("SIMULATE_FAIL - integración aislada")
                .specialty("Urgencias")
                .doctorName("Dr. Control DLQ")
                .build();

        appointmentProducer.publishAppointmentCreated(failedEvent);

        AtomicReference<Message> receivedMessage = new AtomicReference<>();
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Message message = rabbitTemplate.receive(
                    "citas.agendamiento.dlq", 250);
            assertThat(message).isNotNull();
            receivedMessage.set(message);
        });

        // La cola pertenece al broker desechable del test; leer el mensaje aquí no toca
        // mensajes de ningún entorno compartido.
        Message message = receivedMessage.get();
        assertThat(message).isNotNull();

        AppointmentCreatedEvent receivedEvent = (AppointmentCreatedEvent)
                rabbitTemplate.getMessageConverter().fromMessage(message);
        assertThat(receivedEvent.getEventId()).isEqualTo(failedEvent.getEventId());
        assertThat(receivedEvent.getAppointmentId()).isEqualTo(failureAppointmentId);

        Object deathHeader = message.getMessageProperties().getHeaders().get("x-death");
        assertThat(deathHeader).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<Map<String, ?>> deaths = (List<Map<String, ?>>) deathHeader;
        assertThat(deaths).isNotEmpty();
        Map<String, ?> death = deaths.get(0);
        assertThat(headerText(death.get("reason"))).isEqualTo("rejected");
        assertThat(headerText(death.get("queue")))
                .isEqualTo("citas.agendamiento.queue");
        assertThat(death.get("count")).isEqualTo(1L);
    }

    private static String headerText(Object value) {
        return value instanceof LongString longString ? longString.toString() : String.valueOf(value);
    }
}
