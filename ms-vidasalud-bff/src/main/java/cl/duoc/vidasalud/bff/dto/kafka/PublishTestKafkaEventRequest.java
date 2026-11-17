package cl.duoc.vidasalud.bff.dto.kafka;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishTestKafkaEventRequest {

    /**
     * Tipo de evento a simular: "ATENCIONES" o "TELEMETRIA".
     */
    @NotBlank(message = "El tipo de evento (eventType) es obligatorio: 'ATENCIONES' o 'TELEMETRIA'")
    private String eventType;

    /**
     * Clave opcional de particionado. Si es nula, se genera automáticamente.
     */
    private String partitionKey;

    /**
     * Campos opcionales para personalizar el evento de atenciones.
     */
    private String specialty;
    private String status;
    private Integer durationMinutes;

    /**
     * Campos opcionales para personalizar el evento de telemetría.
     */
    private String sourceComponent;
    private String operationType;
    private Long executionTimeMs;
    private Integer statusCode;
    private String severity;
}
