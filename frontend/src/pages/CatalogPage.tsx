import React, { useEffect, useState } from 'react';
import { catalogService } from '../services/catalog.service';
import type { MedicalSpecialty } from '../types/catalog.types';
import { BookOpen, Stethoscope, Users, DollarSign, ShieldAlert } from 'lucide-react';

export const CatalogPage: React.FC = () => {
  const [specialties, setSpecialties] = useState<MedicalSpecialty[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [errorStatus, setErrorStatus] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setErrorStatus(null);
    catalogService
      .getSpecialties()
      .then((data) => setSpecialties(data))
      .catch((err) => {
        const status = err.response?.status || 500;
        const msg = err.response?.data?.message || err.message || 'Error al cargar el catálogo de especialidades desde el BFF.';
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
            <BookOpen className="w-6 h-6 text-blue-400" /> Catálogo de Especialidades Médicas
          </h2>
          <p className="text-slate-400 text-sm">Especialidades disponibles en la red de clínicas VidaSalud.</p>
        </div>
        <span className="px-3 py-1 bg-blue-500/10 text-blue-400 border border-blue-500/20 text-xs font-semibold rounded-full">
          Ruta Protegida: Admin & Recepcionista
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
      )}
    </div>
  );
};
