import api from './api';
import type { Appointment } from '../types/appointment.types';

export const appointmentService = {
  getAppointments: async (): Promise<Appointment[]> => {
    const response = await api.get<Appointment[]>('/api/appointments');
    return response.data;
  },

  getAppointmentById: async (id: string): Promise<Appointment> => {
    const response = await api.get<Appointment>(`/api/appointments/${id}`);
    return response.data;
  },

  confirmAppointment: async (id: string): Promise<Appointment> => {
    const response = await api.post<Appointment>(`/api/appointments/${id}/confirm`);
    return response.data;
  },

  createAppointment: async (appointment: Partial<Appointment>): Promise<Appointment> => {
    const response = await api.post<Appointment>('/api/appointments', appointment);
    return response.data;
  },
};
