export interface Appointment {
  id: string;
  patientName: string;
  doctorName: string;
  specialty: string;
  dateTime: string;
  status: 'PENDING' | 'CONFIRMED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  roomNumber?: string;
  notes?: string;
}
