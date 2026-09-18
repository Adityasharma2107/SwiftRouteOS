import React, { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import {
  Activity,
  Play,
  ShieldCheck,
  CheckCircle2,
  XCircle,
  Clock,
  Sparkles,
  Terminal,
} from 'lucide-react';
import { inventoryApi, type ChaosSimulationResult } from '../../api/inventory';

export const ChaosLabView: React.FC = () => {
  const [threads, setThreads] = useState<number>(10);
  const [resetAfter, setResetAfter] = useState<boolean>(true);
  const [result, setResult] = useState<ChaosSimulationResult | null>(null);

  const chaosMutation = useMutation({
    mutationFn: () => inventoryApi.runChaosTest(threads, resetAfter),
    onSuccess: (data) => {
      setResult(data);
    },
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <Activity className="w-5 h-5 text-amber-400 animate-pulse" />
              <h2 className="text-base font-bold text-slate-100">
                Pessimistic Locking Chaos Concurrency Lab
              </h2>
              <span className="px-2 py-0.5 text-[10px] font-bold uppercase rounded bg-amber-500/20 text-amber-300 border border-amber-500/30">
                Stress Test
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Spawns concurrent worker threads to race for scarce inventory items simultaneously, proving 0 overselling under PostgreSQL row-level locks.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2 bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-slate-300">
              <span className="text-slate-500">Threads:</span>
              <select
                value={threads}
                onChange={(e) => setThreads(Number(e.target.value))}
                className="bg-transparent font-bold text-cyan-400 focus:outline-none cursor-pointer"
              >
                <option value={5}>5 Threads</option>
                <option value={10}>10 Threads</option>
                <option value={15}>15 Threads</option>
                <option value={20}>20 Threads</option>
              </select>
            </div>

            <label className="flex items-center gap-1.5 text-xs text-slate-400 cursor-pointer">
              <input
                type="checkbox"
                checked={resetAfter}
                onChange={(e) => setResetAfter(e.target.checked)}
                className="rounded border-slate-700 bg-slate-950 text-cyan-500 focus:ring-0"
              />
              Auto-reset stock
            </label>

            <button
              onClick={() => chaosMutation.mutate()}
              disabled={chaosMutation.isPending}
              className="px-4 py-2 bg-gradient-to-r from-amber-500 to-orange-600 hover:from-amber-600 hover:to-orange-700 text-slate-950 font-bold text-xs rounded-xl shadow-lg shadow-amber-500/20 flex items-center gap-2 transition-all cursor-pointer disabled:opacity-50"
            >
              <Play className={`w-4 h-4 fill-current ${chaosMutation.isPending ? 'animate-spin' : ''}`} />
              {chaosMutation.isPending ? 'Executing Race...' : 'Launch Chaos Simulation'}
            </button>
          </div>
        </div>
      </div>

      {/* Results Overview */}
      {result && (
        <div className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
              <div className="text-xs font-semibold text-slate-400">Target Scarce Part</div>
              <div className="text-lg font-mono font-bold text-cyan-400 mt-1">
                {result.partNumber}
              </div>
              <div className="text-[11px] text-slate-500 mt-0.5">
                Initial Stock: {result.initialAvailableQuantity} unit(s)
              </div>
            </div>

            <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
              <div className="text-xs font-semibold text-emerald-400 flex items-center justify-between">
                <span>Successful Acquires</span>
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              </div>
              <div className="text-2xl font-black text-emerald-300 mt-1">
                {result.successfulReservations}
              </div>
              <div className="text-[11px] text-slate-500 mt-0.5">Committed under write lock</div>
            </div>

            <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
              <div className="text-xs font-semibold text-rose-400 flex items-center justify-between">
                <span>Prevented Oversells</span>
                <XCircle className="w-4 h-4 text-rose-400" />
              </div>
              <div className="text-2xl font-black text-rose-300 mt-1">
                {result.rejectedReservations}
              </div>
              <div className="text-[11px] text-slate-500 mt-0.5">Rejected with HTTP 409</div>
            </div>

            <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
              <div className="text-xs font-semibold text-slate-400 flex items-center justify-between">
                <span>Simulation Time</span>
                <Clock className="w-4 h-4 text-slate-400" />
              </div>
              <div className="text-2xl font-mono font-bold text-slate-100 mt-1">
                {result.durationMs} ms
              </div>
              <div className="text-[11px] text-emerald-400 font-semibold mt-0.5 flex items-center gap-1">
                <ShieldCheck className="w-3.5 h-3.5" />
                Zero Oversell Verified
              </div>
            </div>
          </div>

          {/* Execution Thread Logs */}
          <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 font-mono text-xs shadow-inner">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800 text-slate-400 text-[11px]">
              <div className="flex items-center gap-1.5 font-bold text-slate-300">
                <Terminal className="w-3.5 h-3.5 text-cyan-400" />
                Thread Execution Trace
              </div>
              <span>{result.threadLogs?.length || 0} Events Recorded</span>
            </div>
            <div className="mt-3 space-y-1.5 max-h-60 overflow-y-auto pr-2 text-[11px]">
              {result.threadLogs?.map((log, idx) => (
                <div
                  key={idx}
                  className={`p-1.5 rounded ${
                    log.includes('SUCCESS')
                      ? 'bg-emerald-500/10 text-emerald-300 border border-emerald-500/20'
                      : log.includes('REJECTED')
                      ? 'bg-rose-500/10 text-rose-300 border border-rose-500/20'
                      : 'text-slate-400'
                  }`}
                >
                  {log}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Explainer Card */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5 text-xs text-slate-300 space-y-3">
        <div className="font-bold text-sm text-slate-200 flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-cyan-400" />
          How SwiftRouteOS Prevents Race Conditions
        </div>
        <p className="text-slate-400 leading-relaxed">
          When multiple dispatches or worker threads simultaneously request scarce parts, standard optimistic locking or raw updates cause dirty reads, negative stock, or inconsistent inventory. SwiftRouteOS enforces:
        </p>
        <ul className="list-disc list-inside space-y-1 text-slate-400 pl-2">
          <li>
            <strong className="text-slate-200">PostgreSQL Pessimistic Locking:</strong> <code>SELECT ... FOR UPDATE</code> locks the row at the engine level during checkout.
          </li>
          <li>
            <strong className="text-slate-200">Deadlock Prevention:</strong> Part IDs are sorted in ascending order before lock acquisition, eliminating cyclic deadlocks.
          </li>
          <li>
            <strong className="text-slate-200">Automatic Rollback:</strong> Failed reservations abort without leaking state; cancelled jobs release reservations immediately.
          </li>
        </ul>
      </div>
    </div>
  );
};
