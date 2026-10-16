package cl.duoc.vidasalud.bff.dto.amqp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Solicitud de publicación de mensaje de prueba en un exchange con routing key específica.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestPublishRequest {

    @NotBlank(message = "El exchange es obligatorio")
    private String exchange;

    @NotBlank(message = "La routing key es obligatoria")
    private String routingKey;

    @NotNull(message = "El payload del mensaje no puede ser nulo")
    private Object payload;

    private Map<String, Object> headers;
}
