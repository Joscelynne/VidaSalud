package cl.duoc.vidasalud.bff.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Evento de telemetría y monitoreo operacional para streaming en Apache Kafka
 * (Tópico: 'vidasalud.telemetria.stream').
 * Permite supervisar rendimiento, latencia de llamadas, estados HTTP, alertas y salud de la plataforma
 * en tiempo real para analítica y observabilidad.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TelemetriaClinicaEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "El telemetryId es obligatorio")
    @JsonProperty("telemetryId")
    private String telemetryId;

    @NotBlank(message = "El sourceComponent es obligatorio")
    @JsonProperty("sourceComponent")
    private String sourceComponent;

    @NotBlank(message = "El operationType es obligatorio")
    @JsonProperty("operationType")
    private String operationType;

    @NotNull(message = "El tiempo de ejecución en milisegundos es obligatorio")
    @JsonProperty("executionTimeMs")
    private Long executionTimeMs;

    @JsonProperty("statusCode")
    private Integer statusCode;

    @JsonProperty("serverNode")
    private String serverNode;

    /**
     * Severidad operativa: INFO, WARN, CRITICAL.
     */
    @JsonProperty("severity")
    private String severity;

    @JsonProperty("systemLoadPct")
    private Double systemLoadPct;

    @NotNull(message = "El timestamp es obligatorio")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("traceId")
    private String traceId;

    @JsonProperty("details")
    private String details;
}
