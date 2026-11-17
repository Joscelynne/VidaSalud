package cl.duoc.vidasalud.bff.dto.kafka;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlterKafkaTopicConfigRequest {

    @NotEmpty(message = "Debe proporcionar al menos una configuración a modificar")
    private Map<String, String> configs;
}
