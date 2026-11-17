package cl.duoc.vidasalud.bff.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaTopicSummaryDto {
    private String name;
    private int partitions;
    private int replicationFactor;
    @com.fasterxml.jackson.annotation.JsonProperty("isProtected")
    private boolean isProtected;
    private String retentionMs;
    private String cleanupPolicy;
    private Map<String, String> keyConfigs;
}
