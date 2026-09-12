import React, { useEffect, useState } from 'react';
import { auditService } from '../services/audit.service';
import type { AuditLogEntry } from '../types/audit.types';
import { History, Terminal, ShieldAlert } from 'lucide-react';

export const AuditorDashboard: React.FC = () => {
  const [logs, setLogs] = useState<AuditLogEntry[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [errorStatus, setErrorStatus] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setErrorStatus(null);
    auditService
      .getAuditLogs()
      .then((data) => setLogs(data))
      .catch((err) => {
        const status = err.response?.status || 500;
        const msg = err.response?.data?.message || err.message || 'Error al obtener registros de auditoría desde BFF.';
        setErrorStatus(status);
        setErrorMessage(msg);
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            <History className="w-6 h-6 text-amber-400" /> Timeline de Auditoría y Trazabilidad
          </h2>
          <p className="text-slate-400 text-sm">Registro inmutable de peticiones y validaciones JWT en el BFF.</p>
        </div>
        <span className="px-3 py-1 bg-amber-500/10 text-amber-400 border border-amber-500/20 text-xs font-semibold rounded-full">
          Rol: Auditor
        </span>
      </div>

      {errorStatus === 401 && (
        <div className="bg-slate-900 border border-rose-500/40 rounded-xl p-6 text-center space-y-3 shadow-xl">
          <div className="w-10 h-10 bg-rose-500/10 text-rose-400 rounded-full flex items-center justify-center mx-auto border border-rose-500/20">
            <ShieldAlert className="w-5 h-5" />
          </div>
          <h3 className="text-base font-bold text-white">401 — Error de Autenticación</h3>
          <p className="text-xs text-slate-300 max-w-md mx-auto">{errorMessage}</p>
        </div>
      )}

      {errorStatus === 403 && (
        <div className="bg-slate-900 border border-amber-500/40 rounded-xl p-6 text-center space-y-3 shadow-xl">
          <div className="w-10 h-10 bg-amber-500/10 text-amber-400 rounded-full flex items-center justify-center mx-auto border border-amber-500/20">
            <ShieldAlert className="w-5 h-5" />
          </div>
          <h3 className="text-base font-bold text-white">403 — Acceso Denegado</h3>
          <p className="text-xs text-slate-300 max-w-md mx-auto">{errorMessage}</p>
        </div>
      )}

      {errorStatus !== null && errorStatus !== 401 && errorStatus !== 403 && (
        <div className="bg-slate-900 border border-slate-700 rounded-xl p-6 text-center space-y-3 shadow-xl">
          <h3 className="text-base font-bold text-white">Error de Conexión BFF</h3>
          <p className="text-xs text-slate-300 max-w-md mx-auto">{errorMessage}</p>
        </div>
      )}

      {loading ? (
        <div className="p-8 text-center text-slate-400 animate-pulse">Cargando timeline de eventos...</div>
      ) : !errorStatus && (
        <div className="relative border-l-2 border-slate-700/60 ml-4 space-y-6 pl-6 py-2">
          {logs.map((log) => (
            <div key={log.id} className="relative group">
              <div className={`absolute -left-[31px] top-1.5 w-4 h-4 rounded-full border-2 ${
                log.status === 'SUCCESS' ? 'bg-emerald-500 border-slate-900' : 'bg-rose-500 border-slate-900'
              }`} />

              <div className="bg-slate-800/50 border border-slate-700/60 rounded-xl p-4 space-y-2 hover:border-slate-600 transition-colors">
                <div className="flex items-center justify-between text-xs text-slate-400 font-mono">
                  <span>{new Date(log.timestamp).toLocaleString('es-CL')}</span>
                  <span className="bg-slate-900 px-2 py-0.5 rounded border border-slate-800 text-slate-300">
                    TraceId: {log.traceId}
                  </span>
                </div>

                <div className="flex items-center justify-between">
                  <h4 className="font-bold text-white text-base font-mono">{log.action}</h4>
                  <span className={`px-2 py-0.5 text-xs font-bold font-mono rounded ${
                    log.status === 'SUCCESS' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-rose-500/10 text-rose-400'
                  }`}>
                    {log.status}
                  </span>
                </div>

                <div className="text-xs text-slate-300 grid grid-cols-2 gap-2 pt-1 font-mono">
                  <div><span className="text-slate-500">Usuario:</span> {log.user}</div>
                  <div><span className="text-slate-500">IP:</span> {log.ipAddress}</div>
                </div>

                {log.details && (
                  <p className="text-xs text-slate-400 bg-slate-900/60 p-2 rounded border border-slate-800/80 font-mono flex items-center gap-2">
                    <Terminal className="w-3.5 h-3.5 text-slate-500" /> {log.details}
                  </p>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
