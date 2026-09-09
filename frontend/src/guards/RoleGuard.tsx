import React from 'react';
import { Navigate } from 'react-router-dom';
import { useRoles } from '../hooks/useRoles';
import type { UserRole } from '../types/auth.types';

interface RoleGuardProps {
  allowedRoles: UserRole[];
  children: React.ReactNode;
}

export const RoleGuard: React.FC<RoleGuardProps> = ({ allowedRoles, children }) => {
  const { hasRole } = useRoles();

  if (!hasRole(allowedRoles)) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <>{children}</>;
};
