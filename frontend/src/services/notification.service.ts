import api from './api';
import type { NotificationItem } from '../types/notification.types';

/**
 * Servicio de notificaciones VidaSalud.
 *
 * IMPORTANTE: Este servicio realiza polling periódico contra el BFF
 * (GET /api/notifications). No utiliza WebSockets ni push en tiempo real.
 * El intervalo de refresco por defecto es de 30 segundos y es configurable
 * por el componente que consuma este servicio.
 */
export const notificationService = {
  /**
   * Obtiene todas las notificaciones del usuario autenticado.
   * El BFF filtra automáticamente según el rol:
   *   - Admin / Recepcionista / Auditor → todas las notificaciones.
   *   - Paciente → solo las propias.
   */
  getNotifications: async (): Promise<NotificationItem[]> => {
    const response = await api.get<NotificationItem[]>('/api/notifications');
    return response.data;
  },

  /** Marca una notificación concreta como leída. */
  markAsRead: async (id: string): Promise<void> => {
    await api.post(`/api/notifications/${id}/read`);
  },

  /** Marca todas las notificaciones del usuario como leídas. */
  markAllAsRead: async (): Promise<void> => {
    await api.post('/api/notifications/mark-all-read');
  },
};
