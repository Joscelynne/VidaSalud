import React from 'react';
import { NavLink } from 'react-router-dom';
import { useRoles } from '../hooks/useRoles';
import {
  LayoutDashboard,
  CalendarCheck,
  BookOpen,
  BarChart3,
  History,
  Activity,
} from 'lucide-react';

export const Sidebar: React.FC = () => {
  const { hasRole, isAdmin } = useRoles();

  const navItems = [
    {
      to: '/dashboard',
      label: 'Dashboard',
      icon: LayoutDashboard,
      show: true,
    },
    {
      to: '/appointments',
      label: 'Atenciones Médicas',
      icon: CalendarCheck,
      show: hasRole(['Admin', 'Operator', 'Client']),
    },
    {
      to: '/catalog',
      label: 'Catálogo de Especialidades',
      icon: BookOpen,
      show: hasRole(['Admin', 'Operator']),
    },
    {
      to: '/reports',
      label: 'Reportería Gerencial',
      icon: BarChart3,
      show: isAdmin,
    },
    {
      to: '/audit',
      label: 'Auditoría de Sistema',
      icon: History,
      show: hasRole(['Admin', 'Auditor']),
    },
  ];

  return (
    <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col justify-between p-4 min-h-screen">
      <div>
        <div className="flex items-center gap-3 px-3 py-4 mb-6 border-b border-slate-800">
          <div className="w-9 h-9 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center">
            <Activity className="w-5 h-5" />
          </div>
          <div>
            <h1 className="font-extrabold text-lg text-white tracking-wide">
              Vida<span className="text-emerald-400">Salud</span>
            </h1>
            <p className="text-[10px] text-slate-400 uppercase tracking-widest font-mono">BFF Cloud Native</p>
          </div>
        </div>

        <nav className="space-y-1">
          {navItems
            .filter((item) => item.show)
            .map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                      isActive
                        ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                        : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                    }`
                  }
                >
                  <Icon className="w-4 h-4" />
                  <span>{item.label}</span>
                </NavLink>
              );
            })}
        </nav>
      </div>

      <div className="p-3 bg-slate-850 rounded-xl border border-slate-800 text-xs text-slate-400 space-y-1">
        <p className="font-semibold text-slate-300">Duoc UC - DSY1107</p>
        <p>Evaluación Parcial N°1</p>
        <p className="text-[10px] text-slate-500">MSAL + Spring Boot BFF</p>
      </div>
    </aside>
  );
};
