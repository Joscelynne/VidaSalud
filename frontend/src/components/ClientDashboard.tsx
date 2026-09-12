import React, { useEffect, useState } from 'react';
import { appointmentService } from '../services/appointment.service';
import type { Appointment } from '../types/appointment.types';
import { Calendar, Clock, MapPin, Stethoscope, AlertTriangle, ShieldAlert, RefreshCw } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';

export const ClientDashboard: React.FC = () => {
  const { user, login } = useAuth();
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [errorStatus, setErrorStatus] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const fetchAppointments = () => {
    setLoading(true);
    setErrorStatus(null);
    setErrorMessage(null);
    appointmentService
      .getAppointments()
      .then((data) => {
        setAppointments(data);
      })
      .catch((err) => {
        const status = err.response?.status || 500;
        const msg = err.response?.data?.message || err.message || 'Error al conectar con el servidor BFF.';
        setErrorStatus(status);
        setErrorMessage(msg);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchAppointments();
  }, [user]);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            <Calendar className="w-6 h-6 text-emerald-400" /> Portal Paciente — Mis Atenciones
          </h2>
          <p className="text-slate-400 text-sm">Consulta el estado de tus citas médicas y tu historial de salud.</p>
        </div>
        <span className="px-3 py-1 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-xs font-semibold rounded-full">
          Rol: Client
        </span>
      </div>

      {errorStatus === 401 && (
        <div className="bg-slate-900 border border-rose-500/40 rounded-xl p-6 text-center space-y-4 shadow-xl">
          <div className="w-12 h-12 bg-rose-500/10 text-rose-400 border border-rose-500/20 rounded-full flex items-center justify-center mx-auto">
            <ShieldAlert className="w-6 h-6" />
          </div>
          <h3 className="text-lg font-bold text-white">401 — Error de Autenticación</h3>
          <p className="text-sm text-slate-300 max-w-md mx-auto">{errorMessage}</p>
          <button
            onClick={login}
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs rounded-lg transition-colors cursor-pointer inline-flex items-center gap-2"
          >
            <RefreshCw className="w-4 h-4" /> Iniciar Sesión con Microsoft
          </button>
        </div>
      )}

      {errorStatus === 403 && (
        <div className="bg-slate-900 border border-amber-500/40 rounded-xl p-6 text-center space-y-4 shadow-xl">
          <div className="w-12 h-12 bg-amber-500/10 text-amber-400 border border-amber-500/20 rounded-full flex items-center justify-center mx-auto">
            <ShieldAlert className="w-6 h-6" />
          </div>
          <h3 className="text-lg font-bold text-white">403 — Acceso Denegado</h3>
          <p className="text-sm text-slate-300 max-w-md mx-auto">{errorMessage}</p>
        </div>
      )}

      {errorStatus !== null && errorStatus !== 401 && errorStatus !== 403 && (
        <div className="bg-slate-900 border border-slate-700 rounded-xl p-6 text-center space-y-4 shadow-xl">
          <div className="w-12 h-12 bg-slate-800 text-slate-400 border border-slate-700 rounded-full flex items-center justify-center mx-auto">
            <AlertTriangle className="w-6 h-6" />
          </div>
          <h3 className="text-lg font-bold text-white">Error de Conexión BFF</h3>
          <p className="text-sm text-slate-300 max-w-md mx-auto">{errorMessage}</p>
          <button
            onClick={fetchAppointments}
            className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold text-xs rounded-lg border border-slate-700 transition-colors cursor-pointer inline-flex items-center gap-2"
          >
            <RefreshCw className="w-4 h-4" /> Reintentar Carga
          </button>
        </div>
      )}

      {!errorStatus && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {loading ? (
            <div className="p-8 text-center text-slate-400 animate-pulse col-span-2">Cargando tus horas médicas...</div>
          ) : appointments.length === 0 ? (
            <div className="p-8 text-center text-slate-400 col-span-2 border border-slate-800 rounded-xl bg-slate-900/50">
              No registras horas médicas en el sistema.
            </div>
          ) : (
            appointments.map((apt) => (
              <div key={apt.id} className="bg-slate-800/50 border border-slate-700/60 rounded-xl p-5 hover:border-emerald-500/40 transition-colors space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-mono px-2 py-0.5 rounded bg-slate-900 text-slate-400 border border-slate-700">
                    {apt.id}
                  </span>
                  <span className={`px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                    apt.status === 'CONFIRMED' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30' : 'bg-amber-500/10 text-amber-400 border border-amber-500/30'
                  }`}>
                    {apt.status === 'CONFIRMED' ? 'Confirmada' : 'Pendiente'}
                  </span>
                </div>

                <div>
                  <h3 className="font-bold text-white text-lg flex items-center gap-2">
                    <Stethoscope className="w-5 h-5 text-emerald-400" /> {apt.specialty}
                  </h3>
                  <p className="text-sm text-slate-300 font-medium">Médico: {apt.doctorName}</p>
                </div>

                <div className="space-y-1.5 text-xs text-slate-400 pt-2 border-t border-slate-700/50">
                  <div className="flex items-center gap-2">
                    <Clock className="w-4 h-4 text-slate-500" />
                    <span>{new Date(apt.dateTime).toLocaleString('es-CL', { dateStyle: 'full', timeStyle: 'short' })}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <MapPin className="w-4 h-4 text-slate-500" />
                    <span>{apt.roomNumber}</span>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
};
