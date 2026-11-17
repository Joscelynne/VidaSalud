package cl.duoc.vidasalud.bff.dto.kafka;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateKafkaTopicRequest {

    @NotBlank(message = "El nombre del tópico es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El nombre del tópico solo puede contener letras, números, puntos, guiones y guiones bajos")
    private String name;

    @Min(value = 1, message = "Debe tener al menos 1 partición")
    @Builder.Default
    private int partitions = 3;

    @Min(value = 1, message = "El factor de réplica debe ser al menos 1")
    @Builder.Default
    private short replicationFactor = 3;

    private Long retentionMs;

    private String cleanupPolicy;

    private Map<String, String> additionalConfigs;
}
