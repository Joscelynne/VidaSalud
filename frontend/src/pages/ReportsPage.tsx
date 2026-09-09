import React from 'react';
import { BarChart3 } from 'lucide-react';
import { AdminDashboard } from '../components/AdminDashboard';

export const ReportsPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            <BarChart3 className="w-6 h-6 text-purple-400" /> Reportería Gerencial y Analítica
          </h2>
          <p className="text-slate-400 text-sm">Informes ejecutivos accesibles exclusivamente por administradores del sistema.</p>
        </div>
        <span className="px-3 py-1 bg-purple-500/10 text-purple-400 border border-purple-500/20 text-xs font-semibold rounded-full">
          Ruta Exclusiva: Admin
        </span>
      </div>

      <AdminDashboard />
    </div>
  );
};
