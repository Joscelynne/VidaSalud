package cl.duoc.vidasalud.bff.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pruebas Unitarias de Configuración y Topología RabbitMQ")
class RabbitMQConfigUnitTest {

    private RabbitMQConfig config;
    private RabbitMQProperties properties;

    @BeforeEach
    void setUp() {
        config = new RabbitMQConfig();
        properties = new RabbitMQProperties();
        RabbitMQProperties.Exchanges exchanges = new RabbitMQProperties.Exchanges();
        exchanges.setDirect("vidasalud.direct.exchange");
        exchanges.setTopic("vidasalud.topic.exchange");
        exchanges.setDlx("vidasalud.dlx.exchange");
        properties.setExchanges(exchanges);
        RabbitMQProperties.Queues queues = new RabbitMQProperties.Queues();
        queues.setAppointmentCreation("citas.agendamiento.queue");
        queues.setAppointmentConfirmation("citas.confirmacion.queue");
        queues.setNotificationAudit("notificaciones.auditoria.queue");
        queues.setAppointmentCreationDlq("citas.agendamiento.dlq");
        queues.setAppointmentConfirmationDlq("citas.confirmacion.dlq");
        queues.setNotificationAuditDlq("notificaciones.auditoria.dlq");
        properties.setQueues(queues);
        RabbitMQProperties.RoutingKeys keys = new RabbitMQProperties.RoutingKeys();
        keys.setAppointmentCreate("citas.crear");
        keys.setAppointmentConfirm("citas.confirmar");
        keys.setNotificationAudit("vidasalud.notificacion.*");
        keys.setAppointmentCreationDlq("citas.agendamiento.dlq.rk");
        keys.setAppointmentConfirmationDlq("citas.confirmacion.dlq.rk");
        keys.setNotificationAuditDlq("notificaciones.auditoria.dlq.rk");
        properties.setRoutingKeys(keys);
    }

    @Test
    @DisplayName("1. Verifica que los 3 exchanges posean los nombres y tipos exactos")
    void shouldDeclareCorrectExchanges() {
        DirectExchange direct = config.directExchange(properties);
        TopicExchange topic = config.topicExchange(properties);
        DirectExchange dlx = config.dlxExchange(properties);

        assertThat(direct.getName()).isEqualTo("vidasalud.direct.exchange");
        assertThat(direct.isDurable()).isTrue();

        assertThat(topic.getName()).isEqualTo("vidasalud.topic.exchange");
        assertThat(topic.isDurable()).isTrue();

        assertThat(dlx.getName()).isEqualTo("vidasalud.dlx.exchange");
        assertThat(dlx.isDurable()).isTrue();
    }

    @Test
    @DisplayName("2. Verifica nombres exactos y durabilidad de las 6 colas")
    void shouldDeclareSixDurableQueues() {
        Queue creationQueue = config.appointmentCreationQueue(properties);
        Queue confirmationQueue = config.appointmentConfirmationQueue(properties);
        Queue auditQueue = config.notificationAuditQueue(properties);

        Queue creationDlq = config.appointmentCreationDlq(properties);
        Queue confirmationDlq = config.appointmentConfirmationDlq(properties);
        Queue auditDlq = config.notificationAuditDlq(properties);

        // 3 Colas principales
        assertThat(creationQueue.getName()).isEqualTo("citas.agendamiento.queue");
        assertThat(creationQueue.isDurable()).isTrue();

        assertThat(confirmationQueue.getName()).isEqualTo("citas.confirmacion.queue");
        assertThat(confirmationQueue.isDurable()).isTrue();

        assertThat(auditQueue.getName()).isEqualTo("notificaciones.auditoria.queue");
        assertThat(auditQueue.isDurable()).isTrue();

        // 3 Colas DLQ
        assertThat(creationDlq.getName()).isEqualTo("citas.agendamiento.dlq");
        assertThat(creationDlq.isDurable()).isTrue();

        assertThat(confirmationDlq.getName()).isEqualTo("citas.confirmacion.dlq");
        assertThat(confirmationDlq.isDurable()).isTrue();

        assertThat(auditDlq.getName()).isEqualTo("notificaciones.auditoria.dlq");
        assertThat(auditDlq.isDurable()).isTrue();
    }

