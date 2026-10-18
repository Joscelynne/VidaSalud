package cl.duoc.vidasalud.bff.service;

import cl.duoc.vidasalud.bff.config.RabbitMQConstants;
import cl.duoc.vidasalud.bff.config.VidaSaludTopologyConfig;
import cl.duoc.vidasalud.bff.dto.amqp.*;
import cl.duoc.vidasalud.bff.exception.QueueNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias de RabbitAdminService")
class RabbitAdminServiceUnitTest {

    @Mock
    private AmqpAdmin amqpAdmin;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private VidaSaludTopologyConfig topologyConfig;
    private RabbitAdminService service;

    @BeforeEach
    void setUp() {
        // Configuración manual de la topología protegida (sin Spring Context)
        topologyConfig = new VidaSaludTopologyConfig();
        
        VidaSaludTopologyConfig.Exchanges exchanges = new VidaSaludTopologyConfig.Exchanges();
        exchanges.setDirect("vidasalud.direct.exchange");
        exchanges.setTopic("vidasalud.topic.exchange");
        exchanges.setDlx("vidasalud.dlx.exchange");
        topologyConfig.setExchanges(exchanges);
        
        VidaSaludTopologyConfig.Queues queues = new VidaSaludTopologyConfig.Queues();
        queues.setAppointmentCreation("citas.agendamiento.queue");
        queues.setAppointmentConfirmation("citas.confirmacion.queue");
        queues.setNotificationAudit("notificaciones.auditoria.queue");
        queues.setAppointmentCreationDlq("citas.agendamiento.dlq");
        queues.setAppointmentConfirmationDlq("citas.confirmacion.dlq");
        queues.setNotificationAuditDlq("notificaciones.auditoria.dlq");
        topologyConfig.setQueues(queues);
        
        service = new RabbitAdminService(amqpAdmin, rabbitTemplate, topologyConfig);
    }

    @Test
    @DisplayName("Verifica creación de cola con argumentos DLX mediante declareQueue")
    void shouldCreateQueueWithDlxArguments() {
        CreateQueueRequest request = CreateQueueRequest.builder()
                .name("test.custom.queue")
                .durable(true)
                .deadLetterExchange("test.dlx")
                .deadLetterRoutingKey("test.dlq.rk")
                .messageTtl(60000)
                .maxLength(500)
                .build();

        // Mock: declareQueue retorna el nombre y getQueueInfo confirma existencia
        when(amqpAdmin.declareQueue(any())).thenReturn("test.custom.queue");
        when(amqpAdmin.getQueueInfo("test.custom.queue")).thenReturn(
                new QueueInformation("test.custom.queue", 0, 0)
        );

        String createdName = service.createQueue(request);

        assertThat(createdName).isEqualTo("test.custom.queue");

        ArgumentCaptor<Queue> queueCaptor = ArgumentCaptor.forClass(Queue.class);
        verify(amqpAdmin).declareQueue(queueCaptor.capture());

        Queue declared = queueCaptor.getValue();
        assertThat(declared.getName()).isEqualTo("test.custom.queue");
        assertThat(declared.isDurable()).isTrue();
        assertThat(declared.getArguments())
                .containsEntry(RabbitMQConstants.X_DEAD_LETTER_EXCHANGE, "test.dlx")
                .containsEntry(RabbitMQConstants.X_DEAD_LETTER_ROUTING_KEY, "test.dlq.rk")
                .containsEntry("x-message-ttl", 60000)
                .containsEntry("x-max-length", 500);
    }

    @Test
    @DisplayName("Verifica consulta de estadísticas de una cola existente")
    void shouldReturnQueueStatsWhenQueueExists() {
        QueueInformation info = new QueueInformation("citas.agendamiento.queue", 42, 3);
        when(amqpAdmin.getQueueInfo("citas.agendamiento.queue")).thenReturn(info);

        QueueStatsResponse response = service.getQueueStats("citas.agendamiento.queue");

        assertThat(response.getName()).isEqualTo("citas.agendamiento.queue");
        assertThat(response.isExists()).isTrue();
        assertThat(response.getMessageCount()).isEqualTo(42);
        assertThat(response.getConsumerCount()).isEqualTo(3);
        assertThat(response.getApiSource()).isNotBlank();
    }

