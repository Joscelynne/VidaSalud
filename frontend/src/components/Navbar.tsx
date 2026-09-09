import React from 'react';
import { useAuth } from '../hooks/useAuth';
import { RoleBadge } from './RoleBadge';
import { LogOut, ShieldCheck } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, logout, roles } = useAuth();

  return (
    <header className="h-16 bg-slate-900/80 backdrop-blur-md border-b border-slate-800 px-6 flex items-center justify-between sticky top-0 z-40">
      <div className="flex items-center gap-3">
        <span className="text-xs px-2.5 py-1 rounded-md bg-emerald-500/10 text-emerald-400 font-mono border border-emerald-500/20 flex items-center gap-1.5">
          <ShieldCheck className="w-3.5 h-3.5" /> Azure AD Secured (MSAL)
        </span>
      </div>

      <div className="flex items-center gap-4">
        {user && (
          <div className="flex items-center gap-3 bg-slate-800/60 px-3 py-1.5 rounded-lg border border-slate-700/50">
            <div className="w-8 h-8 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold text-sm">
              {user.name.charAt(0).toUpperCase()}
            </div>
            <div className="flex flex-col text-left">
              <span className="text-sm font-semibold text-slate-200">{user.name}</span>
              <span className="text-xs text-slate-400">{user.preferredUsername}</span>
            </div>
            <div className="flex gap-1 ml-2">
              {roles.map((r) => (
                <RoleBadge key={r} role={r} />
              ))}
            </div>
          </div>
        )}

        <button
          onClick={logout}
          className="flex items-center gap-2 px-3 py-1.5 text-sm font-medium text-slate-300 hover:text-rose-400 bg-slate-800 hover:bg-rose-950/40 border border-slate-700 hover:border-rose-900/50 rounded-lg transition-colors cursor-pointer"
          title="Cerrar sesión (logoutRedirect)"
        >
          <LogOut className="w-4 h-4" />
          <span>Cerrar Sesión</span>
        </button>
      </div>
    </header>
  );
};
