package cl.duoc.vidasalud.bff.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pruebas Unitarias de Configuración y Topología RabbitMQ")
class RabbitMQConfigUnitTest {

    private RabbitMQConfig config;

    @BeforeEach
    void setUp() {
        config = new RabbitMQConfig();
    }

    @Test
    @DisplayName("1. Verifica que los 3 exchanges posean los nombres y tipos exactos")
    void shouldDeclareCorrectExchanges() {
        DirectExchange direct = config.directExchange();
        TopicExchange topic = config.topicExchange();
        DirectExchange dlx = config.dlxExchange();

        assertThat(direct.getName()).isEqualTo(RabbitMQConstants.EXCHANGE_DIRECT);
        assertThat(direct.isDurable()).isTrue();

        assertThat(topic.getName()).isEqualTo(RabbitMQConstants.EXCHANGE_TOPIC);
        assertThat(topic.isDurable()).isTrue();

        assertThat(dlx.getName()).isEqualTo(RabbitMQConstants.EXCHANGE_DLX);
        assertThat(dlx.isDurable()).isTrue();
    }

    @Test
    @DisplayName("2. Verifica nombres exactos y durabilidad de las 6 colas")
    void shouldDeclareSixDurableQueues() {
        Queue creationQueue = config.appointmentCreationQueue();
        Queue confirmationQueue = config.appointmentConfirmationQueue();
        Queue auditQueue = config.notificationAuditQueue();

        Queue creationDlq = config.appointmentCreationDlq();
        Queue confirmationDlq = config.appointmentConfirmationDlq();
        Queue auditDlq = config.notificationAuditDlq();

        // 3 Colas principales
        assertThat(creationQueue.getName()).isEqualTo(RabbitMQConstants.QUEUE_APPOINTMENT_CREATION);
        assertThat(creationQueue.isDurable()).isTrue();

        assertThat(confirmationQueue.getName()).isEqualTo(RabbitMQConstants.QUEUE_APPOINTMENT_CONFIRMATION);
        assertThat(confirmationQueue.isDurable()).isTrue();

        assertThat(auditQueue.getName()).isEqualTo(RabbitMQConstants.QUEUE_NOTIFICATION_AUDIT);
        assertThat(auditQueue.isDurable()).isTrue();

        // 3 Colas DLQ
        assertThat(creationDlq.getName()).isEqualTo(RabbitMQConstants.QUEUE_APPOINTMENT_CREATION_DLQ);
        assertThat(creationDlq.isDurable()).isTrue();

        assertThat(confirmationDlq.getName()).isEqualTo(RabbitMQConstants.QUEUE_APPOINTMENT_CONFIRMATION_DLQ);
        assertThat(confirmationDlq.isDurable()).isTrue();

        assertThat(auditDlq.getName()).isEqualTo(RabbitMQConstants.QUEUE_NOTIFICATION_AUDIT_DLQ);
        assertThat(auditDlq.isDurable()).isTrue();
    }

    @Test
    @DisplayName("3. Verifica argumentos DLX (x-dead-letter-exchange y routing key) en colas principales")
    void shouldConfigureDlxArgumentsOnMainQueues() {
        Queue creationQueue = config.appointmentCreationQueue();
        Queue confirmationQueue = config.appointmentConfirmationQueue();
        Queue auditQueue = config.notificationAuditQueue();

        assertThat(creationQueue.getArguments())
                .containsEntry(RabbitMQConstants.X_DEAD_LETTER_EXCHANGE, RabbitMQConstants.EXCHANGE_DLX)
                .containsEntry(RabbitMQConstants.X_DEAD_LETTER_ROUTING_KEY, RabbitMQConstants.RK_APPOINTMENT_CREATION_DLQ);

        assertThat(confirmationQueue.getArguments())
                .containsEntry(RabbitMQConstants.X_DEAD_LETTER_EXCHANGE, RabbitMQConstants.EXCHANGE_DLX)
                .containsEntry(RabbitMQConstants.X_DEAD_LETTER_ROUTING_KEY, RabbitMQConstants.RK_APPOINTMENT_CONFIRMATION_DLQ);

        assertThat(auditQueue.getArguments())
                .containsEntry(RabbitMQConstants.X_DEAD_LETTER_EXCHANGE, RabbitMQConstants.EXCHANGE_DLX)
                .containsEntry(RabbitMQConstants.X_DEAD_LETTER_ROUTING_KEY, RabbitMQConstants.RK_NOTIFICATION_AUDIT_DLQ);
    }

    @Test
    @DisplayName("4. Verifica bindings y routing keys principales y de Dead Letter")
    void shouldConfigureAllBindingsCorrectly() {
        DirectExchange direct = config.directExchange();
        TopicExchange topic = config.topicExchange();
        DirectExchange dlx = config.dlxExchange();

        Queue creationQueue = config.appointmentCreationQueue();
        Queue confirmationQueue = config.appointmentConfirmationQueue();
        Queue auditQueue = config.notificationAuditQueue();

        Queue creationDlq = config.appointmentCreationDlq();
        Queue confirmationDlq = config.appointmentConfirmationDlq();
        Queue auditDlq = config.notificationAuditDlq();

        Binding b1 = config.appointmentCreationBinding(creationQueue, direct);
        Binding b2 = config.appointmentConfirmationBinding(confirmationQueue, direct);
        Binding b3 = config.notificationAuditBinding(auditQueue, topic);

        Binding b4 = config.appointmentCreationDlqBinding(creationDlq, dlx);
        Binding b5 = config.appointmentConfirmationDlqBinding(confirmationDlq, dlx);
        Binding b6 = config.notificationAuditDlqBinding(auditDlq, dlx);

        assertThat(b1.getRoutingKey()).isEqualTo(RabbitMQConstants.RK_APPOINTMENT_CREATE);
        assertThat(b1.getExchange()).isEqualTo(RabbitMQConstants.EXCHANGE_DIRECT);

        assertThat(b2.getRoutingKey()).isEqualTo(RabbitMQConstants.RK_APPOINTMENT_CONFIRM);
        assertThat(b2.getExchange()).isEqualTo(RabbitMQConstants.EXCHANGE_DIRECT);

        assertThat(b3.getRoutingKey()).isEqualTo(RabbitMQConstants.RK_NOTIFICATION_TOPIC_PATTERN);
        assertThat(b3.getExchange()).isEqualTo(RabbitMQConstants.EXCHANGE_TOPIC);

        assertThat(b4.getRoutingKey()).isEqualTo(RabbitMQConstants.RK_APPOINTMENT_CREATION_DLQ);
        assertThat(b4.getExchange()).isEqualTo(RabbitMQConstants.EXCHANGE_DLX);

        assertThat(b5.getRoutingKey()).isEqualTo(RabbitMQConstants.RK_APPOINTMENT_CONFIRMATION_DLQ);
        assertThat(b5.getExchange()).isEqualTo(RabbitMQConstants.EXCHANGE_DLX);

        assertThat(b6.getRoutingKey()).isEqualTo(RabbitMQConstants.RK_NOTIFICATION_AUDIT_DLQ);
        assertThat(b6.getExchange()).isEqualTo(RabbitMQConstants.EXCHANGE_DLX);
    }
}
