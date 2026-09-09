import api from './api';
import type { DashboardKPIs, SpecialtyReport } from '../types/report.types';

export const reportService = {
  getKPIs: async (): Promise<DashboardKPIs> => {
    const response = await api.get<DashboardKPIs>('/api/report/kpis');
    return response.data;
  },

  getSpecialtyReport: async (): Promise<SpecialtyReport[]> => {
    const response = await api.get<SpecialtyReport[]>('/api/report/specialties');
    return response.data;
  },
};
