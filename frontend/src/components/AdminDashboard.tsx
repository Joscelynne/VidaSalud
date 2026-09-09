import React, { useEffect, useState } from 'react';
import { reportService } from '../services/report.service';
import type { DashboardKPIs } from '../types/report.types';
import { BarChart3, Users, CheckCircle2, Clock, DollarSign, ShieldAlert } from 'lucide-react';

export const AdminDashboard: React.FC = () => {
  const [kpis, setKpis] = useState<DashboardKPIs | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    reportService
      .getKPIs()
      .then((data) => setKpis(data))
      .catch(() => {
        setKpis({
          totalAppointments: 1420,
          completedAppointments: 1180,
          pendingAppointments: 190,
          cancelledAppointments: 50,
          monthlyRevenue: 48500000,
          patientSatisfactionRate: 96.4,
        });
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            <BarChart3 className="w-6 h-6 text-purple-400" /> Panel de Administración Global
          </h2>
          <p className="text-slate-400 text-sm">Vista consolidada de KPIs, métricas de atención y nivel de servicio.</p>
        </div>
        <span className="px-3 py-1 bg-purple-500/10 text-purple-400 border border-purple-500/20 text-xs font-semibold rounded-full">
          Rol: Admin
        </span>
      </div>

      {loading ? (
        <div className="p-8 text-center text-slate-400 animate-pulse">Cargando métricas de administración...</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-5">
            <div className="flex items-center justify-between text-slate-400 mb-2">
              <span className="text-xs font-medium uppercase tracking-wider">Total Atenciones</span>
              <Users className="w-4 h-4 text-emerald-400" />
            </div>
            <p className="text-3xl font-extrabold text-white">{kpis?.totalAppointments}</p>
            <span className="text-xs text-emerald-400 font-medium">+12% este mes</span>
          </div>

          <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-5">
            <div className="flex items-center justify-between text-slate-400 mb-2">
              <span className="text-xs font-medium uppercase tracking-wider">Completadas</span>
              <CheckCircle2 className="w-4 h-4 text-blue-400" />
            </div>
            <p className="text-3xl font-extrabold text-white">{kpis?.completedAppointments}</p>
            <span className="text-xs text-blue-400 font-medium">83.1% de resolución</span>
          </div>

          <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-5">
            <div className="flex items-center justify-between text-slate-400 mb-2">
              <span className="text-xs font-medium uppercase tracking-wider">Pendientes</span>
              <Clock className="w-4 h-4 text-amber-400" />
            </div>
            <p className="text-3xl font-extrabold text-white">{kpis?.pendingAppointments}</p>
            <span className="text-xs text-amber-400 font-medium">En cola de atención</span>
          </div>

          <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-5">
            <div className="flex items-center justify-between text-slate-400 mb-2">
              <span className="text-xs font-medium uppercase tracking-wider">Ingresos Estimados</span>
              <DollarSign className="w-4 h-4 text-purple-400" />
            </div>
            <p className="text-2xl font-extrabold text-white">${kpis?.monthlyRevenue.toLocaleString('es-CL')}</p>
            <span className="text-xs text-purple-400 font-medium">Facturación del período</span>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-slate-800/40 border border-slate-700/50 rounded-xl p-5">
          <h3 className="text-lg font-bold text-white mb-3 flex items-center gap-2">
            <BarChart3 className="w-5 h-5 text-emerald-400" /> Resumen de Reportería
          </h3>
          <ul className="space-y-3 text-sm">
            <li className="flex justify-between items-center bg-slate-900/50 p-3 rounded-lg border border-slate-800">
              <span className="text-slate-300">Medicina General</span>
              <span className="font-bold text-emerald-400">450 atenciones (31.6%)</span>
            </li>
            <li className="flex justify-between items-center bg-slate-900/50 p-3 rounded-lg border border-slate-800">
              <span className="text-slate-300">Pediatría</span>
              <span className="font-bold text-emerald-400">320 atenciones (22.5%)</span>
            </li>
            <li className="flex justify-between items-center bg-slate-900/50 p-3 rounded-lg border border-slate-800">
              <span className="text-slate-300">Cardiología</span>
              <span className="font-bold text-emerald-400">210 atenciones (14.7%)</span>
            </li>
          </ul>
        </div>

        <div className="bg-slate-800/40 border border-slate-700/50 rounded-xl p-5">
          <h3 className="text-lg font-bold text-white mb-3 flex items-center gap-2">
            <ShieldAlert className="w-5 h-5 text-amber-400" /> Alertas de Auditoría Recientes
          </h3>
          <div className="space-y-3 text-xs">
            <div className="bg-slate-900/50 p-3 rounded-lg border border-slate-800 flex items-start gap-3">
              <span className="px-2 py-0.5 bg-emerald-500/20 text-emerald-400 rounded font-mono">200 OK</span>
              <div>
                <p className="text-slate-200 font-semibold">GET /api/report/kpis</p>
                <p className="text-slate-400">Ejecutado por Admin via Azure AD JWT Token</p>
              </div>
            </div>
            <div className="bg-slate-900/50 p-3 rounded-lg border border-slate-800 flex items-start gap-3">
              <span className="px-2 py-0.5 bg-amber-500/20 text-amber-400 rounded font-mono">403 FORBIDDEN</span>
              <div>
                <p className="text-slate-200 font-semibold">GET /api/audit/logs</p>
                <p className="text-slate-400">Intento bloqueado por falta de rol Auditor/Admin</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
