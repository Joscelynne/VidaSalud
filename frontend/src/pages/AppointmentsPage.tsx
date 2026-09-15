import React, { useEffect, useState } from 'react';
import { appointmentService } from '../services/appointment.service';
import api from '../services/api';
import type { Appointment } from '../types/appointment.types';
import { CalendarCheck, Filter, ShieldAlert, CheckCircle2, Loader2, Check } from 'lucide-react';

export const AppointmentsPage: React.FC = () => {
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [filter, setFilter] = useState<string>('ALL');
  const [errorStatus, setErrorStatus] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [confirmingId, setConfirmingId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  useEffect(() => {
    setLoading(true);
    setErrorStatus(null);
    appointmentService
      .getAppointments()
      .then((data) => setAppointments(data))
      .catch((err) => {
        const status = err.response?.status || 500;
        const msg = err.response?.data?.message || err.message || 'Error al cargar las atenciones desde el BFF.';
        setErrorStatus(status);
        setErrorMessage(msg);
      })
      .finally(() => setLoading(false));
  }, []);

  const handleConfirm = async (id: string) => {
    setConfirmingId(id);
    setFeedback(null);
    try {
      const response = await api.post<Appointment>(`/api/appointments/${id}/confirm`);
      const updatedAppointment = response.data;

      setAppointments((prev) =>
        prev.map((apt) =>
          apt.id === id
            ? updatedAppointment && updatedAppointment.status
              ? updatedAppointment
              : { ...apt, status: 'CONFIRMED' }
            : apt
        )
      );

      setFeedback({
        type: 'success',
        message: `La atención ${id} ha sido confirmada exitosamente.`,
      });
    } catch (err: any) {
      const status = err.response?.status;
      if (status === 403) {
        setFeedback({
          type: 'error',
          message: 'No tienes permisos para confirmar la atención médica.',
        });
      } else if (status === 401) {
        setFeedback({
          type: 'error',
          message: 'Error de autenticación. Sesión no válida o expirada.',
        });
      } else {
        const msg = err.response?.data?.message || err.message || 'Error al confirmar la atención médica.';
        setFeedback({
          type: 'error',
          message: msg,
        });
      }
    } finally {
      setConfirmingId(null);
    }
  };

  const filtered = appointments.filter((apt) => (filter === 'ALL' ? true : apt.status === filter));

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            <CalendarCheck className="w-6 h-6 text-emerald-400" /> Gestión de Atenciones Médicas
          </h2>
          <p className="text-slate-400 text-sm">Consultas médicas registradas en el sistema VidaSalud.</p>
        </div>
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-400" />
          <select
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="bg-slate-900 border border-slate-700 text-slate-200 text-xs rounded-lg px-3 py-1.5 focus:outline-none"
          >
            <option value="ALL">Todas las atenciones</option>
            <option value="PENDING">Pendientes</option>
            <option value="CONFIRMED">Confirmadas</option>
            <option value="COMPLETED">Completadas</option>
          </select>
        </div>
      </div>

      {feedback && (
        <div
          className={`p-4 rounded-xl text-xs flex items-center justify-between shadow-lg border ${
            feedback.type === 'success'
              ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
              : 'bg-rose-500/10 border-rose-500/30 text-rose-300'
          }`}
        >
          <div className="flex items-center gap-2">
            {feedback.type === 'success' ? (
              <Check className="w-4 h-4 text-emerald-400 shrink-0" />
            ) : (
              <ShieldAlert className="w-4 h-4 text-rose-400 shrink-0" />
            )}
            <span>{feedback.message}</span>
          </div>
          <button
            onClick={() => setFeedback(null)}
            className="text-slate-400 hover:text-white transition-colors cursor-pointer text-xs font-semibold px-2"
          >
            ✕
          </button>
        </div>
      )}

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
        <div className="bg-slate-900/60 border border-slate-800 rounded-xl overflow-hidden shadow-xl">
          {loading ? (
            <div className="p-8 text-center text-slate-400 animate-pulse">Cargando atenciones desde API Gateway...</div>
          ) : (
            <table className="w-full text-left text-sm text-slate-300">
              <thead className="bg-slate-800/80 text-xs uppercase tracking-wider text-slate-400 border-b border-slate-700">
                <tr>
                  <th className="p-4">ID</th>
                  <th className="p-4">Paciente</th>
                  <th className="p-4">Médico</th>
                  <th className="p-4">Especialidad</th>
                  <th className="p-4">Fecha y Hora</th>
                  <th className="p-4">Ubicación</th>
                  <th className="p-4 text-right">Estado</th>
                  <th className="p-4 text-center">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {filtered.map((apt) => (
                  <tr key={apt.id} className="hover:bg-slate-800/40 transition-colors">
                    <td className="p-4 font-mono text-slate-400">{apt.id}</td>
                    <td className="p-4 font-semibold text-white">{apt.patientName}</td>
                    <td className="p-4">{apt.doctorName}</td>
                    <td className="p-4 text-emerald-400 font-medium">{apt.specialty}</td>
                    <td className="p-4 text-xs font-mono">{new Date(apt.dateTime).toLocaleString('es-CL')}</td>
                    <td className="p-4 text-xs">{apt.roomNumber}</td>
                    <td className="p-4 text-right">
                      <span
                        className={`px-2.5 py-1 rounded-full text-xs font-semibold ${
                          apt.status === 'CONFIRMED'
                            ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                            : apt.status === 'PENDING'
                            ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                            : 'bg-blue-500/10 text-blue-400 border border-blue-500/20'
                        }`}
                      >
                        {apt.status}
                      </span>
                    </td>
                    <td className="p-4 text-center">
                      {apt.status === 'PENDING' ? (
                        <button
                          onClick={() => handleConfirm(apt.id)}
                          disabled={confirmingId === apt.id}
                          className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 disabled:cursor-not-allowed text-white font-medium text-xs rounded-lg shadow-sm transition-colors cursor-pointer inline-flex items-center gap-1.5"
                        >
                          {confirmingId === apt.id ? (
                            <>
                              <Loader2 className="w-3.5 h-3.5 animate-spin" />
                              <span>Confirmando...</span>
                            </>
                          ) : (
                            <>
                              <CheckCircle2 className="w-3.5 h-3.5" />
                              <span>Confirmar</span>
                            </>
                          )}
                        </button>
                      ) : (
                        <span className="text-xs text-slate-500">—</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
};
