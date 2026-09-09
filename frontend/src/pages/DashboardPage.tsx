import React from 'react';
import { useRoles } from '../hooks/useRoles';
import { AdminDashboard } from '../components/AdminDashboard';
import { OperatorDashboard } from '../components/OperatorDashboard';
import { ClientDashboard } from '../components/ClientDashboard';
import { AuditorDashboard } from '../components/AuditorDashboard';

export const DashboardPage: React.FC = () => {
  const { roles } = useRoles();

  // Si tiene rol Admin, mostrar AdminDashboard
  if (roles.includes('Admin')) {
    return <AdminDashboard />;
  }

  // Si tiene rol Operator, mostrar OperatorDashboard
  if (roles.includes('Operator')) {
    return <OperatorDashboard />;
  }

  // Si tiene rol Auditor, mostrar AuditorDashboard
  if (roles.includes('Auditor')) {
    return <AuditorDashboard />;
  }

  // Por defecto (Client)
  return <ClientDashboard />;
};
