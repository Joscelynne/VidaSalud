import api from './api';
import type {
  KafkaTopicSummary,
  KafkaTopicDetail,
  KafkaConsumerGroup,
  CreateKafkaTopicRequest,
  AlterKafkaTopicConfigRequest,
  PublishTestKafkaEventRequest,
  KafkaOperationResponse,
  ResetConsumerGroupOffsetsRequest,
} from '../types/kafka.types';

export const kafkaService = {
  /**
   * Obtiene la lista de todos los tópicos del clúster con sus particiones y réplicas.
   */
  async listTopics(): Promise<KafkaTopicSummary[]> {
    const response = await api.get<KafkaTopicSummary[]>('/api/admin/kafka/topics');
    return response.data;
  },

  /**
   * Obtiene el detalle técnico de un tópico (particiones, réplicas, ISRs y configuraciones).
   */
  async getTopicDetail(topicName: string): Promise<KafkaTopicDetail> {
    const response = await api.get<KafkaTopicDetail>(`/api/admin/kafka/topics/${encodeURIComponent(topicName)}`);
    return response.data;
  },

  /**
   * Crea un nuevo tópico en el clúster Kafka.
   */
  async createTopic(request: CreateKafkaTopicRequest): Promise<KafkaOperationResponse> {
    const response = await api.post<KafkaOperationResponse>('/api/admin/kafka/topics', request);
    return response.data;
  },

  /**
   * Modifica parámetros de configuración de un tópico (ej. retention.ms).
   */
  async alterTopicConfig(
    topicName: string,
    request: AlterKafkaTopicConfigRequest
  ): Promise<KafkaOperationResponse> {
    const response = await api.patch<KafkaOperationResponse>(
      `/api/admin/kafka/topics/${encodeURIComponent(topicName)}/config`,
      request
    );
    return response.data;
  },

  /**
   * Elimina un tópico del clúster (rechaza tópicos protegidos).
   */
  async deleteTopic(topicName: string): Promise<KafkaOperationResponse> {
    const response = await api.delete<KafkaOperationResponse>(
      `/api/admin/kafka/topics/${encodeURIComponent(topicName)}`
    );
    return response.data;
  },

  /**
   * Lista los Consumer Groups con sus particiones asignadas, offsets y lag.
   */
  async listConsumerGroups(): Promise<KafkaConsumerGroup[]> {
    const response = await api.get<KafkaConsumerGroup[]>('/api/admin/kafka/consumer-groups');
    return response.data;
  },

  /**
   * Reinicia offsets de un consumer group detenido hacia el inicio o final del log.
   */
  async resetConsumerGroupOffsets(
    groupId: string,
    request: ResetConsumerGroupOffsetsRequest
  ): Promise<KafkaOperationResponse> {
    const response = await api.post<KafkaOperationResponse>(
      `/api/admin/kafka/consumer-groups/${encodeURIComponent(groupId)}/offsets/reset`,
      request
    );
    return response.data;
  },

  /**
   * Publica un evento simulado de prueba para comprobar el streaming y avance de offsets.
   */
  async publishTestEvent(request: PublishTestKafkaEventRequest): Promise<KafkaOperationResponse> {
    const response = await api.post<KafkaOperationResponse>('/api/admin/kafka/publish/test', request);
    return response.data;
  },
};
