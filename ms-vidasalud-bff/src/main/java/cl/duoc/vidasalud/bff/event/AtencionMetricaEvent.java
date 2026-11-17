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
 * Evento analítico para streaming en Apache Kafka (Tópico: 'vidasalud.atenciones.stream').
 * Diseñado conforme a la pauta EP5/EP6:
 * - Contiene datos operativos del ciclo de vida de la atención médica para cálculo de KPIs (tiempos de espera, ocupación).
 * - Minimiza los datos: no incluye identificadores de paciente, cita o profesional ni datos de contacto.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtencionMetricaEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "El eventId es obligatorio")
    @JsonProperty("eventId")
    private String eventId;

    @NotBlank(message = "La especialidad médica es obligatoria")
    @JsonProperty("specialty")
    private String specialty;

    /**
     * Estado operativo de la atención: SCHEDULED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW.
     */
    @NotBlank(message = "El estado de atención es obligatorio")
    @JsonProperty("status")
    private String status;

    @JsonProperty("durationMinutes")
    private Integer durationMinutes;

    @NotNull(message = "El timestamp es obligatorio")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("traceId")
    private String traceId;
}
