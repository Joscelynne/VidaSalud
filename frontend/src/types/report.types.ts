export interface DashboardKPIs {
  totalAppointments: number;
  completedAppointments: number;
  pendingAppointments: number;
  cancelledAppointments: number;
  monthlyRevenue: number;
  patientSatisfactionRate: number;
}

export interface SpecialtyReport {
  specialtyName: string;
  count: number;
  percentage: number;
}
