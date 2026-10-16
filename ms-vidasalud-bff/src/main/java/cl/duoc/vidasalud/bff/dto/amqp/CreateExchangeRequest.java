package cl.duoc.vidasalud.bff.dto.amqp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Solicitud de creación de exchange AMQP (direct, topic o fanout).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExchangeRequest {

    @NotBlank(message = "El nombre del exchange es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre del exchange contiene caracteres no permitidos")
    private String name;

    @NotBlank(message = "El tipo de exchange es obligatorio (direct, topic, fanout)")
    @Pattern(regexp = "(?i)^(direct|topic|fanout)$", message = "Tipo no soportado. Debe ser 'direct', 'topic' o 'fanout'")
    private String type;

    @Builder.Default
    private boolean durable = true;

    @Builder.Default
    private boolean autoDelete = false;
}
