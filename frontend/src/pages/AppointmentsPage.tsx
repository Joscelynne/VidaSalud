import React, { useEffect, useState } from 'react';
import { appointmentService } from '../services/appointment.service';
import type { Appointment } from '../types/appointment.types';
import { CalendarCheck, Filter } from 'lucide-react';

export const AppointmentsPage: React.FC = () => {
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [filter, setFilter] = useState<string>('ALL');

  useEffect(() => {
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
            status: 'CONFIRMED',
            roomNumber: 'Box 108',
          },
          {
            id: 'APT-103',
            patientName: 'Felipe Araya',
            doctorName: 'Dra. Camila Morales',
            specialty: 'Pediatría',
            dateTime: '2026-09-10T11:00:00',
            status: 'COMPLETED',
            roomNumber: 'Box 301',
          },
        ]);
      })
      .finally(() => setLoading(false));
  }, []);

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
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};
