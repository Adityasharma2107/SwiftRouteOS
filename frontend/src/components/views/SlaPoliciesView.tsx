import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ShieldAlert, RefreshCw, CheckCircle, Clock, Zap } from 'lucide-react';
import { slaApi } from '../../api/sla';
import { PriorityBadge } from '../common/PriorityBadge';

export const SlaPoliciesView: React.FC = () => {
  const queryClient = useQueryClient();

  const { data: policies = [], isLoading } = useQuery({
    queryKey: ['sla-policies'],
    queryFn: slaApi.getPolicies,
  });

  const { data: slaMetrics } = useQuery({
    queryKey: ['sla-metrics-summary'],
    queryFn: slaApi.getMetricsSummary,
  });

  const sweepMutation = useMutation({
    mutationFn: slaApi.triggerBatchEvaluation,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sla-metrics-summary'] });
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
    },
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
            <ShieldAlert className="w-5 h-5 text-emerald-400" />
            SLA Policies & Scheduled Escalation Engine
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Real-time compliance monitoring, 70% early warnings, and idempotent escalation event logging.
          </p>
        </div>
        <button
          onClick={() => sweepMutation.mutate()}
          disabled={sweepMutation.isPending}
          className="px-3.5 py-1.5 bg-blue-600 hover:bg-blue-500 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer disabled:opacity-50"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${sweepMutation.isPending ? 'animate-spin' : ''}`} />
          {sweepMutation.isPending ? 'Sweeping...' : 'Run SLA Sweep Now'}
        </button>
      </div>

      {/* Policies Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
        <div className="p-4 border-b border-slate-800 flex items-center justify-between">
          <div className="text-sm font-bold text-slate-200">Configured Priority SLA Policies</div>
          <span className="text-xs text-slate-400 font-mono">Table: sla_policies</span>
        </div>

        {isLoading ? (
          <div className="p-12 text-center text-xs text-slate-400 flex flex-col items-center justify-center gap-2">
            <RefreshCw className="w-5 h-5 animate-spin text-blue-500" />
            Loading SLA policies...
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-950/60 text-[11px] font-semibold text-slate-400 uppercase tracking-wider border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3">Priority Level</th>
                  <th className="px-4 py-3">Response SLA Window</th>
                  <th className="px-4 py-3">Resolution SLA Window</th>
                  <th className="px-4 py-3">Warning Stage</th>
                  <th className="px-4 py-3 text-right">Idempotency Guarantee</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {policies.map((policy) => (
                  <tr key={policy.id} className="hover:bg-slate-800/40 transition-colors">
                    <td className="px-4 py-3 font-semibold">
                      <PriorityBadge priority={policy.priority} />
                    </td>
                    <td className="px-4 py-3 font-mono text-slate-200">
                      <span className="flex items-center gap-1">
                        <Clock className="w-3.5 h-3.5 text-cyan-400" />
                        {policy.responseDeadlineMinutes} min
                        <span className="text-slate-500 text-[11px]">
                          ({(policy.responseDeadlineMinutes / 60).toFixed(1)}h)
                        </span>
                      </span>
                    </td>
                    <td className="px-4 py-3 font-mono text-slate-200">
                      <span className="flex items-center gap-1">
                        <Clock className="w-3.5 h-3.5 text-blue-400" />
                        {policy.resolutionDeadlineMinutes} min
                        <span className="text-slate-500 text-[11px]">
                          ({(policy.resolutionDeadlineMinutes / 60).toFixed(1)}h)
                        </span>
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-amber-400 font-medium">
                        &ge; 70% elapsed duration
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <span className="inline-flex items-center gap-1 text-[11px] text-emerald-400 font-mono">
                        <CheckCircle className="w-3 h-3" />
                        UNIQUE(job_id, threshold_stage)
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Scheduled Worker Overview */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-3">
          <div className="text-sm font-bold text-slate-200 flex items-center gap-2">
            <Zap className="w-4 h-4 text-cyan-400" />
            Scheduled Background Worker
          </div>
          <p className="text-xs text-slate-400 leading-relaxed">
            <code>SlaEscalationWorker</code> executes continuously in the background on Spring Boot:
          </p>
          <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 font-mono text-xs space-y-1 text-slate-300">
            <div>Schedule Rate: <span className="text-cyan-400">30,000 ms (30s)</span></div>
            <div>Warning Threshold: <span className="text-amber-400">70.0% elapsed</span></div>
            <div>Breach Threshold: <span className="text-rose-400">&ge; 100.0% elapsed</span></div>
            <div>Audit Event: <span className="text-emerald-400">AuditAction.ESCALATED</span></div>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-3">
          <div className="text-sm font-bold text-slate-200 flex items-center gap-2">
            <ShieldAlert className="w-4 h-4 text-emerald-400" />
            Current Compliance Metrics
          </div>
          <div className="grid grid-cols-2 gap-2 text-xs">
            <div className="bg-slate-950 p-2.5 rounded-lg border border-slate-800">
              <span className="text-slate-400 text-[11px]">Compliance Rate</span>
              <div className="text-lg font-bold text-emerald-400 mt-0.5">
                {slaMetrics?.complianceRatePercent?.toFixed(1) ?? '100.0'}%
              </div>
            </div>
            <div className="bg-slate-950 p-2.5 rounded-lg border border-slate-800">
              <span className="text-slate-400 text-[11px]">Healthy Jobs</span>
              <div className="text-lg font-bold text-slate-200 mt-0.5">
                {slaMetrics?.healthyJobs ?? 0}
              </div>
            </div>
            <div className="bg-slate-950 p-2.5 rounded-lg border border-slate-800">
              <span className="text-slate-400 text-[11px]">Nearing Breach</span>
              <div className="text-lg font-bold text-amber-400 mt-0.5">
                {slaMetrics?.nearingBreachJobs ?? 0}
              </div>
            </div>
            <div className="bg-slate-950 p-2.5 rounded-lg border border-slate-800">
              <span className="text-slate-400 text-[11px]">Breached Jobs</span>
              <div className="text-lg font-bold text-rose-400 mt-0.5">
                {slaMetrics?.breachedJobs ?? 0}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
