import api from './api';
import type { MedicalSpecialty } from '../types/catalog.types';

export const catalogService = {
  getSpecialties: async (): Promise<MedicalSpecialty[]> => {
    const response = await api.get<MedicalSpecialty[]>('/api/catalog/specialties');
    return response.data;
  },

  createSpecialty: async (specialty: Partial<MedicalSpecialty>): Promise<MedicalSpecialty> => {
    const response = await api.post<MedicalSpecialty>('/api/catalog/specialties', specialty);
    return response.data;
  },
};
