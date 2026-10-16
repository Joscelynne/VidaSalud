package cl.duoc.vidasalud.bff.integration;

import cl.duoc.vidasalud.bff.config.RabbitMQConstants;
import cl.duoc.vidasalud.bff.event.AppointmentCreatedEvent;
import cl.duoc.vidasalud.bff.producer.AppointmentEventProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("dev")
@DisplayName("Prueba de Integración Real: Flujo de Mensaje Fallido y Enrutamiento a DLQ")
class RabbitMQDeadLetterIntegrationTest {

    @Autowired
    private AppointmentEventProducer appointmentProducer;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Test
    @DisplayName("Verifica que un mensaje fallido (FAIL-) sea rechazado con NACK (requeue=false) y enrutado a la DLQ correspondiente")
    void shouldRouteFailedMessageToDeadLetterQueue() {
        // Purgar la DLQ antes de la prueba para aislar el conteo
        amqpAdmin.purgeQueue(RabbitMQConstants.QUEUE_APPOINTMENT_CREATION_DLQ, false);

        String failureAppointmentId = "FAIL-INTEGRATION-001";
        AppointmentCreatedEvent failedEvent = AppointmentCreatedEvent.builder()
                .appointmentId(failureAppointmentId)
                .notes("SIMULATE_FAIL - Prueba de integración DLQ")
                .specialty("Urgencias")
                .doctorName("Dr. Control DLQ")
                .build();

        // 1. Publicar evento con prefijo FAIL- hacia citas.crear
        appointmentProducer.publishAppointmentCreated(failedEvent);

        // 2. Esperar a que el consumidor procese, ejecute NACK sin requeue y el broker redirija a DLQ
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            QueueInformation dlqInfo = amqpAdmin.getQueueInfo(RabbitMQConstants.QUEUE_APPOINTMENT_CREATION_DLQ);
            assertThat(dlqInfo).isNotNull();
            assertThat(dlqInfo.getMessageCount()).isGreaterThanOrEqualTo(1);
        });

        // 3. Consumir el mensaje desde la DLQ para verificar su contenido
        Object received = rabbitTemplate.receiveAndConvert(RabbitMQConstants.QUEUE_APPOINTMENT_CREATION_DLQ, 3000);
        assertThat(received).isNotNull();
        assertThat(received).isInstanceOf(AppointmentCreatedEvent.class);

        AppointmentCreatedEvent receivedEvent = (AppointmentCreatedEvent) received;
        assertThat(receivedEvent.getAppointmentId()).isEqualTo(failureAppointmentId);

        // Limpiar la DLQ
        amqpAdmin.purgeQueue(RabbitMQConstants.QUEUE_APPOINTMENT_CREATION_DLQ, false);
    }
}