    @Test
    @DisplayName("Verifica excepción QueueNotFoundException al consultar estadísticas de cola inexistente")
    void shouldThrowWhenQueueDoesNotExist() {
        when(amqpAdmin.getQueueInfo("cola.inexistente")).thenReturn(null);

        assertThatThrownBy(() -> service.getQueueStats("cola.inexistente"))
                .isInstanceOf(QueueNotFoundException.class)
                .hasMessageContaining("cola.inexistente");
    }

    @Test
    @DisplayName("Verifica eliminación de cola existente")
    void shouldDeleteExistingQueue() {
        QueueInformation info = new QueueInformation("test.delete.queue", 0, 0);
        when(amqpAdmin.getQueueInfo("test.delete.queue")).thenReturn(info);
        when(amqpAdmin.deleteQueue("test.delete.queue")).thenReturn(true);

        boolean result = service.deleteQueue("test.delete.queue");

        assertThat(result).isTrue();
        verify(amqpAdmin).deleteQueue("test.delete.queue");
    }

    @Test
    @DisplayName("Verifica purga de cola retornando la cantidad de mensajes purgados")
    void shouldPurgeQueueAndReturnCount() {
        QueueInformation info = new QueueInformation("test.purge.queue", 15, 0);
        when(amqpAdmin.getQueueInfo("test.purge.queue")).thenReturn(info);

        int purged = service.purgeQueue("test.purge.queue");

        assertThat(purged).isEqualTo(15);
        verify(amqpAdmin).purgeQueue("test.purge.queue", false);
    }

    @Test
    @DisplayName("Verifica creación de exchange direct, topic y fanout")
    void shouldCreateVariousExchangeTypes() {
        // Direct
        service.createExchange(CreateExchangeRequest.builder()
                .name("test.direct")
                .type("direct")
                .durable(true)
                .build());

        // Topic
        service.createExchange(CreateExchangeRequest.builder()
                .name("test.topic")
                .type("topic")
                .durable(true)
                .build());

        // Fanout
        service.createExchange(CreateExchangeRequest.builder()
                .name("test.fanout")
                .type("fanout")
                .durable(true)
                .build());

        verify(amqpAdmin, times(3)).declareExchange(any(Exchange.class));
    }

    @Test
    @DisplayName("Verifica fallo ante tipo de exchange no soportado")
    void shouldThrowOnUnsupportedExchangeType() {
        CreateExchangeRequest invalid = CreateExchangeRequest.builder()
                .name("test.invalid")
                .type("unsupported_type")
                .build();

        assertThatThrownBy(() -> service.createExchange(invalid))
                .isInstanceOf(cl.duoc.vidasalud.bff.exception.AmqpValidationException.class)
                .hasMessageContaining("no está soportado");
    }

    @Test
    @DisplayName("Verifica creación de binding entre exchange y cola")
    void shouldCreateBinding() {
        CreateBindingRequest request = CreateBindingRequest.builder()
                .destination("mi.cola")
                .destinationType("QUEUE")
                .exchange("mi.exchange")
                .routingKey("mi.clave")
                .build();

        service.createBinding(request);

        ArgumentCaptor<Binding> captor = ArgumentCaptor.forClass(Binding.class);
        verify(amqpAdmin).declareBinding(captor.capture());

        Binding declared = captor.getValue();
        assertThat(declared.getDestination()).isEqualTo("mi.cola");
        assertThat(declared.getExchange()).isEqualTo("mi.exchange");
        assertThat(declared.getRoutingKey()).isEqualTo("mi.clave");
    }

    @Test
    @DisplayName("Verifica test-publish hacia exchange y routing key con headers")
    void shouldTestPublishMessage() {
        TestPublishRequest request = TestPublishRequest.builder()
                .exchange("vidasalud.direct.exchange")
                .routingKey("citas.crear")
                .payload(Map.of("testKey", "testVal"))
                .headers(Map.of("X-Test", "123"))
                .build();

        service.testPublish(request);

        verify(rabbitTemplate).convertAndSend(
                eq("vidasalud.direct.exchange"),
                eq("citas.crear"),
                eq(request.getPayload()),
                any(MessagePostProcessor.class)
        );
    }
}
