package cl.duoc.vidasalud.bff.consumer;

import cl.duoc.vidasalud.bff.event.AtencionMetricaEvent;
import cl.duoc.vidasalud.bff.event.TelemetriaClinicaEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias de Validación en Consumidores Kafka (EP5)")
class KafkaConsumerValidationTest {

    private AtencionMetricaConsumer atencionConsumer;
    private TelemetriaClinicaConsumer telemetriaConsumer;

    @BeforeEach
    void setUp() {
        atencionConsumer = new AtencionMetricaConsumer();
        telemetriaConsumer = new TelemetriaClinicaConsumer();
    }

    @Test
    @DisplayName("AtencionMetricaConsumer debe rechazar evento nulo con IllegalArgumentException")
    void shouldRejectNullAtencionMetricaEvent() {
        assertThrows(IllegalArgumentException.class, () ->
                atencionConsumer.consumeAtencionMetrica(null, "vidasalud.atenciones.stream", 0, 10L, "key"));
    }

    @Test
    @DisplayName("AtencionMetricaConsumer debe rechazar evento sin especialidad")
    void shouldRejectAtencionMetricaWithoutSpecialty() {
        AtencionMetricaEvent event = AtencionMetricaEvent.builder()
                .eventId("evt-1")
                .specialty("")
                .status("SCHEDULED")
                .timestamp(Instant.now())
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                atencionConsumer.consumeAtencionMetrica(event, "vidasalud.atenciones.stream", 1, 20L, "key"));
    }

    @Test
    @DisplayName("AtencionMetricaConsumer debe simular fallo y lanzar excepción ante prefijo FAIL-KAFKA")
    void shouldSimulateFailureOnSpecialPrefix() {
        AtencionMetricaEvent event = AtencionMetricaEvent.builder()
                .eventId("FAIL-KAFKA-999")
                .specialty("Traumatología")
                .status("SCHEDULED")
                .timestamp(Instant.now())
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                atencionConsumer.consumeAtencionMetrica(event, "vidasalud.atenciones.stream", 2, 30L, "key"));
    }

    @Test
    @DisplayName("AtencionMetricaConsumer debe procesar evento válido e incrementar métricas")
    void shouldProcessValidAtencionMetricaEvent() {
        AtencionMetricaEvent event = AtencionMetricaEvent.builder()
                .eventId("evt-ok")
                .specialty("Pediatría")
                .status("SCHEDULED")
                .durationMinutes(20)
                .timestamp(Instant.now())
                .build();

        atencionConsumer.consumeAtencionMetrica(event, "vidasalud.atenciones.stream", 0, 40L, "Pediatría");

        assertEquals(1, atencionConsumer.getTotalEventsProcessed().get());
        assertNotNull(atencionConsumer.getSpecialtyCounts().get("Pediatría"));
        assertEquals(1, atencionConsumer.getSpecialtyCounts().get("Pediatría").get());
    }

    @Test
    @DisplayName("TelemetriaClinicaConsumer debe rechazar evento con executionTimeMs negativo")
    void shouldRejectNegativeExecutionTime() {
        TelemetriaClinicaEvent event = TelemetriaClinicaEvent.builder()
                .telemetryId("tel-invalid")
                .sourceComponent("bff")
                .operationType("QUERY")
                .executionTimeMs(-50L)
                .timestamp(Instant.now())
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                telemetriaConsumer.consumeTelemetria(event, "vidasalud.telemetria.stream", 0, 5L, "bff"));
    }

    @Test
    @DisplayName("TelemetriaClinicaConsumer debe detectar anomalía y contar alertas ante latencia superior a 2000ms")
    void shouldTriggerAlertOnHighLatency() {
        TelemetriaClinicaEvent event = TelemetriaClinicaEvent.builder()
                .telemetryId("tel-slow")
                .sourceComponent("database")
                .operationType("FETCH_ALL")
                .executionTimeMs(3500L)
                .statusCode(200)
                .timestamp(Instant.now())
                .build();

        telemetriaConsumer.consumeTelemetria(event, "vidasalud.telemetria.stream", 1, 10L, "database");

        assertEquals(1, telemetriaConsumer.getTotalTelemetryProcessed().get());
        assertEquals(1, telemetriaConsumer.getAlertsTriggered().get());
    }
}
