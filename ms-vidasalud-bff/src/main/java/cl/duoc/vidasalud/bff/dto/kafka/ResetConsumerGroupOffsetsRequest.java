package cl.duoc.vidasalud.bff.dto.kafka;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetConsumerGroupOffsetsRequest {

    @NotBlank(message = "La estrategia es requerida: EARLIEST o LATEST")
    private String strategy;

    @AssertTrue(message = "Debe confirmar explícitamente el reinicio de offsets")
    private boolean confirmed;
}
