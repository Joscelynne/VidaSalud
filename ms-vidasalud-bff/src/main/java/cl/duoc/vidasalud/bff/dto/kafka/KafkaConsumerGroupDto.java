package cl.duoc.vidasalud.bff.dto.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaConsumerGroupDto {
    private String groupId;
    private String state;
    private String protocolType;
    private int membersCount;
    private long totalLag;
    private List<GroupPartitionAssignment> partitions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupPartitionAssignment {
        private String topic;
        private int partition;
        private Long currentOffset;
        private Long logEndOffset;
        private Long lag;
        private String memberId;
        private String clientId;
        private String host;
    }
}
