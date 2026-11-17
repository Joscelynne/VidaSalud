export interface KafkaTopicSummary {
  name: string;
  partitions: number;
  replicationFactor: number;
  isProtected: boolean;
  retentionMs?: string;
  cleanupPolicy?: string;
  keyConfigs?: Record<string, string>;
}

export interface PartitionDetail {
  partitionId: number;
  leaderNodeId: number | null;
  replicaNodeIds: number[];
  inSyncReplicaNodeIds: number[];
}

export interface KafkaTopicDetail {
  name: string;
  partitionsCount: number;
  replicationFactor: number;
  isProtected: boolean;
  partitions: PartitionDetail[];
  configuration: Record<string, string>;
}

export interface GroupPartitionAssignment {
  topic: string;
  partition: number;
  currentOffset: number;
  logEndOffset: number;
  lag: number;
  memberId: string;
  clientId: string;
  host: string;
}

export interface KafkaConsumerGroup {
  groupId: string;
  state: string;
  protocolType: string;
  membersCount: number;
  totalLag: number;
  partitions: GroupPartitionAssignment[];
}

export interface CreateKafkaTopicRequest {
  name: string;
  partitions: number;
  replicationFactor: number;
  retentionMs?: number;
  cleanupPolicy?: string;
}

export interface AlterKafkaTopicConfigRequest {
  configs: Record<string, string>;
}

export interface PublishTestKafkaEventRequest {
  eventType: 'ATENCIONES' | 'TELEMETRIA';
  partitionKey?: string;
  specialty?: string;
  status?: string;
  durationMinutes?: number;
  sourceComponent?: string;
  operationType?: string;
  executionTimeMs?: number;
  statusCode?: number;
  severity?: string;
}

export interface KafkaOperationResponse {
  status: string;
  message: string;
  timestamp: string;
  details?: Record<string, unknown>;
}
