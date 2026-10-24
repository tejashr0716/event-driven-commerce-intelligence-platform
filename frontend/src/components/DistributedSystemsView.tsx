import React from 'react';
import { OutboxEvent, SystemStatus, AuditLogItem } from '../types';
import { Database, Play, AlertOctagon, CheckCircle2, ShieldCheck, Server, RefreshCw, Cpu } from 'lucide-react';

interface DistributedSystemsViewProps {
  outboxEvents: OutboxEvent[];
  dlqEvents: OutboxEvent[];
  systemStatus: SystemStatus | null;
  auditLogs: AuditLogItem[];
  onDispatchOutbox: () => void;
  isDispatching: boolean;
  onRefresh: () => void;
}

export const DistributedSystemsView: React.FC<DistributedSystemsViewProps> = ({
  outboxEvents,
  dlqEvents,
  systemStatus,
  auditLogs,
  onDispatchOutbox,
  isDispatching,
  onRefresh,
}) => {
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center space-x-2">
            <h2 className="text-lg font-bold text-slate-900">Distributed Systems & Transactional Outbox</h2>
            <span className="px-2 py-0.5 text-xs font-mono bg-sky-100 text-sky-800 border border-sky-300 rounded-full font-bold">
              Dual-Write Safety Guaranteed
            </span>
          </div>
          <p className="text-xs text-slate-500 font-mono">
            Outbox pattern decouples transactional mutations from message broker delivery, guaranteeing at-least-once delivery with DLQ routing
          </p>
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={onDispatchOutbox}
            disabled={isDispatching}
            className="px-3.5 py-1.5 bg-sky-600 hover:bg-sky-500 disabled:opacity-50 text-white rounded-lg text-xs font-semibold shadow-sm transition flex items-center space-x-1.5"
          >
            <Play className={`w-3.5 h-3.5 ${isDispatching ? 'animate-spin' : ''}`} />
            <span>{isDispatching ? 'Publishing...' : 'Dispatch Outbox Now'}</span>
          </button>
          <button
            onClick={onRefresh}
            className="px-3 py-1.5 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg text-xs font-semibold text-slate-700 shadow-sm transition"
          >
            Refresh
          </button>
        </div>
      </div>

      {/* System Status & Adapters */}
      {systemStatus && (
        <div className="bg-slate-900 text-white p-5 rounded-xl border border-slate-800 shadow-sm space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between border-b border-slate-800 pb-3 gap-2">
            <div className="flex items-center space-x-3">
              <div className="p-2 bg-emerald-500/20 text-emerald-400 rounded-lg border border-emerald-500/30">
                <Server className="w-5 h-5" />
              </div>
              <div>
                <div className="flex items-center space-x-2">
                  <span className="font-bold text-sm text-white">{systemStatus.serviceName}</span>
                  <span className="px-2 py-0.5 text-[10px] font-mono bg-emerald-950 text-emerald-400 border border-emerald-800 rounded font-semibold">
                    ● {systemStatus.status}
                  </span>
                </div>
                <p className="text-xs text-slate-400 font-mono">
                  Catalog Origin: <span className="text-sky-300">{systemStatus.catalogSource}</span>
                </p>
              </div>
            </div>

            <div className="text-xs font-mono text-slate-300 bg-slate-950 px-3 py-1.5 rounded-lg border border-slate-800">
              Pending Outbox Events: <span className="text-sky-400 font-bold">{systemStatus.pendingOutboxCount}</span>
            </div>
          </div>

          {/* Adapters Matrix */}
          <div>
            <span className="text-[11px] font-mono text-slate-400 uppercase tracking-wider block mb-2 font-semibold">
              Enterprise Adapters & Resilience Topology
            </span>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
              {Object.entries(systemStatus.adapters || {}).map(([name, desc]) => (
                <div key={name} className="p-2.5 bg-slate-950 rounded-lg border border-slate-800 text-xs">
                  <div className="text-sky-400 font-mono font-semibold flex items-center gap-1.5">
                    <Cpu className="w-3.5 h-3.5 text-slate-400" /> {name}
                  </div>
                  <div className="text-slate-400 text-[11px] mt-0.5">{desc}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Outbox Events Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-5 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center space-x-2">
            <Database className="w-4 h-4 text-sky-600" />
            <h3 className="font-bold text-sm text-slate-900">Transactional Outbox Event Log</h3>
          </div>
          <span className="text-xs font-mono text-slate-500">{outboxEvents.length} events logged</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs font-mono">
            <thead>
              <tr className="bg-slate-50 text-slate-500 border-y border-slate-200">
                <th className="p-2.5">ID</th>
                <th className="p-2.5">Event Type</th>
                <th className="p-2.5">Aggregate</th>
                <th className="p-2.5">Status</th>
                <th className="p-2.5">Retries</th>
                <th className="p-2.5">Created At</th>
                <th className="p-2.5">Payload Preview</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {outboxEvents.slice(0, 10).map((event) => (
                <tr key={event.id} className="hover:bg-slate-50/80">
                  <td className="p-2.5 font-bold text-slate-700">#{event.id}</td>
                  <td className="p-2.5 font-semibold text-sky-700">{event.eventType}</td>
                  <td className="p-2.5 text-slate-600">
                    {event.aggregateType}:{event.aggregateId}
                  </td>
                  <td className="p-2.5">
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        event.status === 'PROCESSED'
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          : event.status === 'FAILED'
                          ? 'bg-rose-50 text-rose-700 border border-rose-200'
                          : 'bg-amber-50 text-amber-700 border border-amber-200'
                      }`}
                    >
                      {event.status}
                    </span>
                  </td>
                  <td className="p-2.5 text-slate-600">{event.retryCount}</td>
                  <td className="p-2.5 text-slate-400">
                    {new Date(event.createdAt).toLocaleTimeString()}
                  </td>
                  <td className="p-2.5 max-w-xs truncate text-slate-500 font-sans" title={event.payloadJson}>
                    {event.payloadJson}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Dead Letter Queue (DLQ) */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-5 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center space-x-2">
            <AlertOctagon className="w-4 h-4 text-rose-600" />
            <h3 className="font-bold text-sm text-slate-900">Dead Letter Queue (DLQ)</h3>
          </div>
          <span className="text-xs font-mono text-slate-500">{dlqEvents.length} routed messages</span>
        </div>

        {dlqEvents.length === 0 ? (
          <div className="p-6 bg-slate-50 rounded-lg text-center text-xs text-slate-500 font-mono">
            <CheckCircle2 className="w-6 h-6 text-emerald-500 mx-auto mb-1.5" />
            Dead Letter Queue is clear. Zero poison pills or failed deliveries currently active.
          </div>
        ) : (
          <div className="space-y-2">
            {dlqEvents.map((dlq) => (
              <div
                key={dlq.id}
                className="p-3 bg-rose-50 rounded-lg border border-rose-200 text-xs font-mono flex items-start justify-between"
              >
                <div>
                  <div className="font-bold text-rose-800">
                    Event #{dlq.id} [{dlq.eventType}] - Aggregate {dlq.aggregateId}
                  </div>
                  <div className="text-rose-600 text-[11px] mt-0.5">
                    Reason: {dlq.errorMessage || 'Max retry attempts exceeded'}
                  </div>
                </div>
                <span className="px-2 py-0.5 bg-rose-200 text-rose-900 font-bold rounded text-[10px]">
                  DLQ ROUTED
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Immutable Audit Logs */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm p-5 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center space-x-2">
            <ShieldCheck className="w-4 h-4 text-emerald-600" />
            <h3 className="font-bold text-sm text-slate-900">Immutable Audit Trail</h3>
          </div>
          <span className="text-xs font-mono text-slate-500">Correlation ID Verified</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs font-mono">
            <thead>
              <tr className="bg-slate-50 text-slate-500 border-y border-slate-200">
                <th className="p-2.5">Timestamp</th>
                <th className="p-2.5">Action</th>
                <th className="p-2.5">Actor</th>
                <th className="p-2.5">Correlation ID</th>
                <th className="p-2.5">Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {auditLogs.slice(0, 10).map((log) => (
                <tr key={log.id} className="hover:bg-slate-50/80">
                  <td className="p-2.5 text-slate-400">
                    {new Date(log.timestamp).toLocaleTimeString()}
                  </td>
                  <td className="p-2.5 font-semibold text-slate-800">{log.actionType}</td>
                  <td className="p-2.5 text-sky-700">{log.actorName}</td>
                  <td className="p-2.5 text-slate-500 font-mono text-[11px]">{log.correlationId}</td>
                  <td className="p-2.5 text-slate-600 truncate max-w-xs">{log.detailsJson}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
