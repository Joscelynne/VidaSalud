import { useAuth } from './useAuth';
import type { UserRole } from '../types/auth.types';

export const useRoles = () => {
  const { roles, user } = useAuth();

  const hasRole = (allowedRoles: UserRole[]): boolean => {
    if (!roles || roles.length === 0) return false;
    return allowedRoles.some((role) => roles.includes(role));
  };

  const hasAnyRole = (...allowedRoles: UserRole[]): boolean => {
    return hasRole(allowedRoles);
  };

  const isAdmin = roles.includes('Admin');
  const isOperator = roles.includes('Operator');
  const isClient = roles.includes('Client');
  const isAuditor = roles.includes('Auditor');

  return {
    roles,
    hasRole,
    hasAnyRole,
    isAdmin,
    isOperator,
    isClient,
    isAuditor,
    user,
  };
};
