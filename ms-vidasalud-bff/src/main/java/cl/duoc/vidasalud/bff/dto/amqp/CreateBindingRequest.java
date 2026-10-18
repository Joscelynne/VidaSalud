package cl.duoc.vidasalud.bff.dto.amqp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Solicitud de creación de binding entre exchange y cola o entre exchanges.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBindingRequest {

    @NotBlank(message = "El nombre del recurso destino es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El destino contiene caracteres no permitidos")
    private String destination;

    @Builder.Default
    @NotBlank(message = "destinationType es obligatorio")
    @Pattern(regexp = "(?i)^(QUEUE|EXCHANGE)$", message = "destinationType debe ser QUEUE o EXCHANGE")
    private String destinationType = "QUEUE";

    @NotBlank(message = "El nombre del exchange origen es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El exchange contiene caracteres no permitidos")
    private String exchange;

    @NotBlank(message = "La routing key es obligatoria")
    private String routingKey;

    private Map<String, Object> arguments;
}
