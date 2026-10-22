import React, { useState } from 'react';
import {
  amqpService,
  extractAmqpError,
  type CreateQueueRequest,
  type QueueStatsResponse,
  type AmqpOperationResponse,
} from '../services/amqp.service';
import {
  Network,
  Search,
  PlusCircle,
  Trash2,
  RefreshCw,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  ShieldAlert,
  Info,
  Layers,
  Inbox,
  Users,
  Loader2,
  Flame,
  Sliders,
} from 'lucide-react';

// Colas protegidas de la topología esencial de VidaSalud
const PROTECTED_QUEUES = [
  { name: 'citas.agendamiento.queue', type: 'Principal', rk: 'citas.crear' },
  { name: 'citas.confirmacion.queue', type: 'Principal', rk: 'citas.confirmar' },
  { name: 'notificaciones.auditoria.queue', type: 'Principal', rk: 'vidasalud.notificacion.*' },
  { name: 'citas.agendamiento.dlq', type: 'Dead Letter', rk: 'citas.agendamiento.dlq.rk' },
  { name: 'citas.confirmacion.dlq', type: 'Dead Letter', rk: 'citas.confirmacion.dlq.rk' },
  { name: 'notificaciones.auditoria.dlq', type: 'Dead Letter', rk: 'notificaciones.auditoria.dlq.rk' },
];

