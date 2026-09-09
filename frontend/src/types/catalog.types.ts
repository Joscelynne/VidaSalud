export interface MedicalSpecialty {
  id: string;
  name: string;
  description: string;
  availableDoctorsCount: number;
  consultationFee: number;
  active: boolean;
}
