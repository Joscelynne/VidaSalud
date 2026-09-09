import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldAlert, ArrowLeft } from 'lucide-react';
import { useRoles } from '../hooks/useRoles';
import { RoleBadge } from '../components/RoleBadge';

export const UnauthorizedPage: React.FC = () => {
  const navigate = useNavigate();
  const { roles } = useRoles();

  return (
    <div className="min-h-[75vh] flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-slate-900/80 border border-rose-500/30 rounded-2xl p-8 text-center shadow-2xl space-y-6">
        <div className="w-16 h-16 bg-rose-500/10 text-rose-500 rounded-full flex items-center justify-center mx-auto border border-rose-500/20 animate-pulse">
          <ShieldAlert className="w-10 h-10" />
        </div>

        <div className="space-y-2">
          <h2 className="text-2xl font-extrabold text-white">403 — Acceso Denegado</h2>
          <p className="text-sm text-slate-300">
            Tu cuenta autenticada no posee los roles ni privilegios necesarios en Azure AD para acceder a este recurso.
          </p>
        </div>

        <div className="bg-slate-950/80 p-4 rounded-xl border border-slate-800 text-xs text-left space-y-2 font-mono">
          <div className="text-slate-400">Tus roles actuales en el JWT:</div>
          <div className="flex flex-wrap gap-1.5">
            {roles.length > 0 ? (
              roles.map((r) => <RoleBadge key={r} role={r} />)
            ) : (
              <span className="text-slate-500 italic">Sin roles asignados</span>
            )}
          </div>
        </div>

        <button
          onClick={() => navigate('/dashboard')}
          className="w-full py-2.5 px-4 bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold rounded-xl border border-slate-700 transition-colors flex items-center justify-center gap-2 cursor-pointer"
        >
          <ArrowLeft className="w-4 h-4" /> Volver al Dashboard Principal
        </button>
      </div>
    </div>
  );
};
