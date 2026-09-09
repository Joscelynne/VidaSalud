import api from './api';
import type { AuditLogEntry } from '../types/audit.types';

export const auditService = {
  getAuditLogs: async (): Promise<AuditLogEntry[]> => {
    const response = await api.get<AuditLogEntry[]>('/api/audit/logs');
    return response.data;
  },
};
