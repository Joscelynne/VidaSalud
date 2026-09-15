import React from 'react';
import { useRoles } from '../hooks/useRoles';
import { AdminDashboard } from '../components/AdminDashboard';
import { RecepcionistaDashboard } from '../components/RecepcionistaDashboard';
import { PacienteDashboard } from '../components/PacienteDashboard';
import { AuditorDashboard } from '../components/AuditorDashboard';

export const DashboardPage: React.FC = () => {
  const { roles } = useRoles();

  // Si tiene rol Admin, mostrar AdminDashboard
  if (roles.includes('Admin')) {
    return <AdminDashboard />;
  }

  // Si tiene rol Recepcionista, mostrar RecepcionistaDashboard
  if (roles.includes('Recepcionista')) {
    return <RecepcionistaDashboard />;
  }

  // Si tiene rol Auditor, mostrar AuditorDashboard
  if (roles.includes('Auditor')) {
    return <AuditorDashboard />;
  }

  // Si tiene rol Paciente o por defecto
  return <PacienteDashboard />;
};
