package cl.duoc.vidasalud.bff.config;

import cl.duoc.vidasalud.bff.event.AtencionMetricaEvent;
import cl.duoc.vidasalud.bff.event.TelemetriaClinicaEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias de Serialización de Eventos Kafka (EP5)")
class KafkaSerializationUnitTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Debe serializar y deserializar AtencionMetricaEvent sin pérdida de datos")
    void shouldSerializeAndDeserializeAtencionMetricaEvent() throws Exception {
        Instant now = Instant.parse("2026-11-17T18:00:00Z");
        AtencionMetricaEvent original = AtencionMetricaEvent.builder()
                .eventId("evt-" + UUID.randomUUID())
                .specialty("Cardiología")
                .status("SCHEDULED")
                .durationMinutes(45)
                .timestamp(now)
                .traceId("trc-abc-123")
                .build();

        String json = objectMapper.writeValueAsString(original);
        assertNotNull(json);
        assertTrue(json.contains("Cardiología"));
        assertFalse(json.contains("appointmentId"));
        assertFalse(json.contains("patientPseudonym"));
        assertFalse(json.contains("doctorCode"));
        assertFalse(json.contains("roomNumber"));

        AtencionMetricaEvent deserialized = objectMapper.readValue(json, AtencionMetricaEvent.class);
        assertEquals(original.getEventId(), deserialized.getEventId());
        assertEquals(original.getSpecialty(), deserialized.getSpecialty());
        assertEquals(original.getDurationMinutes(), deserialized.getDurationMinutes());
        assertEquals(original.getTimestamp(), deserialized.getTimestamp());
    }

    @Test
    @DisplayName("Debe serializar y deserializar TelemetriaClinicaEvent con métricas operacionales")
    void shouldSerializeAndDeserializeTelemetriaClinicaEvent() throws Exception {
        Instant now = Instant.parse("2026-11-17T18:15:00Z");
        TelemetriaClinicaEvent original = TelemetriaClinicaEvent.builder()
                .telemetryId("tel-" + UUID.randomUUID())
                .sourceComponent("ms-vidasalud-bff")
                .operationType("CREATE_APPOINTMENT")
                .executionTimeMs(120L)
                .statusCode(201)
                .serverNode("node-bff-1")
                .severity("INFO")
                .systemLoadPct(35.2)
                .timestamp(now)
                .traceId("trc-xyz-987")
                .details("Transacción exitosa")
                .build();

        String json = objectMapper.writeValueAsString(original);
        assertNotNull(json);
        assertTrue(json.contains("CREATE_APPOINTMENT"));

        TelemetriaClinicaEvent deserialized = objectMapper.readValue(json, TelemetriaClinicaEvent.class);
        assertEquals(original.getTelemetryId(), deserialized.getTelemetryId());
        assertEquals(original.getSourceComponent(), deserialized.getSourceComponent());
        assertEquals(original.getExecutionTimeMs(), deserialized.getExecutionTimeMs());
        assertEquals(original.getStatusCode(), deserialized.getStatusCode());
        assertEquals(original.getSeverity(), deserialized.getSeverity());
        assertEquals(original.getTimestamp(), deserialized.getTimestamp());
    }
}
