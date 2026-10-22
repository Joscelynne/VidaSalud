export interface NotificationItem {
  id: string;
  eventId?: string;
  type: string;
  title: string;
  message: string;
  appointmentId?: string;
  patientName?: string;
  status: string;
  read: boolean;
  createdAt?: string;
}
