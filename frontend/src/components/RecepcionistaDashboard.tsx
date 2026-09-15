import React, { useEffect, useState } from 'react';
import { appointmentService } from '../services/appointment.service';
import type { Appointment } from '../types/appointment.types';
import { UserCheck, Clock, CheckCircle2, ShieldAlert } from 'lucide-react';

export const RecepcionistaDashboard: React.FC = () => {
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  const [errorStatus, setErrorStatus] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const fetchAppointments = () => {
    setLoading(true);
    setErrorStatus(null);
    appointmentService
      .getAppointments()
      .then((data) => setAppointments(data))
      .catch((err) => {
        const status = err.response?.status || 500;
        const msg = err.response?.data?.message || err.message || 'Error al obtener pacientes en espera desde BFF.';
        setErrorStatus(status);
        setErrorMessage(msg);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchAppointments();
  }, []);

  const handleConfirm = async (id: string) => {
    try {
      await appointmentService.confirmAppointment(id);
      fetchAppointments();
    } catch {
      setAppointments((prev) =>
        prev.map((apt) => (apt.id === id ? { ...apt, status: 'CONFIRMED' } : apt))
      );
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            <UserCheck className="w-6 h-6 text-blue-400" /> Sala de Espera y Admisión
          </h2>
          <p className="text-slate-400 text-sm">Gestión de flujo de pacientes y recepción presencial.</p>
        </div>
        <span className="px-3 py-1 bg-blue-500/10 text-blue-400 border border-blue-500/20 text-xs font-semibold rounded-full">
          Rol: Recepcionista
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

      {!errorStatus && (
        <div className="bg-slate-800/40 border border-slate-700/60 rounded-xl overflow-hidden">
          <div className="p-4 border-b border-slate-700/60 bg-slate-900/40 flex items-center justify-between">
            <span className="font-semibold text-slate-200 text-sm">Pacientes en Espera (Hoy)</span>
            <span className="text-xs text-slate-400 font-mono">Actualizado en tiempo real</span>
          </div>

          {loading ? (
            <div className="p-8 text-center text-slate-400 animate-pulse">Cargando sala de espera...</div>
          ) : (
          <div className="divide-y divide-slate-700/50">
            {appointments.map((apt) => (
              <div key={apt.id} className="p-4 flex items-center justify-between hover:bg-slate-800/30 transition-colors">
                <div className="flex items-center gap-4">
                  <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold ${
                    apt.status === 'CONFIRMED' ? 'bg-emerald-500/20 text-emerald-400' : 'bg-amber-500/20 text-amber-400'
                  }`}>
                    {apt.status === 'CONFIRMED' ? <CheckCircle2 className="w-5 h-5" /> : <Clock className="w-5 h-5" />}
                  </div>
                  <div>
                    <h4 className="font-semibold text-white text-base">{apt.patientName}</h4>
                    <p className="text-xs text-slate-400">
                      {apt.specialty} — {apt.doctorName} ({apt.roomNumber})
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <span className={`px-2.5 py-1 rounded-full text-xs font-mono font-semibold ${
                    apt.status === 'CONFIRMED' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                  }`}>
                    {apt.status}
                  </span>

                  {apt.status === 'PENDING' && (
                    <button
                      onClick={() => handleConfirm(apt.id)}
                      className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs rounded-lg shadow-sm transition-colors cursor-pointer"
                    >
                      Confirmar Atención
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
      )}
    </div>
  );
};
