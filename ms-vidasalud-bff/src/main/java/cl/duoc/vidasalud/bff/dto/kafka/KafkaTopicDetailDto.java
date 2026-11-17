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
public class KafkaTopicDetailDto {
    private String name;
    private int partitionsCount;
    private int replicationFactor;
    @com.fasterxml.jackson.annotation.JsonProperty("isProtected")
    private boolean isProtected;
    private List<PartitionDetail> partitions;
    private Map<String, String> configuration;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartitionDetail {
        private int partitionId;
        private Integer leaderNodeId;
        private List<Integer> replicaNodeIds;
        private List<Integer> inSyncReplicaNodeIds;
    }
}
