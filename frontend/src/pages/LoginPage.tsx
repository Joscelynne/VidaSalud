import React from 'react';
import { useAuth } from '../hooks/useAuth';
import { Navigate } from 'react-router-dom';
import { ShieldCheck, Activity } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const { login, isAuthenticated, isLoading } = useAuth();

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col justify-center items-center p-4 relative overflow-hidden">
      {/* Dynamic background glow */}
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-emerald-500/10 rounded-full blur-3xl" />
      <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-blue-500/10 rounded-full blur-3xl" />

      <div className="w-full max-w-md bg-slate-900/90 border border-slate-800 rounded-2xl shadow-2xl p-8 backdrop-blur-xl relative z-10 space-y-8 text-center">
        {/* Logo VidaSalud */}
        <div className="flex flex-col items-center gap-3">
          <div className="w-16 h-16 rounded-2xl bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center justify-center shadow-inner">
            <Activity className="w-10 h-10" />
          </div>
          <h1 className="text-3xl font-extrabold text-white tracking-wide">
            Vida<span className="text-emerald-400">Salud</span>
          </h1>
          <p className="text-xs text-slate-400 font-medium">
            Plataforma de Gestión de Atenciones Médicas
          </p>
        </div>

        <div className="space-y-4 pt-2">
          <div className="p-4 bg-slate-800/50 rounded-xl border border-slate-700/50 text-left text-xs text-slate-300 space-y-1">
            <p className="font-semibold text-emerald-400 flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4" /> Autenticación Única Enterprise
            </p>
            <p className="text-slate-400">
              Inicia sesión mediante tu cuenta institucional de Microsoft Entra ID (Azure AD).
            </p>
          </div>

          <button
            onClick={login}
            disabled={isLoading}
            className="w-full py-3.5 px-4 bg-slate-800 hover:bg-slate-750 text-white font-semibold rounded-xl border border-slate-700 hover:border-emerald-500/50 shadow-lg hover:shadow-emerald-500/10 transition-all flex items-center justify-center gap-3 cursor-pointer group"
          >
            {/* Microsoft Logo SVG */}
            <svg className="w-5 h-5" viewBox="0 0 23 23">
              <path fill="#f35325" d="M1 1h10v10H1z" />
              <path fill="#81bc06" d="M12 1h10v10H12z" />
              <path fill="#05a6f0" d="M1 12h10v10H1z" />
              <path fill="#ffba08" d="M12 12h10v10H12z" />
            </svg>
            <span className="group-hover:text-emerald-400 transition-colors">
              {isLoading ? 'Redireccionando a Microsoft...' : 'Iniciar sesión con Microsoft'}
            </span>
          </button>
        </div>

        <div className="text-[11px] text-slate-500 border-t border-slate-800/80 pt-4 space-y-1">
          <p>Evaluación Parcial N°1 — DSY1107 Cloud Native I</p>
          <p className="font-mono text-slate-600">MSAL React + Spring Security BFF</p>
        </div>
      </div>
    </div>
  );
};
