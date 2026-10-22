import React, { useEffect, useState } from 'react';
import { appointmentService } from '../services/appointment.service';
import api from '../services/api';
import type { Appointment } from '../types/appointment.types';
import { useAuth } from '../hooks/useAuth';
import {
  CalendarCheck,
  CalendarPlus,
  Check,
  CheckCircle2,
  Filter,
  Loader2,
  ShieldAlert,
  X,
} from 'lucide-react';

// ── Specialties list ──────────────────────────────────────────────────────────
const SPECIALTIES = [
  'Medicina General',
  'Cardiología',
  'Dermatología',
  'Ginecología',
  'Neurología',
  'Oftalmología',
  'Pediatría',
  'Psiquiatría',
  'Traumatología',
  'Urología',
];

interface CreateForm {
  patientName: string;
  doctorName: string;
  specialty: string;
  dateTime: string;
  roomNumber: string;
  notes: string;
}

const EMPTY_FORM: CreateForm = {
  patientName: '',
  doctorName: '',
  specialty: SPECIALTIES[0],
  dateTime: '',
  roomNumber: '',
  notes: '',
};

export const AppointmentsPage: React.FC = () => {
  const { roles, user } = useAuth();

  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [filter, setFilter] = useState<string>('ALL');
  const [errorStatus, setErrorStatus] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [confirmingId, setConfirmingId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // ── Scheduling modal ────────────────────────────────────────────────────────
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState<CreateForm>(EMPTY_FORM);
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  // Roles that can schedule appointments (matches BFF RBAC)
  const canSchedule = roles.some((r) => ['Admin', 'Recepcionista', 'Paciente'].includes(r));
  const canConfirm = roles.some((r) => ['Admin', 'Recepcionista'].includes(r));

  const loadAppointments = () => {
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
  };

  useEffect(() => {
    loadAppointments();
  }, []);

  // ── Confirm ─────────────────────────────────────────────────────────────────
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
        setFeedback({ type: 'error', message: 'No tienes permisos para confirmar la atención médica.' });
      } else if (status === 401) {
        setFeedback({ type: 'error', message: 'Error de autenticación. Sesión no válida o expirada.' });
      } else {
        const msg = err.response?.data?.message || err.message || 'Error al confirmar la atención médica.';
        setFeedback({ type: 'error', message: msg });
      }
    } finally {
      setConfirmingId(null);
    }
  };

  // ── Create (schedule) ───────────────────────────────────────────────────────
  const openModal = () => {
    // Pre-fill patientName for Paciente role
    const isPaciente = roles.includes('Paciente') && !roles.includes('Admin') && !roles.includes('Recepcionista');
    setForm({
      ...EMPTY_FORM,
      patientName: isPaciente && user ? user.name : '',
    });
    setCreateError(null);
    setShowModal(true);
  };

  const handleFormChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.patientName.trim() || !form.doctorName.trim() || !form.specialty || !form.dateTime) {
      setCreateError('Los campos Paciente, Médico, Especialidad y Fecha/Hora son obligatorios.');
      return;
    }
    setCreating(true);
    setCreateError(null);
    try {
      const created = await appointmentService.createAppointment({
        patientName: form.patientName.trim(),
        doctorName: form.doctorName.trim(),
        specialty: form.specialty,
        dateTime: form.dateTime,
        roomNumber: form.roomNumber.trim() || undefined,
        notes: form.notes.trim() || undefined,
      });
      setAppointments((prev) => [created, ...prev]);
      setFeedback({
        type: 'success',
        message: `Cita ${created.id} agendada correctamente. Se publicó un evento a RabbitMQ.`,
      });
      setShowModal(false);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Error al agendar la cita.';
      setCreateError(msg);
    } finally {
      setCreating(false);
    }
  };

  const filtered = appointments.filter((apt) => (filter === 'ALL' ? true : apt.status === filter));

  return (
    <div className="space-y-6">
      {/* ── Header ─────────────────────────────────────────────────────────── */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            <CalendarCheck className="w-6 h-6 text-emerald-400" /> Gestión de Atenciones Médicas
          </h2>
          <p className="text-slate-400 text-sm">Consultas médicas registradas en el sistema VidaSalud.</p>
        </div>
        <div className="flex items-center gap-3">
          {/* Filter */}
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

          {/* Schedule button – visible to Admin, Recepcionista, Paciente */}
          {canSchedule && (
            <button
              id="btn-agendar-cita"
              onClick={openModal}
              className="flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-sm rounded-lg shadow-lg shadow-emerald-900/30 transition-all cursor-pointer"
            >
              <CalendarPlus className="w-4 h-4" />
              Agendar Cita
            </button>
          )}
        </div>
      </div>

      {/* ── Feedback banner ─────────────────────────────────────────────────── */}
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

      {/* ── Error states ─────────────────────────────────────────────────────── */}
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

      {/* ── Table ────────────────────────────────────────────────────────────── */}
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
                {filtered.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="p-8 text-center text-slate-500 text-sm">
                      No hay atenciones que mostrar.
                    </td>
                  </tr>
                ) : (
                  filtered.map((apt) => (
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
                        {apt.status === 'PENDING' && canConfirm ? (
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
                  ))
                )}
              </tbody>
            </table>
          )}
        </div>
      )}

      {/* ── Scheduling modal ─────────────────────────────────────────────────── */}
      {showModal && (
        <div
          id="modal-agendar-cita"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4"
          onClick={(e) => e.target === e.currentTarget && setShowModal(false)}
        >
          <div className="bg-slate-900 border border-slate-700 rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden">
            {/* Modal header */}
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800">
              <h3 className="text-base font-bold text-white flex items-center gap-2">
                <CalendarPlus className="w-5 h-5 text-emerald-400" />
                Agendar Nueva Cita
              </h3>
              <button
                onClick={() => setShowModal(false)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Form */}
            <form onSubmit={handleCreate} className="px-6 py-5 space-y-4">
              {createError && (
                <div className="bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs rounded-lg p-3 flex items-start gap-2">
                  <ShieldAlert className="w-4 h-4 shrink-0 mt-0.5" />
                  <span>{createError}</span>
                </div>
              )}

              {/* RabbitMQ info badge */}
              <div className="bg-emerald-500/5 border border-emerald-500/20 rounded-lg px-3 py-2 text-[11px] text-emerald-400">
                Al confirmar, se publicará un evento <code className="font-mono">AppointmentCreatedEvent</code> en
                RabbitMQ (<code className="font-mono">citas.agendamiento.queue</code>).
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2">
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Nombre del Paciente <span className="text-rose-400">*</span>
                  </label>
                  <input
                    id="input-patient-name"
                    name="patientName"
                    type="text"
                    required
                    value={form.patientName}
                    onChange={handleFormChange}
                    placeholder="Ej: María González López"
                    className="w-full bg-slate-800 border border-slate-700 text-slate-200 text-sm rounded-lg px-3 py-2 focus:outline-none focus:ring-1 focus:ring-emerald-500 placeholder:text-slate-600"
                  />
                </div>

                <div className="col-span-2">
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Nombre del Médico <span className="text-rose-400">*</span>
                  </label>
                  <input
                    id="input-doctor-name"
                    name="doctorName"
                    type="text"
                    required
                    value={form.doctorName}
                    onChange={handleFormChange}
                    placeholder="Ej: Dr. Carlos Pérez Soto"
                    className="w-full bg-slate-800 border border-slate-700 text-slate-200 text-sm rounded-lg px-3 py-2 focus:outline-none focus:ring-1 focus:ring-emerald-500 placeholder:text-slate-600"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Especialidad <span className="text-rose-400">*</span>
                  </label>
                  <select
                    id="input-specialty"
                    name="specialty"
                    required
                    value={form.specialty}
                    onChange={handleFormChange}
                    className="w-full bg-slate-800 border border-slate-700 text-slate-200 text-sm rounded-lg px-3 py-2 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                  >
                    {SPECIALTIES.map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Fecha y Hora <span className="text-rose-400">*</span>
                  </label>
                  <input
                    id="input-date-time"
                    name="dateTime"
                    type="datetime-local"
                    required
                    value={form.dateTime}
                    onChange={handleFormChange}
                    className="w-full bg-slate-800 border border-slate-700 text-slate-200 text-sm rounded-lg px-3 py-2 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Box / Sala</label>
                  <input
                    id="input-room-number"
                    name="roomNumber"
                    type="text"
                    value={form.roomNumber}
                    onChange={handleFormChange}
                    placeholder="Ej: Box 201"
                    className="w-full bg-slate-800 border border-slate-700 text-slate-200 text-sm rounded-lg px-3 py-2 focus:outline-none focus:ring-1 focus:ring-emerald-500 placeholder:text-slate-600"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Observaciones</label>
                  <textarea
                    id="input-notes"
                    name="notes"
                    rows={2}
                    value={form.notes}
                    onChange={handleFormChange}
                    placeholder="Indicaciones, alergias, etc."
                    className="w-full bg-slate-800 border border-slate-700 text-slate-200 text-sm rounded-lg px-3 py-2 focus:outline-none focus:ring-1 focus:ring-emerald-500 placeholder:text-slate-600 resize-none"
                  />
                </div>
              </div>

              {/* Actions */}
              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 text-sm text-slate-400 hover:text-white bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-lg transition-colors cursor-pointer"
                >
                  Cancelar
                </button>
                <button
                  id="btn-submit-agendar"
                  type="submit"
                  disabled={creating}
                  className="flex items-center gap-2 px-5 py-2 text-sm font-semibold bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg shadow-lg shadow-emerald-900/30 transition-all cursor-pointer"
                >
                  {creating ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      Agendando…
                    </>
                  ) : (
                    <>
                      <CalendarPlus className="w-4 h-4" />
                      Confirmar Cita
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
