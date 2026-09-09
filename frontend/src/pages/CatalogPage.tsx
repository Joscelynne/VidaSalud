import React, { useEffect, useState } from 'react';
import { catalogService } from '../services/catalog.service';
import type { MedicalSpecialty } from '../types/catalog.types';
import { BookOpen, Stethoscope, Users, DollarSign } from 'lucide-react';

export const CatalogPage: React.FC = () => {
  const [specialties, setSpecialties] = useState<MedicalSpecialty[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    catalogService
      .getSpecialties()
      .then((data) => setSpecialties(data))
      .catch(() => {
        setSpecialties([
          {
            id: 'SPEC-01',
            name: 'Medicina General',
            description: 'Atención primaria integral para adultos y jóvenes.',
            availableDoctorsCount: 14,
            consultationFee: 25000,
            active: true,
          },
          {
            id: 'SPEC-02',
            name: 'Pediatría',
            description: 'Cuidado especializado para recién nacidos, niños y adolescentes.',
            availableDoctorsCount: 8,
            consultationFee: 32000,
            active: true,
          },
          {
            id: 'SPEC-03',
            name: 'Cardiología',
            description: 'Diagnóstico y tratamiento de patologías del sistema cardiovascular.',
            availableDoctorsCount: 5,
            consultationFee: 45000,
            active: true,
          },
          {
            id: 'SPEC-04',
            name: 'Dermatología',
            description: 'Tratamiento clínico y quirúrgico de enfermedades de la piel.',
            availableDoctorsCount: 6,
            consultationFee: 40000,
            active: true,
          },
        ]);
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            <BookOpen className="w-6 h-6 text-blue-400" /> Catálogo de Especialidades Médicas
          </h2>
          <p className="text-slate-400 text-sm">Especialidades disponibles en la red de clínicas VidaSalud.</p>
        </div>
        <span className="px-3 py-1 bg-blue-500/10 text-blue-400 border border-blue-500/20 text-xs font-semibold rounded-full">
          Ruta Protegida: Admin & Operator
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {loading ? (
          <div className="p-8 text-center text-slate-400 animate-pulse col-span-2">Cargando catálogo desde BFF...</div>
        ) : (
          specialties.map((spec) => (
            <div key={spec.id} className="bg-slate-900/60 border border-slate-800 rounded-xl p-6 space-y-4 hover:border-blue-500/30 transition-colors">
              <div className="flex items-center justify-between">
                <h3 className="text-xl font-bold text-white flex items-center gap-2">
                  <Stethoscope className="w-5 h-5 text-blue-400" /> {spec.name}
                </h3>
                <span className="text-xs font-mono text-slate-500 px-2 py-0.5 bg-slate-950 rounded border border-slate-800">
                  {spec.id}
                </span>
              </div>

              <p className="text-slate-300 text-sm">{spec.description}</p>

              <div className="flex items-center justify-between pt-4 border-t border-slate-800 text-xs text-slate-400">
                <div className="flex items-center gap-1.5">
                  <Users className="w-4 h-4 text-emerald-400" />
                  <span>{spec.availableDoctorsCount} Médicos Disponibles</span>
                </div>
                <div className="flex items-center gap-1 font-semibold text-white text-sm">
                  <DollarSign className="w-4 h-4 text-emerald-400" />
                  <span>${spec.consultationFee.toLocaleString('es-CL')}</span>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