export const RabbitMQAdminPage: React.FC = () => {
  // ─── Query Queue State ──────────────────────────────────────────────────────
  const [queryName, setQueryName] = useState<string>('');
  const [isQuerying, setIsQuerying] = useState<boolean>(false);
  const [queriedStats, setQueriedStats] = useState<QueueStatsResponse | null>(null);

  // ─── Create Queue State ─────────────────────────────────────────────────────
  const [createForm, setCreateForm] = useState<CreateQueueRequest>({
    name: '',
    durable: true,
    exclusive: false,
    autoDelete: false,
    deadLetterExchange: '',
    deadLetterRoutingKey: '',
    messageTtl: undefined,
    maxLength: undefined,
  });
  const [showAdvanced, setShowAdvanced] = useState<boolean>(false);
  const [isCreating, setIsCreating] = useState<boolean>(false);

  // ─── Delete Queue State ─────────────────────────────────────────────────────
  const [deleteName, setDeleteName] = useState<string>('');
  const [isDeleting, setIsDeleting] = useState<boolean>(false);

  // ─── Purge Queue State ──────────────────────────────────────────────────────
  const [isPurging, setIsPurging] = useState<boolean>(false);

  // ─── Confirmation Modal State ───────────────────────────────────────────────
  const [confirmModal, setConfirmModal] = useState<{
    isOpen: boolean;
    action: 'delete' | 'purge';
    queueName: string;
  }>({
    isOpen: false,
    action: 'delete',
    queueName: '',
  });

  // ─── Global Notifications / Feedback State ──────────────────────────────────
  const [lastOperation, setLastOperation] = useState<{
    type: 'success' | 'error';
    title: string;
    message: string;
    errorCode?: string;
    status?: number;
    timestamp?: string;
    data?: Record<string, unknown> | null;
  } | null>(null);

  // Helper para clasificar errores conocidos
  const getErrorCodeDescription = (code?: string): { title: string; hint: string } | null => {
    switch (code) {
      case 'AMQP_EMPTY_NAME':
        return {
          title: 'Nombre Inválido (Vacío)',
          hint: 'El nombre de la cola no puede estar vacío ni contener solo espacios en blanco.',
        };
      case 'AMQP_RESERVED_PREFIX':
        return {
          title: 'Prefijo Reservado por RabbitMQ',
          hint: "Los nombres que inician con 'amq.' están reservados para el uso interno del broker RabbitMQ.",
        };
      case 'AMQP_PROTECTED_RESOURCE':
        return {
          title: 'Recurso Protegido del Sistema',
          hint: 'Esta cola o DLQ pertenece a la topología esencial de VidaSalud y no puede ser eliminada ni alterada.',
        };
      case 'AMQP_INVALID_CHARACTERS':
        return {
          title: 'Caracteres no Permitidos',
          hint: 'El nombre solo puede contener letras, números, puntos (.), guiones (-) y guiones bajos (_).',
        };
      case 'AMQP_BROKER_REJECTED':
        return {
          title: 'Rechazo del Broker',
          hint: 'RabbitMQ rechazó la declaración del recurso por violar directivas internas de configuración.',
        };
      default:
        return null;
    }
  };

  // ─── 1. Consultar Estadísticas de Cola ───────────────────────────────────────
  const handleQueryQueue = async (targetQueueName?: string) => {
    const queueToQuery = (targetQueueName ?? queryName).trim();
    if (!queueToQuery) {
      setLastOperation({
        type: 'error',
        title: 'Nombre Requerido',
        message: 'Por favor ingresa o selecciona el nombre de la cola a consultar.',
        errorCode: 'AMQP_EMPTY_NAME',
        status: 400,
      });
      return;
    }

    setIsQuerying(true);
    setLastOperation(null);

    try {
      const stats = await amqpService.getQueueStats(queueToQuery);
      setQueriedStats(stats);
      setQueryName(queueToQuery);
      setLastOperation({
        type: 'success',
        title: 'Consulta Exitosa',
        message: `Estadísticas obtenidas para la cola '${stats.name}'.`,
        timestamp: new Date().toISOString(),
        data: {
          exists: stats.exists,
          messageCount: stats.messageCount,
          consumerCount: stats.consumerCount,
          apiSource: stats.apiSource,
        },
      });
    } catch (err: unknown) {
      setQueriedStats(null);
      const parsed = extractAmqpError(err);
      setLastOperation({
        type: 'error',
        title: parsed.status === 404 ? 'Cola no Encontrada' : 'Error en Consulta AMQP',
        message: parsed.message,
        errorCode: parsed.errorCode,
        status: parsed.status,
        timestamp: parsed.timestamp,
      });
    } finally {
      setIsQuerying(false);
    }
  };

  // ─── 2. Crear Cola ──────────────────────────────────────────────────────────
  const handleCreateQueue = async (e: React.FormEvent) => {
    e.preventDefault();

    const trimmedName = createForm.name.trim();

    // Validaciones en cliente
    if (!trimmedName) {
      setLastOperation({
        type: 'error',
        title: 'Validación Local Fallida',
        message: 'El nombre de la cola es obligatorio.',
        errorCode: 'AMQP_EMPTY_NAME',
        status: 400,
      });
      return;
    }

    if (!/^[a-zA-Z0-9._-]+$/.test(trimmedName)) {
      setLastOperation({
        type: 'error',
        title: 'Validación Local Fallida',
        message: 'El nombre contiene caracteres no permitidos. Usa solo letras, números, puntos, guiones y guiones bajos.',
        errorCode: 'AMQP_INVALID_CHARACTERS',
        status: 400,
      });
      return;
    }

    if (trimmedName.toLowerCase().startsWith('amq.')) {
      setLastOperation({
        type: 'error',
        title: 'Validación Local Fallida',
        message: "El nombre no puede comenzar con el prefijo reservado 'amq.'.",
        errorCode: 'AMQP_RESERVED_PREFIX',
        status: 400,
      });
      return;
    }

    if (createForm.messageTtl !== undefined && createForm.messageTtl <= 0) {
      setLastOperation({
        type: 'error',
        title: 'Validación Local Fallida',
        message: 'messageTtl debe ser un número entero mayor a 0 (milisegundos).',
        status: 400,
      });
      return;
    }

    if (createForm.maxLength !== undefined && createForm.maxLength <= 0) {
      setLastOperation({
        type: 'error',
        title: 'Validación Local Fallida',
        message: 'maxLength debe ser un número entero mayor a 0.',
        status: 400,
      });
      return;
    }

    setIsCreating(true);
    setLastOperation(null);

    const payload: CreateQueueRequest = {
      name: trimmedName,
      durable: createForm.durable,
      exclusive: createForm.exclusive,
      autoDelete: createForm.autoDelete,
      deadLetterExchange: createForm.deadLetterExchange?.trim() || undefined,
      deadLetterRoutingKey: createForm.deadLetterRoutingKey?.trim() || undefined,
      messageTtl: createForm.messageTtl ? Number(createForm.messageTtl) : undefined,
      maxLength: createForm.maxLength ? Number(createForm.maxLength) : undefined,
    };

    try {
      const response: AmqpOperationResponse = await amqpService.createQueue(payload);
      setLastOperation({
        type: 'success',
        title: 'Cola Declarada con Éxito',
        message: response.message || `Cola '${trimmedName}' creada en RabbitMQ.`,
        timestamp: response.timestamp,
        data: response.data,
      });

      // Limpiar formulario y auto-consultar la cola creada
      setCreateForm({
        name: '',
        durable: true,
        exclusive: false,
        autoDelete: false,
        deadLetterExchange: '',
        deadLetterRoutingKey: '',
        messageTtl: undefined,
        maxLength: undefined,
      });
      setQueryName(trimmedName);
      handleQueryQueue(trimmedName);
    } catch (err: unknown) {
      const parsed = extractAmqpError(err);
      setLastOperation({
        type: 'error',
        title: 'Error al Crear Cola en BFF',
        message: parsed.message,
        errorCode: parsed.errorCode,
        status: parsed.status,
        timestamp: parsed.timestamp,
      });
    } finally {
      setIsCreating(false);
    }
  };

  // ─── 3. Solicitar Confirmación de Eliminación o Purga ────────────────────────
  const requestDelete = (name: string) => {
    const trimmed = name.trim();
    if (!trimmed) {
      setLastOperation({
        type: 'error',
        title: 'Nombre Requerido',
        message: 'Ingresa el nombre de la cola que deseas eliminar.',
        errorCode: 'AMQP_EMPTY_NAME',
        status: 400,
      });
      return;
    }
    setConfirmModal({
      isOpen: true,
      action: 'delete',
      queueName: trimmed,
    });
  };

  const requestPurge = (name: string) => {
    const trimmed = name.trim();
    if (!trimmed) return;
    setConfirmModal({
      isOpen: true,
      action: 'purge',
      queueName: trimmed,
    });
  };

  // ─── 4. Ejecutar Eliminación (DELETE /api/admin/amqp/queues/{name}) ──────────
  const executeDelete = async (name: string) => {
    setConfirmModal({ isOpen: false, action: 'delete', queueName: '' });
    setIsDeleting(true);
    setLastOperation(null);

    try {
      const res = await amqpService.deleteQueue(name);
      setLastOperation({
        type: 'success',
        title: 'Cola Eliminada',
        message: res.message || `La cola '${name}' ha sido eliminada exitosamente del broker.`,
        timestamp: res.timestamp,
        data: res.data,
      });

      if (queriedStats?.name === name) {
        setQueriedStats(null);
      }
      if (deleteName === name) {
        setDeleteName('');
      }
    } catch (err: unknown) {
      const parsed = extractAmqpError(err);
      setLastOperation({
        type: 'error',
        title: 'Fallo al Eliminar Cola',
        message: parsed.message,
        errorCode: parsed.errorCode,
        status: parsed.status,
        timestamp: parsed.timestamp,
      });
    } finally {
      setIsDeleting(false);
    }
  };

  // ─── 5. Ejecutar Purga (POST /api/admin/amqp/queues/{name}/purge) ────────────
  const executePurge = async (name: string) => {
    setConfirmModal({ isOpen: false, action: 'purge', queueName: '' });
    setIsPurging(true);
    setLastOperation(null);

    try {
      const res = await amqpService.purgeQueue(name);
      setLastOperation({
        type: 'success',
        title: 'Cola Purgada',
        message: res.message || `Mensajes purgados exitosamente de '${name}'.`,
        timestamp: res.timestamp,
        data: res.data,
      });
      // Actualizar estadísticas si es la cola actual
      if (queriedStats?.name === name) {
        handleQueryQueue(name);
      }
    } catch (err: unknown) {
      const parsed = extractAmqpError(err);
      setLastOperation({
        type: 'error',
        title: 'Fallo al Purgar Cola',
        message: parsed.message,
        errorCode: parsed.errorCode,
        status: parsed.status,
        timestamp: parsed.timestamp,
      });
    } finally {
      setIsPurging(false);
    }
  };

  const isAnyActionInProgress = isQuerying || isCreating || isDeleting || isPurging;

  return (
    <div className="space-y-6">
      {/* ─── Encabezado Principal ─── */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-white flex items-center gap-2">
            <Network className="w-7 h-7 text-emerald-400" /> Administración de RabbitMQ
          </h2>
          <p className="text-slate-400 text-sm">
            Gestión administrativa de colas, inspección de métricas y validación de topología sobre el broker AMQP.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="px-3 py-1 bg-purple-500/10 text-purple-400 border border-purple-500/20 text-xs font-semibold rounded-full flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-purple-400 animate-pulse"></span>
            Rol: Admin
          </span>
          <span className="px-3 py-1 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-xs font-semibold rounded-full">
            AMQP 0-9-1
          </span>
        </div>
      </div>

      {/* ─── Panel Informativo de Alcance de la API y Endpoints ─── */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 space-y-4">
        <div className="flex items-start gap-3">
          <div className="p-2 bg-blue-500/10 text-blue-400 rounded-lg shrink-0 border border-blue-500/20">
            <Info className="w-5 h-5" />
          </div>
          <div className="space-y-1">
            <h3 className="text-sm font-bold text-white">
              Arquitectura de la API Administrativa BFF (Spring AMQP)
            </h3>
            <p className="text-xs text-slate-300 leading-relaxed">
              El BFF expone operaciones directas sobre RabbitMQ mediante la interfaz oficial <code className="text-emerald-400 font-mono">RabbitAdmin</code>.
              Las rutas disponibles son puntuales por cola (<code className="text-slate-200 font-mono">POST /queues</code>, <code className="text-slate-200 font-mono">GET /queues/{'{name}'}</code>, <code className="text-slate-200 font-mono">DELETE /queues/{'{name}'}</code>, <code className="text-slate-200 font-mono">POST /queues/{'{name}'}/purge</code>).
            </p>
            <div className="pt-2 text-[11px] text-amber-300/90 flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 shrink-0 text-amber-400" />
              <span>
                <strong>Nota técnica sobre listados globales:</strong> El protocolo AMQP 0-9-1 no dispone de un comando nativo para listar todas las colas, exchanges o DLQs (<code className="font-mono">GET /api/admin/amqp/queues</code> no existe en el backend). Para listar la totalidad del cluster y ver consumo de memoria se utiliza el plugin RabbitMQ Management HTTP API (<code className="font-mono text-slate-200">http://localhost:15672</code>).
              </span>
            </div>
          </div>
        </div>

        {/* Colas de Topología Esencial del Sistema */}
        <div className="pt-3 border-t border-slate-800/80">
          <p className="text-xs font-semibold text-slate-300 mb-2 flex items-center gap-2">
            <Layers className="w-4 h-4 text-emerald-400" />
            Colas Protegidas de VidaSalud (Haz clic para consultar métricas o probar protección):
          </p>
          <div className="flex flex-wrap gap-2">
            {PROTECTED_QUEUES.map((q) => (
              <button
                key={q.name}
                type="button"
                onClick={() => handleQueryQueue(q.name)}
                disabled={isAnyActionInProgress}
                className="group px-2.5 py-1.5 bg-slate-800/70 hover:bg-slate-800 text-xs text-slate-300 hover:text-white rounded-lg border border-slate-700/60 hover:border-emerald-500/40 transition flex items-center gap-1.5 disabled:opacity-50"
              >
                <span
                  className={`w-1.5 h-1.5 rounded-full ${
                    q.type === 'Dead Letter' ? 'bg-amber-400' : 'bg-emerald-400'
                  }`}
                ></span>
                <span className="font-mono">{q.name}</span>
                <span className="text-[10px] text-slate-500 group-hover:text-slate-400">({q.type})</span>
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* ─── Banner de Feedback / Resultado de Operación ─── */}
      {lastOperation && (
        <div
          className={`rounded-xl p-4 border transition-all ${
            lastOperation.type === 'success'
              ? 'bg-emerald-950/40 border-emerald-500/40 text-emerald-200'
              : 'bg-rose-950/40 border-rose-500/40 text-rose-200'
          }`}
        >
          <div className="flex items-start gap-3">
            {lastOperation.type === 'success' ? (
              <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
            ) : (
              <XCircle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
            )}
            <div className="flex-1 space-y-1">
              <div className="flex flex-wrap items-center gap-2">
                <h4 className="font-bold text-sm text-white">{lastOperation.title}</h4>
                {lastOperation.status && (
                  <span
                    className={`text-[11px] font-mono px-2 py-0.5 rounded border ${
                      lastOperation.type === 'success'
                        ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30'
                        : 'bg-rose-500/20 text-rose-300 border-rose-500/30'
                    }`}
                  >
                    HTTP {lastOperation.status}
                  </span>
                )}
                {lastOperation.errorCode && (
                  <span className="text-[11px] font-mono px-2 py-0.5 bg-amber-500/20 text-amber-300 border border-amber-500/30 rounded font-bold">
                    Código: {lastOperation.errorCode}
                  </span>
                )}
              </div>

              <p className="text-xs text-slate-200 leading-relaxed">{lastOperation.message}</p>

              {/* Explicación amigable para códigos de error específicos del backend */}
              {lastOperation.errorCode && getErrorCodeDescription(lastOperation.errorCode) && (
                <div className="mt-2 p-2.5 bg-slate-900/80 rounded-lg border border-slate-700/60 text-xs text-slate-300 space-y-0.5">
                  <p className="font-semibold text-amber-300">
                    {getErrorCodeDescription(lastOperation.errorCode)?.title}
                  </p>
                  <p className="text-slate-400">
                    {getErrorCodeDescription(lastOperation.errorCode)?.hint}
                  </p>
                </div>
              )}

              {lastOperation.data && (
                <div className="mt-2 text-[11px] font-mono bg-slate-950/80 p-2 rounded border border-slate-800 text-slate-300 overflow-x-auto">
                  <pre>{JSON.stringify(lastOperation.data, null, 2)}</pre>
                </div>
              )}

              {lastOperation.timestamp && (
                <p className="text-[10px] text-slate-400 pt-1">
                  Registrado: {new Date(lastOperation.timestamp).toLocaleString()}
                </p>
              )}
            </div>
            <button
              type="button"
              onClick={() => setLastOperation(null)}
              className="text-slate-400 hover:text-white text-xs px-2 py-1"
            >
              Cerrar
            </button>
          </div>
        </div>
      )}

      {/* ─── Grid de Acciones Principales ─── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* ─── Tarjeta 1: Consultar Cola por Nombre ─── */}
        <div className="bg-slate-900/70 border border-slate-800 rounded-xl p-6 space-y-5">
          <div className="flex items-center justify-between pb-3 border-b border-slate-800">
            <div className="flex items-center gap-2">
              <Search className="w-5 h-5 text-blue-400" />
              <h3 className="font-bold text-white text-lg">Consultar Cola</h3>
            </div>
            <span className="text-[11px] font-mono px-2 py-0.5 bg-blue-500/10 text-blue-300 border border-blue-500/20 rounded">
              GET /queues/{'{name}'}
            </span>
          </div>

          <p className="text-xs text-slate-400">
            Verifica pasivamente si una cola existe en el broker y obtiene la cantidad exacta de mensajes encolados y consumidores conectados.
          </p>

          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleQueryQueue();
            }}
            className="flex gap-2"
          >
            <div className="relative flex-1">
              <input
                type="text"
                value={queryName}
                onChange={(e) => setQueryName(e.target.value)}
                placeholder="Nombre de la cola (ej: citas.agendamiento.queue)"
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 font-mono"
                disabled={isAnyActionInProgress}
              />
            </div>
            <button
              type="submit"
              disabled={isAnyActionInProgress || !queryName.trim()}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-500 disabled:bg-slate-800 disabled:text-slate-500 text-white text-sm font-medium rounded-lg transition flex items-center gap-2"
            >
              {isQuerying ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" /> Consultando...
                </>
              ) : (
                <>
                  <Search className="w-4 h-4" /> Consultar
                </>
              )}
            </button>
          </form>

          {/* Resultado de la Consulta */}
          {queriedStats && (
            <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-4 space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <span className="text-[10px] uppercase font-mono text-slate-400">Cola Consultada</span>
                  <h4 className="text-base font-bold text-white font-mono flex items-center gap-2">
                    {queriedStats.name}
                    {queriedStats.exists && (
                      <span className="text-[10px] font-sans px-2 py-0.5 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 rounded-full font-semibold">
                        Existe en Broker
                      </span>
                    )}
                  </h4>
                </div>
                <button
                  type="button"
                  onClick={() => handleQueryQueue(queriedStats.name)}
                  disabled={isAnyActionInProgress}
                  className="p-1.5 text-slate-400 hover:text-white bg-slate-800/60 hover:bg-slate-800 rounded-lg transition"
                  title="Refrescar métricas"
                >
                  <RefreshCw className={`w-4 h-4 ${isQuerying ? 'animate-spin' : ''}`} />
                </button>
              </div>

              {/* Métricas Principales */}
              <div className="grid grid-cols-2 gap-3">
                <div className="bg-slate-900 border border-slate-800 rounded-lg p-3">
                  <div className="flex items-center justify-between text-slate-400 mb-1">
                    <span className="text-xs">Mensajes en Cola</span>
                    <Inbox className="w-4 h-4 text-emerald-400" />
                  </div>
                  <p className="text-2xl font-extrabold text-white font-mono">
                    {queriedStats.messageCount}
                  </p>
                  <span className="text-[10px] text-slate-500">Pendientes de entrega</span>
                </div>

                <div className="bg-slate-900 border border-slate-800 rounded-lg p-3">
                  <div className="flex items-center justify-between text-slate-400 mb-1">
                    <span className="text-xs">Consumidores</span>
                    <Users className="w-4 h-4 text-blue-400" />
                  </div>
                  <p className="text-2xl font-extrabold text-white font-mono">
                    {queriedStats.consumerCount}
                  </p>
                  <span className="text-[10px] text-slate-500">Listeners activos</span>
                </div>
              </div>

              {/* Metadatos y Origen de API */}
              <div className="text-[11px] text-slate-400 bg-slate-900/60 p-2.5 rounded border border-slate-800 space-y-1">
                <p>
                  <strong className="text-slate-300">Fuente:</strong> {queriedStats.apiSource}
                </p>
                <p className="text-[10px] text-slate-500 leading-normal">{queriedStats.details}</p>
              </div>

              {/* Botones de Acción sobre la Cola Consultada */}
              <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => requestPurge(queriedStats.name)}
                  disabled={isAnyActionInProgress || queriedStats.messageCount === 0}
                  className="px-3 py-1.5 bg-amber-600/20 hover:bg-amber-600/30 text-amber-300 border border-amber-500/30 rounded-lg text-xs font-medium transition flex items-center gap-1.5 disabled:opacity-40"
                  title="Eliminar todos los mensajes contenidos en la cola"
                >
                  <Flame className="w-3.5 h-3.5 text-amber-400" />
                  Purgar Mensajes
                </button>
                <button
                  type="button"
                  onClick={() => requestDelete(queriedStats.name)}
                  disabled={isAnyActionInProgress}
                  className="px-3 py-1.5 bg-rose-600/20 hover:bg-rose-600/30 text-rose-300 border border-rose-500/30 rounded-lg text-xs font-medium transition flex items-center gap-1.5 disabled:opacity-40"
                  title="Eliminar cola definitivamente"
                >
                  <Trash2 className="w-3.5 h-3.5 text-rose-400" />
                  Eliminar Cola
                </button>
              </div>
            </div>
          )}
        </div>

        {/* ─── Tarjeta 2: Eliminar Cola Directamente ─── */}
        <div className="bg-slate-900/70 border border-slate-800 rounded-xl p-6 space-y-5 flex flex-col justify-between">
          <div className="space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center gap-2">
                <Trash2 className="w-5 h-5 text-rose-400" />
                <h3 className="font-bold text-white text-lg">Eliminar Cola</h3>
              </div>
              <span className="text-[11px] font-mono px-2 py-0.5 bg-rose-500/10 text-rose-300 border border-rose-500/20 rounded">
                DELETE /queues/{'{name}'}
              </span>
            </div>

            <p className="text-xs text-slate-400 leading-relaxed">
              Elimina una cola no protegida del broker RabbitMQ. Toda petición requiere confirmación explícita.
            </p>

            <div className="p-3 bg-amber-500/10 border border-amber-500/20 rounded-lg text-xs text-amber-200/90 space-y-1">
              <p className="font-semibold flex items-center gap-1.5">
                <ShieldAlert className="w-4 h-4 text-amber-400 shrink-0" />
                Manejo de Errores y Seguridad:
              </p>
              <ul className="list-disc list-inside text-[11px] text-amber-300/80 space-y-0.5 pl-1">
                <li>
                  Las 6 colas de la topología esencial retornarán <code className="font-mono">403 AMQP_PROTECTED_RESOURCE</code>.
                </li>
                <li>
                  Colas con prefijo <code className="font-mono">amq.</code> retornarán <code className="font-mono">400 AMQP_RESERVED_PREFIX</code>.
                </li>
                <li>
                  Colas no existentes retornarán <code className="font-mono">404 Not Found</code>.
                </li>
              </ul>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-medium text-slate-300">Nombre de la cola a eliminar</label>
              <input
                type="text"
                value={deleteName}
                onChange={(e) => setDeleteName(e.target.value)}
                placeholder="ej: cola.temporal.descartable"
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-rose-500 font-mono"
                disabled={isAnyActionInProgress}
              />
            </div>
          </div>

          <div className="pt-4 border-t border-slate-800">
            <button
              type="button"
              onClick={() => requestDelete(deleteName)}
              disabled={isAnyActionInProgress || !deleteName.trim()}
              className="w-full py-2.5 bg-rose-600 hover:bg-rose-500 disabled:bg-slate-800 disabled:text-slate-500 text-white text-sm font-semibold rounded-lg transition flex items-center justify-center gap-2"
            >
              {isDeleting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" /> Procesando eliminación...
                </>
              ) : (
                <>
                  <Trash2 className="w-4 h-4" /> Solicitar Eliminación de Cola
                </>
              )}
            </button>
          </div>
        </div>
      </div>

      {/* ─── Tarjeta 3: Formulario para Crear Nueva Cola ─── */}
      <div className="bg-slate-900/70 border border-slate-800 rounded-xl p-6 space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-slate-800 gap-2">
          <div className="flex items-center gap-2">
            <PlusCircle className="w-5 h-5 text-emerald-400" />
            <h3 className="font-bold text-white text-lg">Crear Nueva Cola en RabbitMQ</h3>
          </div>
          <span className="text-[11px] font-mono px-2 py-0.5 bg-emerald-500/10 text-emerald-300 border border-emerald-500/20 rounded">
            POST /api/admin/amqp/queues
          </span>
        </div>

        <p className="text-xs text-slate-400">
          Declara una nueva cola en el broker respetando las restricciones del backend (<code className="font-mono text-slate-300">CreateQueueRequest</code>). Soporta durabilidad, auto-delete, exclusividad y enrutamiento hacia Dead Letter Exchange (DLQ).
        </p>

        <form onSubmit={handleCreateQueue} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Nombre de la Cola */}
            <div className="space-y-1.5 md:col-span-2">
              <label className="text-xs font-semibold text-slate-200 flex items-center justify-between">
                <span>
                  Nombre de la Cola <span className="text-rose-400">*</span>
                </span>
                <span className="text-[11px] text-slate-400 font-mono font-normal">
                  Regex: ^[a-zA-Z0-9._-]+$
                </span>
              </label>
              <input
                type="text"
                required
                value={createForm.name}
                onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })}
                placeholder="ej: citas.recordatorios.queue"
                className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500 font-mono"
                disabled={isAnyActionInProgress}
              />
              <p className="text-[11px] text-slate-400">
                No debe comenzar con <code className="text-amber-400">amq.</code> ni coincidir con las colas protegidas del sistema.
              </p>
            </div>

            {/* Opciones booleanas */}
            <div className="bg-slate-950/60 border border-slate-800 rounded-lg p-3 space-y-2.5 md:col-span-2">
              <span className="text-xs font-semibold text-slate-300 block">Propiedades de Ciclo de Vida:</span>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <label className="flex items-center gap-2 text-xs text-slate-200 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={createForm.durable}
                    onChange={(e) => setCreateForm({ ...createForm, durable: e.target.checked })}
                    disabled={isAnyActionInProgress}
                    className="w-4 h-4 rounded bg-slate-900 border-slate-700 text-emerald-500 focus:ring-emerald-500"
                  />
                  <span>
                    <strong>Durable</strong> (recomendado)
                  </span>
                </label>

                <label className="flex items-center gap-2 text-xs text-slate-200 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={createForm.exclusive}
                    onChange={(e) => setCreateForm({ ...createForm, exclusive: e.target.checked })}
                    disabled={isAnyActionInProgress}
                    className="w-4 h-4 rounded bg-slate-900 border-slate-700 text-emerald-500 focus:ring-emerald-500"
                  />
                  <span>Exclusiva</span>
                </label>

                <label className="flex items-center gap-2 text-xs text-slate-200 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={createForm.autoDelete}
                    onChange={(e) => setCreateForm({ ...createForm, autoDelete: e.target.checked })}
                    disabled={isAnyActionInProgress}
                    className="w-4 h-4 rounded bg-slate-900 border-slate-700 text-emerald-500 focus:ring-emerald-500"
                  />
                  <span>Auto-Delete</span>
                </label>
              </div>
            </div>
          </div>

          {/* Opciones Avanzadas: DLQ y TTL */}
          <div className="pt-2">
            <button
              type="button"
              onClick={() => setShowAdvanced(!showAdvanced)}
              className="text-xs text-emerald-400 hover:text-emerald-300 flex items-center gap-1.5 font-medium transition"
            >
              <Sliders className="w-3.5 h-3.5" />
              {showAdvanced ? 'Ocultar Parámetros DLQ y TTL' : 'Configurar Dead Letter Queue (DLQ) y TTL (Opcional)'}
            </button>

            {showAdvanced && (
              <div className="mt-3 p-4 bg-slate-950/80 border border-slate-800 rounded-lg space-y-4">
                <p className="text-[11px] text-slate-400">
                  Permite redirigir mensajes fallidos (NACK) hacia un Dead Letter Exchange específico o limitar el tiempo de vida en milisegundos.
                </p>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1">
                    <label className="text-xs text-slate-300 font-medium">Dead Letter Exchange (DLX)</label>
                    <input
                      type="text"
                      value={createForm.deadLetterExchange || ''}
                      onChange={(e) => setCreateForm({ ...createForm, deadLetterExchange: e.target.value })}
                      placeholder="ej: vidasalud.dlx.exchange"
                      className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white placeholder-slate-500 font-mono"
                      disabled={isAnyActionInProgress}
                    />
                  </div>

                  <div className="space-y-1">
                    <label className="text-xs text-slate-300 font-medium">Dead Letter Routing Key</label>
                    <input
                      type="text"
                      value={createForm.deadLetterRoutingKey || ''}
                      onChange={(e) => setCreateForm({ ...createForm, deadLetterRoutingKey: e.target.value })}
                      placeholder="ej: recordatorios.dlq.rk"
                      className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white placeholder-slate-500 font-mono"
                      disabled={isAnyActionInProgress}
                    />
                  </div>

                  <div className="space-y-1">
                    <label className="text-xs text-slate-300 font-medium">Message TTL (ms)</label>
                    <input
                      type="number"
                      min="1"
                      value={createForm.messageTtl ?? ''}
                      onChange={(e) =>
                        setCreateForm({
                          ...createForm,
                          messageTtl: e.target.value ? Number(e.target.value) : undefined,
                        })
                      }
                      placeholder="ej: 60000 (1 minuto)"
                      className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white placeholder-slate-500 font-mono"
                      disabled={isAnyActionInProgress}
                    />
                  </div>

                  <div className="space-y-1">
                    <label className="text-xs text-slate-300 font-medium">Max Length (cantidad de mensajes)</label>
                    <input
                      type="number"
                      min="1"
                      value={createForm.maxLength ?? ''}
                      onChange={(e) =>
                        setCreateForm({
                          ...createForm,
                          maxLength: e.target.value ? Number(e.target.value) : undefined,
                        })
                      }
                      placeholder="ej: 1000"
                      className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white placeholder-slate-500 font-mono"
                      disabled={isAnyActionInProgress}
                    />
                  </div>
                </div>
              </div>
            )}
          </div>

          <div className="pt-3 border-t border-slate-800 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={() =>
                setCreateForm({
                  name: '',
                  durable: true,
                  exclusive: false,
                  autoDelete: false,
                  deadLetterExchange: '',
                  deadLetterRoutingKey: '',
                  messageTtl: undefined,
                  maxLength: undefined,
                })
              }
              disabled={isAnyActionInProgress}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 text-sm rounded-lg transition"
            >
              Limpiar Campos
            </button>
            <button
              type="submit"
              disabled={isAnyActionInProgress || !createForm.name.trim()}
              className="px-5 py-2 bg-emerald-600 hover:bg-emerald-500 disabled:bg-slate-800 disabled:text-slate-500 text-white text-sm font-semibold rounded-lg transition flex items-center gap-2"
            >
              {isCreating ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" /> Declarando Cola en RabbitMQ...
                </>
              ) : (
                <>
                  <PlusCircle className="w-4 h-4" /> Declarar Cola
                </>
              )}
            </button>
          </div>
        </form>
      </div>

      {/* ─── Modal de Confirmación para Acciones Destructivas ─── */}
      {confirmModal.isOpen && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-md w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center gap-3">
              <div
                className={`p-3 rounded-full border ${
                  confirmModal.action === 'delete'
                    ? 'bg-rose-500/10 border-rose-500/30 text-rose-400'
                    : 'bg-amber-500/10 border-amber-500/30 text-amber-400'
                }`}
              >
                <AlertTriangle className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-white">
                  {confirmModal.action === 'delete' ? '¿Confirmar Eliminación?' : '¿Confirmar Purga de Mensajes?'}
                </h3>
                <p className="text-xs text-slate-400">Esta operación se ejecuta directamente en RabbitMQ.</p>
              </div>
            </div>

            <div className="p-3 bg-slate-950 rounded-lg border border-slate-800 text-xs text-slate-300 space-y-1">
              <p>
                <strong>Recurso:</strong> <span className="font-mono text-emerald-400">{confirmModal.queueName}</span>
              </p>
              <p className="text-slate-400">
                {confirmModal.action === 'delete'
                  ? 'La cola se borrará por completo del broker. Si contiene mensajes acumulados, se perderán irremediablemente.'
                  : 'Todos los mensajes actualmente encolados serán purgados inmediatamente a cero.'}
              </p>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setConfirmModal({ isOpen: false, action: 'delete', queueName: '' })}
                disabled={isAnyActionInProgress}
                className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-medium rounded-lg transition"
              >
                Cancelar
              </button>

              {confirmModal.action === 'delete' ? (
                <button
                  type="button"
                  onClick={() => executeDelete(confirmModal.queueName)}
                  disabled={isAnyActionInProgress}
                  className="px-4 py-2 bg-rose-600 hover:bg-rose-500 text-white text-sm font-semibold rounded-lg transition flex items-center gap-1.5"
                >
                  <Trash2 className="w-4 h-4" /> Sí, Eliminar Definitivamente
                </button>
              ) : (
                <button
                  type="button"
                  onClick={() => executePurge(confirmModal.queueName)}
                  disabled={isAnyActionInProgress}
                  className="px-4 py-2 bg-amber-600 hover:bg-amber-500 text-white text-sm font-semibold rounded-lg transition flex items-center gap-1.5"
                >
                  <Flame className="w-4 h-4" /> Sí, Purgar Mensajes
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
