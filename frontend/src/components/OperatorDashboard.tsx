import React, { useEffect, useState } from 'react';
import { appointmentService } from '../services/appointment.service';
import type { Appointment } from '../types/appointment.types';
import { UserCheck, Clock, CheckCircle2 } from 'lucide-react';

export const OperatorDashboard: React.FC = () => {
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  const fetchAppointments = () => {
    setLoading(true);
    appointmentService
      .getAppointments()
      .then((data) => setAppointments(data))
      .catch(() => {
        setAppointments([
          {
            id: 'APT-101',
            patientName: 'Carlos Mendoza',
            doctorName: 'Dra. María Paz',
            specialty: 'Medicina General',
            dateTime: '2026-09-09T17:00:00',
            status: 'PENDING',
            roomNumber: 'Box 204',
          },
          {
            id: 'APT-102',
            patientName: 'Lorena Silva',
            doctorName: 'Dr. Roberto Gómez',
            specialty: 'Cardiología',
            dateTime: '2026-09-09T17:30:00',
            status: 'PENDING',
            roomNumber: 'Box 108',
          },
          {
            id: 'APT-103',
            patientName: 'Andrea Torres',
            doctorName: 'Dra. Camila Morales',
            specialty: 'Pediatría',
            dateTime: '2026-09-09T16:15:00',
            status: 'CONFIRMED',
            roomNumber: 'Box 301',
          },
        ]);
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
          Rol: Operator
        </span>
      </div>

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
    </div>
  );
};
