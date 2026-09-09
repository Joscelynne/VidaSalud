export interface AuditLogEntry {
  id: string;
  timestamp: string;
  user: string;
  action: string;
  resource: string;
  status: 'SUCCESS' | 'FAILURE' | 'UNAUTHORIZED';
  ipAddress: string;
  traceId: string;
  details?: string;
}