    @Test
    @DisplayName("3. Verifica argumentos DLX (x-dead-letter-exchange y routing key) en colas principales")
    void shouldConfigureDlxArgumentsOnMainQueues() {
        Queue creationQueue = config.appointmentCreationQueue(properties);
        Queue confirmationQueue = config.appointmentConfirmationQueue(properties);
        Queue auditQueue = config.notificationAuditQueue(properties);

        assertThat(creationQueue.getArguments())
                .containsEntry(RabbitMQConstants.X_DEAD_LETTER_EXCHANGE, "vidasalud.dlx.exchange")
                .containsEntry(RabbitMQConstants.X_DEAD_LETTER_ROUTING_KEY, "citas.agendamiento.dlq.rk");

        assertThat(confirmationQueue.getArguments())
                .containsEntry(RabbitMQConstants.X_DEAD_LETTER_EXCHANGE, "vidasalud.dlx.exchange")
                .containsEntry(RabbitMQConstants.X_DEAD_LETTER_ROUTING_KEY, "citas.confirmacion.dlq.rk");

        assertThat(auditQueue.getArguments())
                .containsEntry(RabbitMQConstants.X_DEAD_LETTER_EXCHANGE, "vidasalud.dlx.exchange")
                .containsEntry(RabbitMQConstants.X_DEAD_LETTER_ROUTING_KEY, "notificaciones.auditoria.dlq.rk");
    }

    @Test
    @DisplayName("4. Verifica bindings y routing keys principales y de Dead Letter")
    void shouldConfigureAllBindingsCorrectly() {
        DirectExchange direct = config.directExchange(properties);
        TopicExchange topic = config.topicExchange(properties);
        DirectExchange dlx = config.dlxExchange(properties);

        Queue creationQueue = config.appointmentCreationQueue(properties);
        Queue confirmationQueue = config.appointmentConfirmationQueue(properties);
        Queue auditQueue = config.notificationAuditQueue(properties);

        Queue creationDlq = config.appointmentCreationDlq(properties);
        Queue confirmationDlq = config.appointmentConfirmationDlq(properties);
        Queue auditDlq = config.notificationAuditDlq(properties);

        Binding b1 = config.appointmentCreationBinding(creationQueue, direct, properties);
        Binding b2 = config.appointmentConfirmationBinding(confirmationQueue, direct, properties);
        Binding b3 = config.notificationAuditBinding(auditQueue, topic, properties);

        Binding b4 = config.appointmentCreationDlqBinding(creationDlq, dlx, properties);
        Binding b5 = config.appointmentConfirmationDlqBinding(confirmationDlq, dlx, properties);
        Binding b6 = config.notificationAuditDlqBinding(auditDlq, dlx, properties);

        assertThat(b1.getRoutingKey()).isEqualTo("citas.crear");
        assertThat(b1.getExchange()).isEqualTo("vidasalud.direct.exchange");

        assertThat(b2.getRoutingKey()).isEqualTo("citas.confirmar");
        assertThat(b2.getExchange()).isEqualTo("vidasalud.direct.exchange");

        assertThat(b3.getRoutingKey()).isEqualTo("vidasalud.notificacion.*");
        assertThat(b3.getExchange()).isEqualTo("vidasalud.topic.exchange");

        assertThat(b4.getRoutingKey()).isEqualTo("citas.agendamiento.dlq.rk");
        assertThat(b4.getExchange()).isEqualTo("vidasalud.dlx.exchange");

        assertThat(b5.getRoutingKey()).isEqualTo("citas.confirmacion.dlq.rk");
        assertThat(b5.getExchange()).isEqualTo("vidasalud.dlx.exchange");

        assertThat(b6.getRoutingKey()).isEqualTo("notificaciones.auditoria.dlq.rk");
        assertThat(b6.getExchange()).isEqualTo("vidasalud.dlx.exchange");
    }
}
