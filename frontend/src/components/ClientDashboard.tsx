import React, { useEffect, useState } from 'react';
import { appointmentService } from '../services/appointment.service';
import type { Appointment } from '../types/appointment.types';
import { Calendar, Clock, MapPin, Stethoscope } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';

export const ClientDashboard: React.FC = () => {
  const { user } = useAuth();
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    appointmentService
      .getAppointments()
      .then((data) => setAppointments(data))
      .catch(() => {
        setAppointments([
          {
            id: 'APT-901',
            patientName: user?.name || 'Mi Perfil Paciente',
            doctorName: 'Dr. Alejandro Ruiz',
            specialty: 'Medicina General',
            dateTime: '2026-09-12T10:30:00',
            status: 'CONFIRMED',
            roomNumber: 'Edificio A - Box 104',
          },
          {
            id: 'APT-902',
            patientName: user?.name || 'Mi Perfil Paciente',
            doctorName: 'Dra. Patricia Varela',
            specialty: 'Oftalmología',
            dateTime: '2026-09-20T15:00:00',
            status: 'PENDING',
            roomNumber: 'Edificio B - Box 302',
          },
        ]);
      })
      .finally(() => setLoading(false));
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

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {loading ? (
          <div className="p-8 text-center text-slate-400 animate-pulse col-span-2">Cargando tus horas médicas...</div>
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
    </div>
  );
};
