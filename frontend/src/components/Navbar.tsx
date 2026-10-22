import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { RoleBadge } from './RoleBadge';
import { Bell, Check, LogOut, RefreshCw, ShieldCheck, X } from 'lucide-react';
import { notificationService } from '../services/notification.service';
import type { NotificationItem } from '../types/notification.types';

/** Intervalo de polling en milisegundos (30 segundos). No usa WebSockets. */
const POLL_INTERVAL_MS = 30_000;

export const Navbar: React.FC = () => {
  const { user, logout, roles } = useAuth();

  // ─── Notificaciones (polling periódico, no real-time) ─────────────────────
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [bellOpen, setBellOpen] = useState(false);
  const [polling, setPolling] = useState(false);
  const bellRef = useRef<HTMLDivElement>(null);
  const intervalRef = useRef<number | null>(null);

  const fetchNotifications = useCallback(async () => {
    try {
      const data = await notificationService.getNotifications();
      setNotifications(data);
    } catch {
      // silencioso: no mostramos error en la navbar
    }
  }, []);

  // Poll on mount + interval
  useEffect(() => {
    fetchNotifications();
    intervalRef.current = window.setInterval(fetchNotifications, POLL_INTERVAL_MS);
    return () => {
      if (intervalRef.current !== null) clearInterval(intervalRef.current);
    };
  }, [fetchNotifications]);

  // Close dropdown on outside click
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (bellRef.current && !bellRef.current.contains(e.target as Node)) {
        setBellOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const unreadCount = notifications.filter((n) => !n.read).length;

  const handleMarkAsRead = async (id: string) => {
    await notificationService.markAsRead(id);
    setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)));
  };

  const handleMarkAllAsRead = async () => {
    await notificationService.markAllAsRead();
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  const handleManualRefresh = async () => {
    setPolling(true);
    await fetchNotifications();
    setPolling(false);
  };

  // ──────────────────────────────────────────────────────────────────────────
  return (
    <header className="h-16 bg-slate-900/80 backdrop-blur-md border-b border-slate-800 px-6 flex items-center justify-between sticky top-0 z-40">
      <div className="flex items-center gap-3">
        <span className="text-xs px-2.5 py-1 rounded-md bg-emerald-500/10 text-emerald-400 font-mono border border-emerald-500/20 flex items-center gap-1.5">
          <ShieldCheck className="w-3.5 h-3.5" /> Azure AD Secured (MSAL)
        </span>
      </div>

      <div className="flex items-center gap-4">
        {/* ── Bell dropdown ─────────────────────────────────────────────── */}
        <div ref={bellRef} className="relative">
          <button
            id="notification-bell-btn"
            onClick={() => setBellOpen((prev) => !prev)}
            className="relative p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
            title="Notificaciones (polling cada 30 s)"
            aria-label="Abrir notificaciones"
          >
            <Bell className="w-5 h-5" />
            {unreadCount > 0 && (
              <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] bg-rose-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center px-0.5 animate-pulse">
                {unreadCount > 99 ? '99+' : unreadCount}
              </span>
            )}
          </button>

          {bellOpen && (
            <div
              id="notification-dropdown"
              className="absolute right-0 top-12 w-80 bg-slate-900 border border-slate-700 rounded-xl shadow-2xl z-50 overflow-hidden"
            >
              {/* Header */}
              <div className="flex items-center justify-between px-4 py-3 border-b border-slate-800">
                <span className="text-sm font-semibold text-white">Notificaciones</span>
                <div className="flex items-center gap-2">
                  <span className="text-[10px] text-slate-500 italic">polling / 30 s</span>
                  <button
                    id="notification-refresh-btn"
                    onClick={handleManualRefresh}
                    disabled={polling}
                    className="p-1 rounded text-slate-400 hover:text-white hover:bg-slate-800 disabled:opacity-50 transition-colors cursor-pointer"
                    title="Actualizar ahora (polling manual)"
                  >
                    <RefreshCw className={`w-3.5 h-3.5 ${polling ? 'animate-spin' : ''}`} />
                  </button>
                  {unreadCount > 0 && (
                    <button
                      id="notification-mark-all-btn"
                      onClick={handleMarkAllAsRead}
                      className="text-[10px] text-emerald-400 hover:text-emerald-300 transition-colors cursor-pointer"
                    >
                      Leer todo
                    </button>
                  )}
                  <button
                    onClick={() => setBellOpen(false)}
                    className="p-1 rounded text-slate-500 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>

              {/* List */}
              <ul className="max-h-72 overflow-y-auto divide-y divide-slate-800/60">
                {notifications.length === 0 ? (
                  <li className="p-6 text-center text-xs text-slate-500">Sin notificaciones</li>
                ) : (
                  notifications.map((n) => (
                    <li
                      key={n.id}
                      className={`flex items-start gap-3 px-4 py-3 cursor-pointer transition-colors hover:bg-slate-800/60 ${
                        n.read ? 'opacity-60' : ''
                      }`}
                      onClick={() => !n.read && handleMarkAsRead(n.id)}
                    >
                      {/* Unread dot */}
                      <span
                        className={`mt-1.5 w-2 h-2 rounded-full shrink-0 ${
                          n.read ? 'bg-slate-700' : 'bg-emerald-400'
                        }`}
                      />
                      <div className="flex-1 min-w-0">
                        <p className="text-xs font-semibold text-white truncate">{n.title}</p>
                        <p className="text-[11px] text-slate-400 line-clamp-2">{n.message}</p>
                        {n.createdAt && (
                          <p className="text-[10px] text-slate-600 mt-0.5">
                            {new Date(n.createdAt).toLocaleString('es-CL')}
                          </p>
                        )}
                      </div>
                      {!n.read && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleMarkAsRead(n.id);
                          }}
                          className="shrink-0 p-1 rounded text-slate-500 hover:text-emerald-400 transition-colors cursor-pointer"
                          title="Marcar como leída"
                        >
                          <Check className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </li>
                  ))
                )}
              </ul>
            </div>
          )}
        </div>

        {/* ── User info ─────────────────────────────────────────────────── */}
        {user && (
          <div className="flex items-center gap-3 bg-slate-800/60 px-3 py-1.5 rounded-lg border border-slate-700/50">
            <div className="w-8 h-8 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold text-sm">
              {user.name.charAt(0).toUpperCase()}
            </div>
            <div className="flex flex-col text-left">
              <span className="text-sm font-semibold text-slate-200">{user.name}</span>
              <span className="text-xs text-slate-400">{user.preferredUsername}</span>
            </div>
            <div className="flex gap-1 ml-2">
              {roles.map((r) => (
                <RoleBadge key={r} role={r} />
              ))}
            </div>
          </div>
        )}

        <button
          onClick={logout}
          className="flex items-center gap-2 px-3 py-1.5 text-sm font-medium text-slate-300 hover:text-rose-400 bg-slate-800 hover:bg-rose-950/40 border border-slate-700 hover:border-rose-900/50 rounded-lg transition-colors cursor-pointer"
          title="Cerrar sesión (logoutRedirect)"
        >
          <LogOut className="w-4 h-4" />
          <span>Cerrar Sesión</span>
        </button>
      </div>
    </header>
  );
};
