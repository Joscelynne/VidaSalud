import React from 'react';
import { History } from 'lucide-react';
import { AuditorDashboard } from '../components/AuditorDashboard';

export const AuditPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            <History className="w-6 h-6 text-amber-400" /> Registro Completo de Auditoría
          </h2>
          <p className="text-slate-400 text-sm">Monitoreo de seguridad, validaciones JWT y accesos al BFF.</p>
        </div>
        <span className="px-3 py-1 bg-amber-500/10 text-amber-400 border border-amber-500/20 text-xs font-semibold rounded-full">
          Ruta Protegida: Admin & Auditor
        </span>
      </div>

      <AuditorDashboard />
    </div>
  );
};
