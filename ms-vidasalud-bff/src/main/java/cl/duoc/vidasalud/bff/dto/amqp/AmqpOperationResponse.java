package cl.duoc.vidasalud.bff.dto.amqp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Respuesta genérica estándar para operaciones administrativas sobre RabbitMQ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmqpOperationResponse {

    @Builder.Default
    private String timestamp = Instant.now().toString();

    private String status;
    private String message;
    private Object data;
}
