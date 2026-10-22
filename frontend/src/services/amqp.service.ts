import axios from 'axios';
import api from './api';

// ─── Request types (match Java DTOs exactly) ──────────────────────────────────

export interface CreateQueueRequest {
  name: string;
  durable?: boolean;
  exclusive?: boolean;
  autoDelete?: boolean;
  deadLetterExchange?: string;
  deadLetterRoutingKey?: string;
  messageTtl?: number;
  maxLength?: number;
}

export interface CreateExchangeRequest {
  name: string;
  type: 'direct' | 'topic' | 'fanout';
  durable?: boolean;
  autoDelete?: boolean;
}

export interface CreateBindingRequest {
  destination: string;
  destinationType?: 'QUEUE' | 'EXCHANGE';
  exchange: string;
  routingKey: string;
}

export interface TestPublishRequest {
  exchange: string;
  routingKey: string;
  payload: unknown;
  headers?: Record<string, unknown>;
}

// ─── Response types (match Java DTOs exactly) ─────────────────────────────────

export interface AmqpOperationResponse {
  timestamp: string;
  status: string;
  message: string;
  data: Record<string, unknown>;
}

export interface QueueStatsResponse {
  name: string;
  exists: boolean;
  messageCount: number;
  consumerCount: number;
  apiSource: string;
  details: string;
}

/** ErrorResponse structure returned by GlobalExceptionHandler */
export interface AmqpErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  /**
   * Known codes: AMQP_EMPTY_NAME, AMQP_RESERVED_PREFIX, AMQP_PROTECTED_RESOURCE,
   * AMQP_INVALID_CHARACTERS, AMQP_UNSUPPORTED_EXCHANGE_TYPE,
   * AMQP_INVALID_DESTINATION_TYPE, AMQP_BROKER_REJECTED
   */
  errorCode?: string;
}

export interface ParsedAmqpError {
  status: number;
  errorCode?: string;
  message: string;
  timestamp?: string;
  path?: string;
}

export function extractAmqpError(err: unknown): ParsedAmqpError {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as AmqpErrorResponse | undefined;
    return {
      status: data?.status ?? err.response?.status ?? 500,
      errorCode: data?.errorCode,
      message: data?.message || err.message || 'Error de comunicación con RabbitMQ',
      timestamp: data?.timestamp,
      path: data?.path,
    };
  }
  if (err instanceof Error) {
    return {
      status: 500,
      message: err.message,
    };
  }
  return {
    status: 500,
    message: 'Error inesperado al procesar la operación AMQP',
  };
}

// ─── Service ──────────────────────────────────────────────────────────────────

const BASE = '/api/admin/amqp';

export const amqpService = {
  // ── Queues ─────────────────────────────────────────────────────────────────

  createQueue: async (req: CreateQueueRequest): Promise<AmqpOperationResponse> => {
    const response = await api.post<AmqpOperationResponse>(`${BASE}/queues`, req);
    return response.data;
  },

  getQueueStats: async (name: string): Promise<QueueStatsResponse> => {
    const response = await api.get<QueueStatsResponse>(`${BASE}/queues/${encodeURIComponent(name)}`);
    return response.data;
  },

  deleteQueue: async (name: string): Promise<AmqpOperationResponse> => {
    const response = await api.delete<AmqpOperationResponse>(`${BASE}/queues/${encodeURIComponent(name)}`);
    return response.data;
  },

  purgeQueue: async (name: string): Promise<AmqpOperationResponse> => {
    const response = await api.post<AmqpOperationResponse>(`${BASE}/queues/${encodeURIComponent(name)}/purge`);
    return response.data;
  },

  // ── Exchanges ──────────────────────────────────────────────────────────────

  createExchange: async (req: CreateExchangeRequest): Promise<AmqpOperationResponse> => {
    const response = await api.post<AmqpOperationResponse>(`${BASE}/exchanges`, req);
    return response.data;
  },

  deleteExchange: async (name: string): Promise<AmqpOperationResponse> => {
    const response = await api.delete<AmqpOperationResponse>(`${BASE}/exchanges/${encodeURIComponent(name)}`);
    return response.data;
  },

  // ── Bindings ───────────────────────────────────────────────────────────────

  createBinding: async (req: CreateBindingRequest): Promise<AmqpOperationResponse> => {
    const response = await api.post<AmqpOperationResponse>(`${BASE}/bindings`, req);
    return response.data;
  },

  deleteBinding: async (req: CreateBindingRequest): Promise<AmqpOperationResponse> => {
    const params = new URLSearchParams({
      destination: req.destination,
      destinationType: req.destinationType ?? 'QUEUE',
      exchange: req.exchange,
      routingKey: req.routingKey,
    });
    const response = await api.delete<AmqpOperationResponse>(`${BASE}/bindings?${params.toString()}`);
    return response.data;
  },

  // ── Test Publish ───────────────────────────────────────────────────────────

  testPublish: async (req: TestPublishRequest): Promise<AmqpOperationResponse> => {
    const response = await api.post<AmqpOperationResponse>(`${BASE}/test-publish`, req);
    return response.data;
  },
};
