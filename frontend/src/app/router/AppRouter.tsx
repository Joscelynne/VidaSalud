import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { MainLayout } from '../../layouts/MainLayout';
import { ProtectedRoute } from '../../guards/ProtectedRoute';
import { RoleGuard } from '../../guards/RoleGuard';

import { LoginPage } from '../../pages/LoginPage';
import { DashboardPage } from '../../pages/DashboardPage';
import { AppointmentsPage } from '../../pages/AppointmentsPage';
import { CatalogPage } from '../../pages/CatalogPage';
import { ReportsPage } from '../../pages/ReportsPage';
import { AuditPage } from '../../pages/AuditPage';
import { UnauthorizedPage } from '../../pages/UnauthorizedPage';

export const AppRouter: React.FC = () => {
  return (
    <Routes>
      {/* Ruta pública de Autenticación con MSAL */}
      <Route path="/login" element={<LoginPage />} />

      {/* Rutas Protegidas en MainLayout */}
      <Route
        element={
          <ProtectedRoute>
            <MainLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/dashboard" element={<DashboardPage />} />

        {/* Atenciones Médicas: Admin, Operator, Client */}
        <Route
          path="/appointments"
          element={
            <RoleGuard allowedRoles={['Admin', 'Operator', 'Client']}>
              <AppointmentsPage />
            </RoleGuard>
          }
        />

        {/* Catálogo de Especialidades: Admin, Operator */}
        <Route
          path="/catalog"
          element={
            <RoleGuard allowedRoles={['Admin', 'Operator']}>
              <CatalogPage />
            </RoleGuard>
          }
        />

        {/* Reportería Gerencial: Admin */}
        <Route
          path="/reports"
          element={
            <RoleGuard allowedRoles={['Admin']}>
              <ReportsPage />
            </RoleGuard>
          }
        />

        {/* Auditoría: Admin, Auditor */}
        <Route
          path="/audit"
          element={
            <RoleGuard allowedRoles={['Admin', 'Auditor']}>
              <AuditPage />
            </RoleGuard>
          }
        />

        {/* Página de Sin Autorización (403) */}
        <Route path="/unauthorized" element={<UnauthorizedPage />} />
      </Route>

      {/* Redirección por defecto */}
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
};
