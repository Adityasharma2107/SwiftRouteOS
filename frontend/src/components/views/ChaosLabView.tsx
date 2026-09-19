import React, { useState, useEffect, useRef } from 'react';
import { useMutation } from '@tanstack/react-query';
import {
  Activity,
  Play,
  ShieldCheck,
  CheckCircle2,
  XCircle,
  Sparkles,
  Lock,
  Unlock,
  Cpu,
  Zap,
  AlertOctagon,
  Copy,
  Check,
} from 'lucide-react';
import { inventoryApi, type ChaosSimulationResult } from '../../api/inventory';
import { useWebSocket } from '../../context/WebSocketContext';

interface VisualThreadState {
  id: number;
  name: string;
  stage: 'idle' | 'contending' | 'success' | 'rejected';
  log?: string;
  delayMs?: number;
}

export const ChaosLabView: React.FC = () => {
  const [threads, setThreads] = useState<number>(10);
  const [resetAfter, setResetAfter] = useState<boolean>(true);
  const [result, setResult] = useState<ChaosSimulationResult | null>(null);
  const [activeTab, setActiveTab] = useState<'lanes' | 'rollbacks' | 'logs'>('lanes');
  const [logFilter, setLogFilter] = useState<'all' | 'success' | 'rejected'>('all');
  const [copied, setCopied] = useState(false);
  const [simulatedThreads, setSimulatedThreads] = useState<VisualThreadState[]>([]);
  const [isLockHeld, setIsLockHeld] = useState(false);
  const { publish } = useWebSocket();
  const animationTimerRef = useRef<ReturnType<typeof setTimeout>[]>([]);

  // Cleanup animation timers on unmount
  useEffect(() => {
    return () => {
      animationTimerRef.current.forEach((t) => clearTimeout(t));
    };
  }, []);

  const chaosMutation = useMutation({
    mutationFn: () => inventoryApi.runChaosTest(threads, resetAfter),
    onMutate: () => {
      // Clear previous timers
      animationTimerRef.current.forEach((t) => clearTimeout(t));
      animationTimerRef.current = [];

      // Initialize visual thread lanes in contending state
      const initial: VisualThreadState[] = Array.from({ length: threads }, (_, i) => ({
        id: i + 1,
        name: `Thread-${String(i + 1).padStart(2, '0')}`,
        stage: 'contending',
      }));
      setSimulatedThreads(initial);
      setIsLockHeld(true);
    },
    onSuccess: (data) => {
      setResult(data);

      // Publish WebSocket event so other connected tabs or clients see live chaos action
      publish('/topic/chaos', {
        eventType: 'CHAOS_EVENT',
        message: `Chaos race simulated with ${data.threadsAttempted} threads: ${data.successfulReservations} acquired, ${data.rejectedReservations} 409 rollbacks.`,
        payload: {
          partNumber: data.partNumber,
          threads: data.threadsAttempted,
          durationMs: data.durationMs,
        },
      });

      // Parse backend threadLogs to map realistic final states
      const logMap = new Map<number, { success: boolean; log: string }>();
      data.threadLogs.forEach((log) => {
        const match = log.match(/\[Thread-(\d+)\]/);
        if (match) {
          const tid = parseInt(match[1], 10);
          const isSuccess = log.includes('SUCCESS');
          logMap.set(tid, { success: isSuccess, log });
        }
      });

      // Stagger thread lane animations for realistic visual race effect
      const updated: VisualThreadState[] = Array.from({ length: data.threadsAttempted }, (_, i) => {
        const tid = i + 1;
        const entry = logMap.get(tid);
        const isSuccess = entry ? entry.success : tid <= data.successfulReservations;
        const log = entry ? entry.log : isSuccess ? 'Acquired row lock' : 'REJECTED: 409 Conflict';

        return {
          id: tid,
          name: `Thread-${String(tid).padStart(2, '0')}`,
          stage: isSuccess ? 'success' : 'rejected',
          log,
        };
      });

      // Animate transition
      const timer = setTimeout(() => {
        setSimulatedThreads(updated);
        setIsLockHeld(false);
      }, 400);
      animationTimerRef.current.push(timer);
    },
    onError: () => {
      setIsLockHeld(false);
      setSimulatedThreads((prev) =>
        prev.map((t) => ({ ...t, stage: 'rejected', log: 'Failed to complete transaction' }))
      );
    },
  });

  const handleCopyLogs = () => {
    if (!result?.threadLogs) return;
    navigator.clipboard.writeText(result.threadLogs.join('\n'));
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const filteredLogs = result?.threadLogs?.filter((log) => {
    if (logFilter === 'success') return log.includes('SUCCESS');
    if (logFilter === 'rejected') return log.includes('REJECTED') || log.includes('FAILED');
    return true;
  });

  const rejectedThreads = simulatedThreads.filter((t) => t.stage === 'rejected');

  return (
    <div className="space-y-6">
      {/* Header & Controls Panel */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm">
        <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <Activity className="w-5 h-5 text-amber-400 animate-pulse" />
              <h2 className="text-base font-bold text-slate-100">
                Pessimistic Locking Chaos Concurrency Lab
              </h2>
              <span className="px-2 py-0.5 text-[10px] font-bold uppercase rounded bg-amber-500/20 text-amber-300 border border-amber-500/30">
                Real-Time Race Simulation
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Spawns concurrent worker threads executing <code className="text-cyan-400 font-mono">SELECT ... FOR UPDATE</code> simultaneously against scarce parts to prove zero oversell invariants.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2 bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-slate-300">
              <span className="text-slate-500 flex items-center gap-1">
                <Cpu className="w-3.5 h-3.5 text-cyan-400" />
                Threads:
              </span>
              <select
                value={threads}
                onChange={(e) => setThreads(Number(e.target.value))}
                className="bg-transparent font-bold text-cyan-400 focus:outline-none cursor-pointer"
                disabled={chaosMutation.isPending}
              >
                <option value={5} className="bg-slate-900 text-slate-200">5 Threads</option>
                <option value={10} className="bg-slate-900 text-slate-200">10 Threads</option>
                <option value={15} className="bg-slate-900 text-slate-200">15 Threads</option>
                <option value={20} className="bg-slate-900 text-slate-200">20 Threads</option>
              </select>
            </div>

            <label className="flex items-center gap-1.5 text-xs text-slate-400 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={resetAfter}
                onChange={(e) => setResetAfter(e.target.checked)}
                className="rounded border-slate-700 bg-slate-950 text-cyan-500 focus:ring-0 cursor-pointer"
                disabled={chaosMutation.isPending}
              />
              Auto-reset stock
            </label>

            <button
              onClick={() => chaosMutation.mutate()}
              disabled={chaosMutation.isPending}
              className="px-4 py-2 bg-gradient-to-r from-amber-500 to-orange-600 hover:from-amber-600 hover:to-orange-700 text-slate-950 font-bold text-xs rounded-xl shadow-lg shadow-amber-500/20 flex items-center gap-2 transition-all cursor-pointer disabled:opacity-50"
            >
              <Play className={`w-4 h-4 fill-current ${chaosMutation.isPending ? 'animate-spin' : ''}`} />
              {chaosMutation.isPending ? 'Executing Concurrency Race...' : 'Launch Chaos Simulation'}
            </button>
          </div>
        </div>
      </div>

      {/* Central Concurrency Lock Vault */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <div className="lg:col-span-2 bg-gradient-to-br from-slate-900 via-slate-900 to-slate-950 border border-slate-800 rounded-xl p-5 shadow-sm">
          <div className="flex items-center justify-between pb-3 border-b border-slate-800">
            <div className="flex items-center gap-2">
              <Zap className="w-4 h-4 text-cyan-400" />
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-300">
                PostgreSQL Engine Row-Lock Status
              </h3>
            </div>
            <span className="text-[11px] font-mono text-slate-400">
              Isolation: READ_COMMITTED + FOR UPDATE
            </span>
          </div>

          <div className="mt-4 grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="p-3 rounded-lg bg-slate-950/60 border border-slate-800/80">
              <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">Lock Engine State</span>
              <div className="flex items-center gap-2 mt-1.5">
                {isLockHeld ? (
                  <>
                    <Lock className="w-5 h-5 text-amber-400 animate-pulse" />
                    <span className="text-xs font-black text-amber-400 uppercase">HELD (EXCLUSIVE)</span>
                  </>
                ) : (
                  <>
                    <Unlock className="w-5 h-5 text-emerald-400" />
                    <span className="text-xs font-black text-emerald-400 uppercase">UNLOCKED / IDLE</span>
                  </>
                )}
              </div>
            </div>

            <div className="p-3 rounded-lg bg-slate-950/60 border border-slate-800/80">
              <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">Target Database Record</span>
              <div className="text-xs font-mono font-bold text-cyan-400 mt-1.5 truncate">
                {result?.partNumber || 'SCARCE-SENSOR-CHILLER'}
              </div>
            </div>

            <div className="p-3 rounded-lg bg-slate-950/60 border border-slate-800/80">
              <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">Oversell Invariant</span>
              <div className="flex items-center gap-1.5 mt-1.5 text-xs font-bold text-emerald-400">
                <ShieldCheck className="w-4 h-4 text-emerald-400" />
                Guaranteed Zero Oversell
              </div>
            </div>
          </div>

          {/* Real-time thread progress bar */}
          <div className="mt-4 pt-3 border-t border-slate-800/80">
            <div className="flex items-center justify-between text-xs text-slate-400 mb-1.5">
              <span>Thread Contention Progress</span>
              <span className="font-mono text-cyan-400">
                {simulatedThreads.length > 0
                  ? `${simulatedThreads.filter((t) => t.stage !== 'contending').length} / ${threads} Threads Completed`
                  : 'Ready'}
              </span>
            </div>
            <div className="w-full h-2.5 bg-slate-950 rounded-full overflow-hidden flex">
              <div
                className="bg-emerald-500 transition-all duration-300"
                style={{
                  width: `${
                    threads > 0
                      ? ((result?.successfulReservations ?? 0) / threads) * 100
                      : 0
                  }%`,
                }}
              />
              <div
                className="bg-rose-500 transition-all duration-300"
                style={{
                  width: `${
                    threads > 0
                      ? ((result?.rejectedReservations ?? 0) / threads) * 100
                      : 0
                  }%`,
                }}
              />
            </div>
          </div>
        </div>

        {/* Live Metrics Summary Card */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 flex flex-col justify-between shadow-sm">
          <div>
            <span className="text-xs font-semibold text-slate-400">Race Simulation Latency</span>
            <div className="text-3xl font-mono font-black text-slate-100 mt-1">
              {result ? `${result.durationMs} ms` : '--'}
            </div>
            <p className="text-[11px] text-slate-400 mt-1">
              Simultaneous thread latch release time to full transaction commit / rollback.
            </p>
          </div>

          <div className="mt-4 pt-4 border-t border-slate-800 grid grid-cols-2 gap-2 text-center">
            <div className="p-2.5 rounded-lg bg-emerald-500/10 border border-emerald-500/20">
              <div className="text-[10px] font-bold uppercase text-emerald-400">Acquired</div>
              <div className="text-xl font-black text-emerald-300 mt-0.5">
                {result?.successfulReservations ?? 0}
              </div>
            </div>
            <div className="p-2.5 rounded-lg bg-rose-500/10 border border-rose-500/20">
              <div className="text-[10px] font-bold uppercase text-rose-400">Prevented (409)</div>
              <div className="text-xl font-black text-rose-300 mt-0.5">
                {result?.rejectedReservations ?? 0}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Tabs: Live Thread Lanes | 409 Rollbacks | Execution Trace */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl shadow-sm overflow-hidden">
        <div className="flex items-center justify-between px-5 pt-4 pb-2 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <button
              onClick={() => setActiveTab('lanes')}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-colors cursor-pointer ${
                activeTab === 'lanes'
                  ? 'bg-cyan-500/20 text-cyan-300 border border-cyan-500/40'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Live Thread Lanes ({simulatedThreads.length || threads})
            </button>
            <button
              onClick={() => setActiveTab('rollbacks')}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-colors cursor-pointer ${
                activeTab === 'rollbacks'
                  ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              409 Conflict Rollbacks ({result?.rejectedReservations ?? 0})
            </button>
            <button
              onClick={() => setActiveTab('logs')}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-colors cursor-pointer ${
                activeTab === 'logs'
                  ? 'bg-blue-500/20 text-blue-300 border border-blue-500/40'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Engine Execution Trace ({result?.threadLogs?.length ?? 0})
            </button>
          </div>

          {activeTab === 'logs' && result?.threadLogs && (
            <div className="flex items-center gap-2">
              <div className="flex items-center gap-1 bg-slate-950 border border-slate-800 rounded-md p-0.5 text-[10px]">
                <button
                  onClick={() => setLogFilter('all')}
                  className={`px-2 py-0.5 rounded cursor-pointer ${
                    logFilter === 'all' ? 'bg-slate-800 text-slate-100 font-bold' : 'text-slate-400'
                  }`}
                >
                  All
                </button>
                <button
                  onClick={() => setLogFilter('success')}
                  className={`px-2 py-0.5 rounded cursor-pointer ${
                    logFilter === 'success' ? 'bg-emerald-950 text-emerald-400 font-bold' : 'text-slate-400'
                  }`}
                >
                  Success
                </button>
                <button
                  onClick={() => setLogFilter('rejected')}
                  className={`px-2 py-0.5 rounded cursor-pointer ${
                    logFilter === 'rejected' ? 'bg-rose-950 text-rose-400 font-bold' : 'text-slate-400'
                  }`}
                >
                  409 Conflicts
                </button>
              </div>
              <button
                onClick={handleCopyLogs}
                className="flex items-center gap-1 text-[11px] text-slate-400 hover:text-cyan-400 px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 transition-colors cursor-pointer"
              >
                {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                {copied ? 'Copied' : 'Copy'}
              </button>
            </div>
          )}
        </div>

        {/* Tab 1: Live Animated Thread Lanes */}
        {activeTab === 'lanes' && (
          <div className="p-5">
            {simulatedThreads.length === 0 ? (
              <div className="py-12 text-center text-slate-500 text-xs">
                Click &quot;Launch Chaos Simulation&quot; above to view live animated thread racing and lock contention.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {simulatedThreads.map((thread) => {
                  const isSuccess = thread.stage === 'success';
                  const isRejected = thread.stage === 'rejected';
                  const isContending = thread.stage === 'contending';

                  return (
                    <div
                      key={thread.id}
                      className={`p-3.5 rounded-xl border transition-all duration-300 ${
                        isSuccess
                          ? 'bg-emerald-950/20 border-emerald-500/40 shadow-sm shadow-emerald-950/20'
                          : isRejected
                          ? 'bg-rose-950/20 border-rose-500/40 shadow-sm shadow-rose-950/20'
                          : 'bg-slate-950/60 border-amber-500/40 animate-pulse'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <Cpu className="w-4 h-4 text-slate-400" />
                          <span className="text-xs font-mono font-bold text-slate-200">
                            {thread.name}
                          </span>
                        </div>

                        {isSuccess && (
                          <span className="flex items-center gap-1 text-[10px] font-black uppercase text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/30">
                            <CheckCircle2 className="w-3 h-3" />
                            Lock Acquired (200)
                          </span>
                        )}

                        {isRejected && (
                          <span className="flex items-center gap-1 text-[10px] font-black uppercase text-rose-400 bg-rose-500/10 px-2 py-0.5 rounded border border-rose-500/30">
                            <XCircle className="w-3 h-3" />
                            Rollback (409 Conflict)
                          </span>
                        )}

                        {isContending && (
                          <span className="flex items-center gap-1 text-[10px] font-black uppercase text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded border border-amber-500/30">
                            <Activity className="w-3 h-3 animate-spin" />
                            Contending Lock...
                          </span>
                        )}
                      </div>

                      <div className="mt-2 text-[11px] text-slate-400 font-mono truncate">
                        {thread.log || (isContending ? 'Awaiting PostgreSQL row lock...' : 'Complete')}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* Tab 2: 409 Conflict Rollback Cards */}
        {activeTab === 'rollbacks' && (
          <div className="p-5 space-y-3">
            {rejectedThreads.length === 0 ? (
              <div className="py-12 text-center text-slate-500 text-xs">
                No 409 conflicts recorded in this run. (All contending threads acquired stock or no test has run yet).
              </div>
            ) : (
              <div className="space-y-3">
                <div className="p-3 bg-rose-950/30 border border-rose-500/30 rounded-lg text-xs text-rose-300 flex items-center gap-2">
                  <AlertOctagon className="w-4 h-4 text-rose-400 shrink-0" />
                  <span>
                    <strong>{rejectedThreads.length} Race Conditions Blocked:</strong> These worker threads encountered zero available quantity under exclusive lock and immediately triggered atomic transaction rollbacks with zero state leakage.
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  {rejectedThreads.map((thread) => (
                    <div
                      key={thread.id}
                      className="p-3.5 bg-slate-950 border border-rose-900/40 rounded-xl"
                    >
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-mono font-bold text-rose-300">
                          {thread.name}
                        </span>
                        <span className="px-1.5 py-0.5 text-[9px] font-black uppercase rounded bg-rose-500/20 text-rose-400 border border-rose-500/30">
                          HTTP 409 Conflict
                        </span>
                      </div>
                      <div className="mt-2 text-xs text-slate-300 font-sans">
                        <strong>Exception:</strong> <span className="font-mono text-[11px] text-rose-300">InsufficientInventoryException</span>
                      </div>
                      <div className="mt-1 text-[11px] text-slate-400 font-mono">
                        {thread.log}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Tab 3: Execution Trace Logs */}
        {activeTab === 'logs' && (
          <div className="p-5 font-mono text-xs">
            {filteredLogs && filteredLogs.length > 0 ? (
              <div className="space-y-1.5 max-h-80 overflow-y-auto pr-2 text-[11px]">
                {filteredLogs.map((log, idx) => (
                  <div
                    key={idx}
                    className={`p-2 rounded font-mono ${
                      log.includes('SUCCESS')
                        ? 'bg-emerald-500/10 text-emerald-300 border border-emerald-500/20'
                        : log.includes('REJECTED')
                        ? 'bg-rose-500/10 text-rose-300 border border-rose-500/20'
                        : 'bg-slate-950 text-slate-400'
                    }`}
                  >
                    {log}
                  </div>
                ))}
              </div>
            ) : (
              <div className="py-12 text-center text-slate-500 text-xs">
                No trace logs available. Launch a simulation to record engine traces.
              </div>
            )}
          </div>
        )}
      </div>

      {/* Educational Explainer Card */}
      <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5 text-xs text-slate-300 space-y-3">
        <div className="font-bold text-sm text-slate-200 flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-cyan-400" />
          Under The Hood: PostgreSQL Row-Level Locking Architecture
        </div>
        <p className="text-slate-400 leading-relaxed">
          When multiple dispatches or worker threads simultaneously request scarce inventory parts, ordinary optimistic locking or uncoordinated updates cause dirty reads, negative stock, and phantom inventory. SwiftRouteOS strictly enforces:
        </p>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3 pt-1">
          <div className="p-3 bg-slate-950 border border-slate-800/80 rounded-lg">
            <h4 className="font-bold text-slate-200 mb-1">1. Pessimistic Write Lock</h4>
            <p className="text-[11px] text-slate-400">
              <code className="text-cyan-400">SELECT ... FOR UPDATE</code> holds an exclusive row-level lock in PostgreSQL for the duration of the reservation transaction.
            </p>
          </div>
          <div className="p-3 bg-slate-950 border border-slate-800/80 rounded-lg">
            <h4 className="font-bold text-slate-200 mb-1">2. Deadlock Elimination</h4>
            <p className="text-[11px] text-slate-400">
              Multi-part reservations are sorted by ID in ascending order prior to acquisition, preventing cyclic lock dependency graphs.
            </p>
          </div>
          <div className="p-3 bg-slate-950 border border-slate-800/80 rounded-lg">
            <h4 className="font-bold text-slate-200 mb-1">3. Atomic Rollback (409)</h4>
            <p className="text-[11px] text-slate-400">
              When available stock reaches zero, contending threads fail fast with HTTP 409 Conflict without leaking intermediate state.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
