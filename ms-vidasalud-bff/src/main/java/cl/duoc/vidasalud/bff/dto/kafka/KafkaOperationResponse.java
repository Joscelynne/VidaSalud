package cl.duoc.vidasalud.bff.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaOperationResponse {
    private String status;
    private String message;
    private Instant timestamp;
    private Map<String, Object> details;
}
