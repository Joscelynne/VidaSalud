package cl.duoc.vidasalud.bff.dto.amqp;

import jakarta.validation.constraints.NotBlank;
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
    private String destination;

    @Builder.Default
    private String destinationType = "QUEUE";

    @NotBlank(message = "El nombre del exchange origen es obligatorio")
    private String exchange;

    @NotBlank(message = "La routing key es obligatoria")
    private String routingKey;

    private Map<String, Object> arguments;
}
