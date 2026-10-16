package cl.duoc.vidasalud.bff.dto.amqp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Solicitud de creación de cola AMQP con soporte para Dead Letter Exchange y argumentos avanzados.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQueueRequest {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre de la cola contiene caracteres no permitidos")
    private String name;

    @Builder.Default
    private boolean durable = true;

    @Builder.Default
    private boolean exclusive = false;

    @Builder.Default
    private boolean autoDelete = false;

    // Configuración Dead Letter Queue (DLQ) opcional
    private String deadLetterExchange;
    private String deadLetterRoutingKey;
    private Integer messageTtl;
    private Integer maxLength;
}
