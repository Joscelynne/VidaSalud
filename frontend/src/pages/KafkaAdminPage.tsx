import React, { useState, useEffect, useCallback } from 'react';
import { kafkaService } from '../services/kafka.service';
import type {
  KafkaTopicSummary,
  KafkaConsumerGroup,
  CreateKafkaTopicRequest,
} from '../types/kafka.types';
import {
  Layers,
  Radio,
  ExternalLink,
  RefreshCw,
  PlusCircle,
  Trash2,
  Send,
  AlertTriangle,
  CheckCircle2,
  Users,
  Activity,
  Server,
  ShieldCheck,
} from 'lucide-react';

export const KafkaAdminPage: React.FC = () => {
  const [topics, setTopics] = useState<KafkaTopicSummary[]>([]);
  const [consumerGroups, setConsumerGroups] = useState<KafkaConsumerGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Form states
  const [newTopicName, setNewTopicName] = useState('');
  const [newTopicPartitions, setNewTopicPartitions] = useState(3);
  const [newTopicReplicas, setNewTopicReplicas] = useState(3);
  const [newTopicRetention, setNewTopicRetention] = useState(604800000);

  // Test event states
  const [simEventType, setSimEventType] = useState<'ATENCIONES' | 'TELEMETRIA'>('ATENCIONES');
  const [simSpecialty, setSimSpecialty] = useState('Cardiología');
  const [simLatency, setSimLatency] = useState(150);
  const [offsetResetStrategy, setOffsetResetStrategy] = useState<'EARLIEST' | 'LATEST'>('EARLIEST');

  const loadData = useCallback(async () => {
    try {
      const [topicsData, groupsData] = await Promise.all([
        kafkaService.listTopics(),
        kafkaService.listConsumerGroups(),
      ]);
      setTopics(topicsData);
      setConsumerGroups(groupsData);
      setError(null);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Error al consultar clúster Kafka';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchData = async () => {
    setLoading(true);
    await loadData();
    setLoading(false);
  };

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const handleCreateTopic = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTopicName.trim()) return;

    setError(null);
    setSuccessMsg(null);
    try {
      const req: CreateKafkaTopicRequest = {
        name: newTopicName.trim(),
        partitions: newTopicPartitions,
        replicationFactor: newTopicReplicas,
        retentionMs: newTopicRetention,
        cleanupPolicy: 'delete',
      };
      const res = await kafkaService.createTopic(req);
      setSuccessMsg(res.message);
      setNewTopicName('');
      fetchData();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Error al crear tópico';
      setError(message);
    }
  };

  const handleDeleteTopic = async (topicName: string) => {
    if (!window.confirm(`¿Seguro que deseas eliminar el tópico '${topicName}'?`)) return;

    setError(null);
    setSuccessMsg(null);
    try {
      const res = await kafkaService.deleteTopic(topicName);
      setSuccessMsg(res.message);
      fetchData();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Error al eliminar tópico';
      setError(message);
    }
  };

  const handleResetGroupOffsets = async (groupId: string) => {
    const target = offsetResetStrategy === 'EARLIEST' ? 'el inicio (se reprocesarán mensajes)' : 'el final (se saltarán mensajes pendientes)';
    const confirmed = window.confirm(
      `Vas a mover los offsets de '${groupId}' a ${target}. El grupo debe estar detenido. ¿Confirmas esta acción?`
    );
    if (!confirmed) return;

    setError(null);
    setSuccessMsg(null);
    try {
      const response = await kafkaService.resetConsumerGroupOffsets(groupId, {
        strategy: offsetResetStrategy,
        confirmed: true,
      });
      setSuccessMsg(response.message);
      await fetchData();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'No se pudieron reiniciar los offsets';
      setError(message);
    }
  };

  const handlePublishTestEvent = async () => {
    setError(null);
    setSuccessMsg(null);
    try {
      const res = await kafkaService.publishTestEvent({
        eventType: simEventType,
        specialty: simSpecialty,
        executionTimeMs: simLatency,
        severity: simLatency > 2000 ? 'WARN' : 'INFO',
      });
      setSuccessMsg(res.message);
      fetchData();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Error al publicar evento de prueba';
      setError(message);
    }
  };

  return (
    <div className="space-y-6">
      {/* Encabezado */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-slate-900 border border-slate-800 p-6 rounded-2xl shadow-sm">
        <div>
          <div className="flex items-center gap-3">
            <span className="p-2.5 bg-sky-500/10 text-sky-400 rounded-xl">
              <Radio className="w-6 h-6" />
            </span>
            <h1 className="text-2xl font-bold text-white tracking-tight">
              Streaming & Analítica con Apache Kafka (EP5 / EP6)
            </h1>
          </div>
          <p className="text-sm text-slate-400 mt-1">
            Plataforma de eventos en tiempo real: Ensamble ZooKeeper 3 nodos, Clúster 3 brokers, factor de réplica 3 y particionado distribuido.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <a
            href="http://localhost:8090"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-xl transition shadow-sm"
          >
            <ExternalLink className="w-4 h-4" />
            Abrir Kafka UI (Puerto 8090)
          </a>
          <button
            onClick={fetchData}
            disabled={loading}
            className="flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 text-sm font-medium rounded-xl transition border border-slate-700"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            Refrescar
          </button>
        </div>
      </div>

      {/* Alertas */}
      {error && (
        <div className="flex items-start gap-3 p-4 bg-rose-500/10 border border-rose-500/20 text-rose-300 rounded-xl text-sm">
          <AlertTriangle className="w-5 h-5 flex-shrink-0 mt-0.5" />
          <div>{error}</div>
        </div>
      )}

      {successMsg && (
        <div className="flex items-start gap-3 p-4 bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 rounded-xl text-sm">
          <CheckCircle2 className="w-5 h-5 flex-shrink-0 mt-0.5" />
          <div>{successMsg}</div>
        </div>
      )}

      {/* Tarjetas de Arquitectura EP6 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-slate-900 border border-slate-800 p-5 rounded-2xl">
          <div className="flex items-center gap-3">
            <Server className="w-5 h-5 text-sky-400" />
            <h3 className="font-semibold text-white text-sm">ZooKeeper Quórum (3 Nodos)</h3>
          </div>
          <p className="text-xs text-slate-400 mt-2">
            zookeeper1 (2181), zookeeper2 (2182), zookeeper3 (2183). Tolerancia a fallos: quórum 2f+1 resiste la caída de 1 nodo.
          </p>
        </div>

        <div className="bg-slate-900 border border-slate-800 p-5 rounded-2xl">
          <div className="flex items-center gap-3">
            <Layers className="w-5 h-5 text-emerald-400" />
            <h3 className="font-semibold text-white text-sm">Clúster Kafka (3 Brokers)</h3>
          </div>
          <p className="text-xs text-slate-400 mt-2">
            kafka1:9092, kafka2:9093, kafka3:9094. Factor de réplica 3 con min.insync.replicas=2. Acks=all e idempotencia activa.
          </p>
        </div>

        <div className="bg-slate-900 border border-slate-800 p-5 rounded-2xl">
          <div className="flex items-center gap-3">
            <Activity className="w-5 h-5 text-indigo-400" />
            <h3 className="font-semibold text-white text-sm">Streaming & Consumer Groups</h3>
          </div>
          <p className="text-xs text-slate-400 mt-2">
            Grupos 'vidasalud-analitica-group' y 'vidasalud-alertas-group' procesando eventos desacoplados en tiempo real.
          </p>
        </div>
      </div>

      {/* Tópicos Existentes */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden">
        <div className="p-5 border-b border-slate-800 flex items-center justify-between">
          <h2 className="font-semibold text-white text-base flex items-center gap-2">
            <Layers className="w-5 h-5 text-sky-400" />
            Tópicos en el Clúster
          </h2>
          <span className="text-xs text-slate-400">{topics.length} tópicos encontrados</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-300">
            <thead className="bg-slate-800/50 text-xs uppercase text-slate-400 font-semibold">
              <tr>
                <th className="px-5 py-3">Nombre del Tópico</th>
                <th className="px-5 py-3 text-center">Particiones</th>
                <th className="px-5 py-3 text-center">Factor Réplica</th>
                <th className="px-5 py-3">Retención (retention.ms)</th>
                <th className="px-5 py-3">Cleanup Policy</th>
                <th className="px-5 py-3 text-right">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {topics.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-5 py-6 text-center text-slate-500">
                    {loading ? 'Consultando tópicos...' : 'No se detectaron tópicos o el clúster Kafka está iniciando.'}
                  </td>
                </tr>
              ) : (
                topics.map((t) => (
                  <tr key={t.name} className="hover:bg-slate-800/30 transition">
                    <td className="px-5 py-3.5 font-medium text-white flex items-center gap-2">
                      {t.isProtected && (
                        <span title="Tópico esencial protegido" className="inline-flex items-center">
                          <ShieldCheck className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                        </span>
                      )}
                      <span>{t.name}</span>
                    </td>
                    <td className="px-5 py-3.5 text-center font-mono text-sky-400">{t.partitions}</td>
                    <td className="px-5 py-3.5 text-center font-mono text-emerald-400">{t.replicationFactor}</td>
                    <td className="px-5 py-3.5 text-xs font-mono text-slate-400">{t.retentionMs || 'default'}</td>
                    <td className="px-5 py-3.5 text-xs text-slate-400">{t.cleanupPolicy || 'delete'}</td>
                    <td className="px-5 py-3.5 text-right">
                      {t.isProtected ? (
                        <span className="text-xs px-2 py-1 bg-slate-800 text-slate-400 rounded-md">Protegido</span>
                      ) : (
                        <button
                          onClick={() => handleDeleteTopic(t.name)}
                          className="text-rose-400 hover:text-rose-300 text-xs font-medium inline-flex items-center gap-1"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                          Eliminar
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Creación y Simulación en 2 Columnas */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Formulario Crear Tópico */}
        <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl">
          <h2 className="font-semibold text-white text-base flex items-center gap-2 mb-4">
            <PlusCircle className="w-5 h-5 text-emerald-400" />
            Crear Nuevo Tópico Administrativo
          </h2>
          <form onSubmit={handleCreateTopic} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Nombre del Tópico</label>
              <input
                type="text"
                value={newTopicName}
                onChange={(e) => setNewTopicName(e.target.value)}
                placeholder="ej: vidasalud.reportes.stream"
                className="w-full px-3.5 py-2 bg-slate-800 border border-slate-700 rounded-xl text-white text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                required
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Particiones</label>
                <input
                  type="number"
                  min="1"
                  max="10"
                  value={newTopicPartitions}
                  onChange={(e) => setNewTopicPartitions(Number(e.target.value))}
                  className="w-full px-3.5 py-2 bg-slate-800 border border-slate-700 rounded-xl text-white text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Factor Réplica</label>
                <input
                  type="number"
                  min="1"
                  max="3"
                  value={newTopicReplicas}
                  onChange={(e) => setNewTopicReplicas(Number(e.target.value))}
                  className="w-full px-3.5 py-2 bg-slate-800 border border-slate-700 rounded-xl text-white text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                />
              </div>
            </div>
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Retención (retention.ms)</label>
              <select
                value={newTopicRetention}
                onChange={(e) => setNewTopicRetention(Number(e.target.value))}
                className="w-full px-3.5 py-2 bg-slate-800 border border-slate-700 rounded-xl text-white text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
              >
                <option value={86400000}>24 Horas (86.400.000 ms)</option>
                <option value={259200000}>3 Días (259.200.000 ms)</option>
                <option value={604800000}>7 Días (604.800.000 ms)</option>
              </select>
            </div>
            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white text-sm font-semibold rounded-xl transition shadow-sm"
            >
              Crear Tópico en Clúster
            </button>
          </form>
        </div>

        {/* Simulador de Eventos de Streaming */}
        <div className="bg-slate-900 border border-slate-800 p-6 rounded-2xl">
          <h2 className="font-semibold text-white text-base flex items-center gap-2 mb-4">
            <Send className="w-5 h-5 text-sky-400" />
            Simulador de Eventos de Streaming (EP6)
          </h2>
          <div className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Tipo de Evento</label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setSimEventType('ATENCIONES')}
                  className={`py-2 px-3 text-xs font-semibold rounded-xl border transition ${
                    simEventType === 'ATENCIONES'
                      ? 'bg-sky-600 text-white border-sky-500'
                      : 'bg-slate-800 text-slate-300 border-slate-700 hover:bg-slate-750'
                  }`}
                >
                  Atención Médica (vidasalud.atenciones.stream)
                </button>
                <button
                  type="button"
                  onClick={() => setSimEventType('TELEMETRIA')}
                  className={`py-2 px-3 text-xs font-semibold rounded-xl border transition ${
                    simEventType === 'TELEMETRIA'
                      ? 'bg-sky-600 text-white border-sky-500'
                      : 'bg-slate-800 text-slate-300 border-slate-700 hover:bg-slate-750'
                  }`}
                >
                  Telemetría (vidasalud.telemetria.stream)
                </button>
              </div>
            </div>

            {simEventType === 'ATENCIONES' ? (
              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Especialidad (Clave de partición)</label>
                <select
                  value={simSpecialty}
                  onChange={(e) => setSimSpecialty(e.target.value)}
                  className="w-full px-3.5 py-2 bg-slate-800 border border-slate-700 rounded-xl text-white text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                >
                  <option value="Cardiología">Cardiología (Partición determinista)</option>
                  <option value="Dermatología">Dermatología</option>
                  <option value="Pediatría">Pediatría</option>
                  <option value="Traumatología">Traumatología</option>
                  <option value="Medicina General">Medicina General</option>
                </select>
              </div>
            ) : (
              <div>
                <label className="block text-xs font-medium text-slate-400 mb-1">Latencia Simulada (ms)</label>
                <input
                  type="number"
                  value={simLatency}
                  onChange={(e) => setSimLatency(Number(e.target.value))}
                  placeholder="ej: 120"
                  className="w-full px-3.5 py-2 bg-slate-800 border border-slate-700 rounded-xl text-white text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                />
                <span className="text-xs text-slate-500 mt-1 block">
                  Valores &gt; 2000 ms gatillan una alerta operacional en el consumidor.
                </span>
              </div>
            )}

            <button
              type="button"
              onClick={handlePublishTestEvent}
              disabled={loading}
              className="w-full py-2.5 bg-sky-600 hover:bg-sky-500 text-white text-sm font-semibold rounded-xl transition shadow-sm flex items-center justify-center gap-2"
            >
              <Send className="w-4 h-4" />
              Publicar Evento a Kafka
            </button>
          </div>
        </div>
      </div>

      {/* Consumer Groups y Lag */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden">
        <div className="p-5 border-b border-slate-800 flex items-center justify-between">
          <h2 className="font-semibold text-white text-base flex items-center gap-2">
            <Users className="w-5 h-5 text-indigo-400" />
            Consumer Groups, Offsets y Lag (EP6)
          </h2>
          <div className="flex items-center gap-2">
            <label htmlFor="offset-reset-strategy" className="text-xs text-slate-400">Reiniciar a</label>
            <select
              id="offset-reset-strategy"
              value={offsetResetStrategy}
              onChange={(event) => setOffsetResetStrategy(event.target.value as 'EARLIEST' | 'LATEST')}
              className="px-2 py-1 bg-slate-800 border border-slate-700 rounded-lg text-xs text-slate-200"
            >
              <option value="EARLIEST">Inicio (reprocesar)</option>
              <option value="LATEST">Final (omitir pendientes)</option>
            </select>
            <span className="text-xs text-slate-400">{consumerGroups.length} grupos</span>
          </div>
        </div>

        <div className="p-5 space-y-4">
          {consumerGroups.length === 0 ? (
            <p className="text-sm text-slate-500 text-center py-4">
              No se registran Consumer Groups activos o el clúster está offline.
            </p>
          ) : (
            consumerGroups.map((group) => (
              <div key={group.groupId} className="p-4 bg-slate-800/40 rounded-xl border border-slate-800">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-slate-800">
                  <div>
                    <h4 className="font-bold text-white text-sm">{group.groupId}</h4>
                    <span className="text-xs text-slate-400">Estado: {group.state} | Miembros: {group.membersCount}</span>
                  </div>
                  <div className="flex items-center gap-4">
                    <button
                      type="button"
                      onClick={() => handleResetGroupOffsets(group.groupId)}
                      disabled={loading || !['EMPTY', 'DEAD'].includes(group.state.toUpperCase())}
                      title={['EMPTY', 'DEAD'].includes(group.state.toUpperCase()) ? 'Reiniciar offsets del grupo detenido' : 'Detén todos los consumidores de este grupo antes de reiniciar offsets'}
                      className="px-3 py-1.5 rounded-lg border border-amber-500/30 text-amber-300 text-xs font-medium hover:bg-amber-500/10 disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                      Reiniciar offsets
                    </button>
                    <div className="text-right">
                      <span className="text-xs text-slate-400 block">Total Lag</span>
                      <span className={`font-mono text-sm font-bold ${group.totalLag > 0 ? 'text-amber-400' : 'text-emerald-400'}`}>
                        {group.totalLag} mensajes
                      </span>
                    </div>
                  </div>
                </div>

                {group.partitions && group.partitions.length > 0 && (
                  <div className="mt-3 overflow-x-auto">
                    <table className="w-full text-xs text-left text-slate-300">
                      <thead className="text-slate-500 uppercase">
                        <tr>
                          <th className="py-1 px-2">Tópico</th>
                          <th className="py-1 px-2 text-center">Partición</th>
                          <th className="py-1 px-2 text-center">Current Offset</th>
                          <th className="py-1 px-2 text-center">Log End Offset</th>
                          <th className="py-1 px-2 text-center">Lag</th>
                          <th className="py-1 px-2">Host / Cliente</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-800/50">
                        {group.partitions.map((p, idx) => (
                          <tr key={idx}>
                            <td className="py-1 px-2 font-mono text-slate-300">{p.topic}</td>
                            <td className="py-1 px-2 text-center font-mono text-sky-400">{p.partition}</td>
                            <td className="py-1 px-2 text-center font-mono">{p.currentOffset}</td>
                            <td className="py-1 px-2 text-center font-mono">{p.logEndOffset}</td>
                            <td className={`py-1 px-2 text-center font-mono font-bold ${p.lag > 0 ? 'text-amber-400' : 'text-emerald-400'}`}>
                              {p.lag}
                            </td>
                            <td className="py-1 px-2 text-slate-400 font-mono text-[11px] truncate max-w-xs">{p.clientId} ({p.host})</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
