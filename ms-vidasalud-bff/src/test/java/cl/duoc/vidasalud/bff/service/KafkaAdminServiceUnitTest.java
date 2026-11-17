package cl.duoc.vidasalud.bff.service;

import cl.duoc.vidasalud.bff.config.KafkaProperties;
import cl.duoc.vidasalud.bff.dto.kafka.KafkaOperationResponse;
import cl.duoc.vidasalud.bff.dto.kafka.PublishTestKafkaEventRequest;
import cl.duoc.vidasalud.bff.event.AtencionMetricaEvent;
import cl.duoc.vidasalud.bff.event.TelemetriaClinicaEvent;
import cl.duoc.vidasalud.bff.producer.AtencionMetricaProducer;
import cl.duoc.vidasalud.bff.producer.TelemetriaClinicaProducer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias de KafkaAdminService (EP5)")
class KafkaAdminServiceUnitTest {

    @Mock
    private AdminClient adminClient;

    @Mock
    private AtencionMetricaProducer atencionMetricaProducer;

    @Mock
    private TelemetriaClinicaProducer telemetriaClinicaProducer;

    private KafkaProperties properties;
    private KafkaAdminService kafkaAdminService;

    @BeforeEach
    void setUp() {
        properties = new KafkaProperties();
        kafkaAdminService = new KafkaAdminService(
                adminClient,
                properties,
                atencionMetricaProducer,
                telemetriaClinicaProducer
        );
    }

    @Test
    @DisplayName("deleteTopic debe impedir la eliminación de tópicos esenciales protegidos")
    void shouldRejectDeletionOfProtectedTopics() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                kafkaAdminService.deleteTopic("vidasalud.atenciones.stream"));

        assertTrue(ex.getStatusCode().is4xxClientError());
        assertTrue(ex.getReason().contains("esencial protegido"));
        verify(adminClient, never()).deleteTopics(anyCollection());
    }

    @Test
    @DisplayName("deleteTopic debe permitir la eliminación de tópicos no protegidos")
    void shouldAllowDeletionOfNonProtectedTopics() {
        DeleteTopicsResult mockResult = mock(DeleteTopicsResult.class);
        when(mockResult.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(adminClient.deleteTopics(Collections.singletonList("topico.temporal"))).thenReturn(mockResult);

        KafkaOperationResponse response = kafkaAdminService.deleteTopic("topico.temporal");

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getMessage().contains("topico.temporal"));
        verify(adminClient).deleteTopics(Collections.singletonList("topico.temporal"));
    }

    @Test
    @DisplayName("publishTestEvent debe publicar AtencionMetricaEvent cuando eventType es ATENCIONES")
    void shouldPublishAtencionMetricaTestEvent() {
        when(atencionMetricaProducer.publishAtencionMetrica(any(AtencionMetricaEvent.class)))
                .thenReturn(new CompletableFuture<>());

        PublishTestKafkaEventRequest request = PublishTestKafkaEventRequest.builder()
                .eventType("ATENCIONES")
                .specialty("Neurología")
                .status("SCHEDULED")
                .durationMinutes(40)
                .build();

        KafkaOperationResponse response = kafkaAdminService.publishTestEvent(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getMessage().contains("AtencionMetricaEvent"));
        verify(atencionMetricaProducer).publishAtencionMetrica(any(AtencionMetricaEvent.class));
        verifyNoInteractions(telemetriaClinicaProducer);
    }

    @Test
    @DisplayName("publishTestEvent debe publicar TelemetriaClinicaEvent cuando eventType es TELEMETRIA")
    void shouldPublishTelemetriaTestEvent() {
        when(telemetriaClinicaProducer.publishTelemetria(any(TelemetriaClinicaEvent.class)))
                .thenReturn(new CompletableFuture<>());

        PublishTestKafkaEventRequest request = PublishTestKafkaEventRequest.builder()
                .eventType("TELEMETRIA")
                .sourceComponent("ms-vidasalud-bff")
                .operationType("LIST_APPOINTMENTS")
                .executionTimeMs(85L)
                .statusCode(200)
                .severity("INFO")
                .build();

        KafkaOperationResponse response = kafkaAdminService.publishTestEvent(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getMessage().contains("TelemetriaClinicaEvent"));
        verify(telemetriaClinicaProducer).publishTelemetria(any(TelemetriaClinicaEvent.class));
        verifyNoInteractions(atencionMetricaProducer);
    }

    @Test
    @DisplayName("publishTestEvent debe rechazar eventType inválido")
    void shouldRejectInvalidEventType() {
        PublishTestKafkaEventRequest request = PublishTestKafkaEventRequest.builder()
                .eventType("TIPO_INVENTADO")
                .build();

        assertThrows(ResponseStatusException.class, () ->
                kafkaAdminService.publishTestEvent(request));
    }
}
