import React from 'react';
import type { UserRole } from '../types/auth.types';

interface RoleBadgeProps {
  role: UserRole;
}

export const RoleBadge: React.FC<RoleBadgeProps> = ({ role }) => {
  const getBadgeStyle = (r: UserRole) => {
    switch (r) {
      case 'Admin':
        return 'bg-purple-500/10 text-purple-400 border-purple-500/30';
      case 'Operator':
        return 'bg-blue-500/10 text-blue-400 border-blue-500/30';
      case 'Client':
        return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30';
      case 'Auditor':
        return 'bg-amber-500/10 text-amber-400 border-amber-500/30';
      default:
        return 'bg-slate-500/10 text-slate-400 border-slate-500/30';
    }
  };

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${getBadgeStyle(
        role
      )}`}
    >
      {role}
    </span>
  );
};
