import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  X,
  Clock,
  MapPin,
  Phone,
  Wrench,
  Package,
  ShieldAlert,
  XCircle,
  Car,
  Play,
  Check,
  AlertTriangle,
  RefreshCw,
} from 'lucide-react';
import { jobsApi } from '../../api/jobs';
import { inventoryApi } from '../../api/inventory';
import { slaApi } from '../../api/sla';
import { SlaStatusBadge } from '../common/SlaStatusBadge';
import { PriorityBadge } from '../common/PriorityBadge';
import { JobStatusBadge } from '../common/JobStatusBadge';
import type { JobStatus } from '../../types';

interface JobDetailDrawerProps {
  jobId: number | null;
  isOpen: boolean;
  onClose: () => void;
  onOpenDispatchModal: (jobId: number) => void;
}

export const JobDetailDrawer: React.FC<JobDetailDrawerProps> = ({
  jobId,
  isOpen,
  onClose,
  onOpenDispatchModal,
}) => {
  const queryClient = useQueryClient();
  const [transitionNotes, setTransitionNotes] = useState<string>('');
  const [cancellationReason, setCancellationReason] = useState<string>('');
  const [isCancelConfirmOpen, setIsCancelConfirmOpen] = useState<boolean>(false);
  const [transitionError, setTransitionError] = useState<string | null>(null);

  // Fetch single job details
  const {
    data: job,
    isLoading: isJobLoading,
  } = useQuery({
    queryKey: ['job', jobId],
    queryFn: () => (jobId ? jobsApi.getJobById(jobId) : null),
    enabled: isOpen && !!jobId,
  });

  // Fetch job parts reservations
  const { data: reservations = [] } = useQuery({
    queryKey: ['job-reservations', jobId],
    queryFn: () => (jobId ? inventoryApi.getJobReservations(jobId) : []),
    enabled: isOpen && !!jobId,
  });

  // Fetch job SLA escalation events
  const { data: escalationEvents = [] } = useQuery({
    queryKey: ['job-escalations', jobId],
    queryFn: () => (jobId ? slaApi.getJobEscalations(jobId) : []),
    enabled: isOpen && !!jobId,
  });

  // Transition mutation
  const transitionMutation = useMutation({
    mutationFn: async (targetStatus: JobStatus) => {
      if (!jobId) throw new Error('No job selected');
      return jobsApi.transitionJob(
        jobId,
        targetStatus,
        transitionNotes.trim() || undefined,
        targetStatus === 'CANCELLED' ? cancellationReason.trim() : undefined
      );
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['job', jobId] });
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
      queryClient.invalidateQueries({ queryKey: ['job-reservations', jobId] });
      queryClient.invalidateQueries({ queryKey: ['inventory-items'] });
      queryClient.invalidateQueries({ queryKey: ['sla-metrics-summary'] });
      setTransitionNotes('');
      setCancellationReason('');
      setIsCancelConfirmOpen(false);
      setTransitionError(null);
    },
    onError: (err: unknown) => {
      const errorObj = err as { response?: { data?: { error?: { message?: string } } } };
      setTransitionError(
        errorObj.response?.data?.error?.message || 'Failed to transition job status.'
      );
    },
  });

  if (!isOpen || !jobId) return null;

  const currentStatus = job?.status;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-black/60 backdrop-blur-xs flex justify-end animate-in fade-in duration-150">
      <div className="w-full max-w-2xl bg-slate-900 border-l border-slate-800 h-full flex flex-col shadow-2xl overflow-hidden">
        {/* Drawer Header */}
        <div className="p-5 border-b border-slate-800 bg-slate-950 flex items-start justify-between">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="font-mono font-bold text-sm text-cyan-400">Job #{jobId}</span>
              {job && <PriorityBadge priority={job.priority} />}
              {job && <JobStatusBadge status={job.status} />}
              {job && <SlaStatusBadge status={job.slaStatus} />}
            </div>
            <h2 className="text-base font-bold text-slate-100">
              {job?.serviceRequest?.customerName || 'Job Detail Overview'}
            </h2>
            <p className="text-xs text-slate-400 flex items-center gap-1 mt-0.5">
              <MapPin className="w-3.5 h-3.5 text-slate-500" />
              {job?.serviceRequest?.serviceAddress}
            </p>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Drawer Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6 text-xs text-slate-300">
          {isJobLoading ? (
            <div className="py-20 text-center flex flex-col items-center justify-center gap-2 text-slate-400">
              <RefreshCw className="w-6 h-6 animate-spin text-cyan-400" />
              Loading job details...
            </div>
          ) : !job ? (
            <div className="py-20 text-center text-slate-500">Job not found.</div>
          ) : (
            <>
              {/* Error Banner */}
              {transitionError && (
                <div className="bg-rose-500/10 border border-rose-500/30 rounded-xl p-3 text-rose-400 flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4 shrink-0" />
                  <span>{transitionError}</span>
                </div>
              )}

              {/* Operational State Machine Actions */}
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-3">
                <div className="text-[11px] font-bold uppercase tracking-wider text-slate-400 flex items-center justify-between">
                  <span>Lifecycle State Transition Controls</span>
                  <span className="text-[10px] text-slate-500 font-mono">Current: {currentStatus}</span>
                </div>

                {currentStatus === 'PENDING' && (
                  <div className="flex items-center gap-3">
                    <button
                      onClick={() => {
                        onClose();
                        onOpenDispatchModal(job.id);
                      }}
                      className="px-4 py-2 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-bold rounded-lg flex items-center gap-2 shadow-lg shadow-blue-500/20 cursor-pointer"
                    >
                      <Wrench className="w-4 h-4" />
                      Find & Assign Technicians (AI Dispatch)
                    </button>
                  </div>
                )}

                {currentStatus === 'ASSIGNED' && (
                  <div className="flex flex-wrap items-center gap-2">
                    <button
                      onClick={() => transitionMutation.mutate('EN_ROUTE')}
                      disabled={transitionMutation.isPending}
                      className="px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold rounded-lg flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
                    >
                      <Car className="w-3.5 h-3.5" />
                      Mark En Route
                    </button>
                    <button
                      onClick={() => setIsCancelConfirmOpen(true)}
                      className="px-3 py-1.5 bg-rose-950/60 hover:bg-rose-900 text-rose-300 border border-rose-800/60 font-semibold rounded-lg flex items-center gap-1 cursor-pointer"
                    >
                      <XCircle className="w-3.5 h-3.5" />
                      Cancel Job
                    </button>
                  </div>
                )}

                {currentStatus === 'EN_ROUTE' && (
                  <div className="flex flex-wrap items-center gap-2">
                    <button
                      onClick={() => transitionMutation.mutate('IN_PROGRESS')}
                      disabled={transitionMutation.isPending}
                      className="px-3.5 py-1.5 bg-blue-600 hover:bg-blue-500 text-white font-semibold rounded-lg flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
                    >
                      <Play className="w-3.5 h-3.5" />
                      Start Work (In Progress)
                    </button>
                    <button
                      onClick={() => setIsCancelConfirmOpen(true)}
                      className="px-3 py-1.5 bg-rose-950/60 hover:bg-rose-900 text-rose-300 border border-rose-800/60 font-semibold rounded-lg flex items-center gap-1 cursor-pointer"
                    >
                      <XCircle className="w-3.5 h-3.5" />
                      Cancel Job
                    </button>
                  </div>
                )}

                {currentStatus === 'IN_PROGRESS' && (
                  <div className="flex flex-wrap items-center gap-2">
                    <button
                      onClick={() => transitionMutation.mutate('COMPLETED')}
                      disabled={transitionMutation.isPending}
                      className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-bold rounded-lg flex items-center gap-1.5 shadow-lg shadow-emerald-600/20 cursor-pointer disabled:opacity-50"
                    >
                      <Check className="w-4 h-4" />
                      Complete Job & Consume Parts
                    </button>
                    <button
                      onClick={() => setIsCancelConfirmOpen(true)}
                      className="px-3 py-1.5 bg-rose-950/60 hover:bg-rose-900 text-rose-300 border border-rose-800/60 font-semibold rounded-lg flex items-center gap-1 cursor-pointer"
                    >
                      <XCircle className="w-3.5 h-3.5" />
                      Cancel Job
                    </button>
                  </div>
                )}

                {(currentStatus === 'COMPLETED' || currentStatus === 'CANCELLED') && (
                  <div className="text-slate-500 text-xs italic">
                    Job is in terminal state <strong>{currentStatus}</strong>. No further state transitions allowed.
                  </div>
                )}

                {/* Optional Transition Notes Input */}
                {currentStatus !== 'COMPLETED' && currentStatus !== 'CANCELLED' && (
                  <div className="pt-2">
                    <input
                      type="text"
                      placeholder="Optional notes for state transition..."
                      value={transitionNotes}
                      onChange={(e) => setTransitionNotes(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1 text-slate-300 text-[11px] placeholder-slate-500 focus:outline-none focus:border-blue-500"
                    />
                  </div>
                )}

                {/* Cancel Confirm Drawer Dialog */}
                {isCancelConfirmOpen && (
                  <div className="p-3 bg-rose-950/40 border border-rose-800/60 rounded-lg space-y-2 mt-2">
                    <div className="font-bold text-rose-300 flex items-center gap-1">
                      <AlertTriangle className="w-3.5 h-3.5" />
                      Confirm Job Cancellation
                    </div>
                    <p className="text-[11px] text-slate-400">
                      Cancelling this job will automatically release all reserved spare parts back to available stock.
                    </p>
                    <input
                      type="text"
                      placeholder="Cancellation reason (mandatory)..."
                      value={cancellationReason}
                      onChange={(e) => setCancellationReason(e.target.value)}
                      className="w-full bg-slate-900 border border-rose-700/50 rounded p-1.5 text-xs text-slate-200"
                    />
                    <div className="flex justify-end gap-2 pt-1">
                      <button
                        onClick={() => setIsCancelConfirmOpen(false)}
                        className="px-2.5 py-1 rounded bg-slate-800 text-slate-300 text-[11px]"
                      >
                        Keep Job
                      </button>
                      <button
                        onClick={() => transitionMutation.mutate('CANCELLED')}
                        disabled={!cancellationReason.trim() || transitionMutation.isPending}
                        className="px-2.5 py-1 rounded bg-rose-600 hover:bg-rose-500 text-white font-bold text-[11px] disabled:opacity-40"
                      >
                        Confirm Cancellation
                      </button>
                    </div>
                  </div>
                )}
              </div>

              {/* Customer & Service Request Details */}
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-2.5">
                <div className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                  Service Request Information
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <span className="text-slate-500 text-[11px]">Customer Contact:</span>
                    <div className="font-semibold text-slate-200">
                      {job.serviceRequest.customerName}
                    </div>
                    <div className="text-slate-400 flex items-center gap-1 mt-0.5">
                      <Phone className="w-3 h-3 text-slate-500" />
                      {job.serviceRequest.customerPhone}
                    </div>
                  </div>
                  <div>
                    <span className="text-slate-500 text-[11px]">Equipment Type:</span>
                    <div className="font-semibold text-slate-200">
                      {job.serviceRequest.equipmentType}
                    </div>
                    <div className="text-cyan-400 text-[11px]">
                      Required: {job.serviceRequest.requiredSkill?.name}
                    </div>
                  </div>
                </div>

                <div className="pt-2 border-t border-slate-800/60">
                  <span className="text-slate-500 text-[11px]">Issue Reported:</span>
                  <p className="text-slate-300 mt-0.5 leading-relaxed bg-slate-900/60 p-2 rounded-lg border border-slate-800">
                    {job.serviceRequest.issueDescription}
                  </p>
                </div>
              </div>

              {/* SLA Deadlines & Timeline */}
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-3">
                <div className="text-[11px] font-bold uppercase tracking-wider text-slate-400 flex items-center justify-between">
                  <span className="flex items-center gap-1.5">
                    <Clock className="w-3.5 h-3.5 text-cyan-400" />
                    SLA Deadlines & Compliance Status
                  </span>
                  <SlaStatusBadge status={job.slaStatus} />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="bg-slate-900/80 p-2.5 rounded-lg border border-slate-800">
                    <div className="text-[10px] text-slate-400 uppercase font-semibold">
                      Response Deadline
                    </div>
                    <div className="font-mono text-xs font-bold text-slate-200 mt-1">
                      {new Date(job.slaResponseDeadline).toLocaleTimeString()}
                    </div>
                    <div className="text-[10px] text-slate-500 mt-0.5">
                      {job.actualResponseAt
                        ? `Responded at ${new Date(job.actualResponseAt).toLocaleTimeString()}`
                        : 'Awaiting response'}
                    </div>
                  </div>

                  <div className="bg-slate-900/80 p-2.5 rounded-lg border border-slate-800">
                    <div className="text-[10px] text-slate-400 uppercase font-semibold">
                      Resolution Deadline
                    </div>
                    <div className="font-mono text-xs font-bold text-slate-200 mt-1">
                      {new Date(job.slaResolutionDeadline).toLocaleTimeString()}
                    </div>
                    <div className="text-[10px] text-slate-500 mt-0.5">
                      {job.actualResolutionAt
                        ? `Resolved at ${new Date(job.actualResolutionAt).toLocaleTimeString()}`
                        : 'Awaiting resolution'}
                    </div>
                  </div>
                </div>

                {/* Escalation Events */}
                {escalationEvents.length > 0 && (
                  <div className="pt-2 border-t border-slate-800/60 space-y-1.5">
                    <span className="text-[10px] font-bold uppercase text-amber-400 flex items-center gap-1">
                      <ShieldAlert className="w-3 h-3" />
                      Recorded Escalation Events ({escalationEvents.length})
                    </span>
                    {escalationEvents.map((evt) => (
                      <div
                        key={evt.id}
                        className="bg-slate-900 p-2 rounded border border-slate-800 flex items-center justify-between text-[11px]"
                      >
                        <span className="font-semibold text-amber-300">
                          {evt.thresholdStage}
                        </span>
                        <span className="font-mono text-slate-500 text-[10px]">
                          {new Date(evt.triggeredAt).toLocaleTimeString()}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Reserved Parts & Inventory */}
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-3">
                <div className="text-[11px] font-bold uppercase tracking-wider text-slate-400 flex items-center justify-between">
                  <span className="flex items-center gap-1.5">
                    <Package className="w-3.5 h-3.5 text-blue-400" />
                    Allocated Spare Parts ({reservations.length})
                  </span>
                  <span className="text-[10px] text-slate-500 font-mono">Pessimistic Locking</span>
                </div>

                {reservations.length === 0 ? (
                  <div className="text-slate-500 text-[11px] italic">
                    No spare parts currently reserved for this job.
                  </div>
                ) : (
                  <div className="space-y-1.5">
                    {reservations.map((res) => (
                      <div
                        key={res.id}
                        className="bg-slate-900 p-2.5 rounded-lg border border-slate-800 flex items-center justify-between"
                      >
                        <div>
                          <div className="font-bold text-slate-200">
                            {res.partName} <span className="text-cyan-400 font-mono text-[10px]">({res.partNumber})</span>
                          </div>
                          <div className="text-[10px] text-slate-500">
                            Quantity Reserved: {res.quantityReserved} unit(s)
                          </div>
                        </div>

                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-bold font-mono ${
                            res.status === 'RESERVED'
                              ? 'bg-sky-500/10 text-sky-400 border border-sky-500/20'
                              : res.status === 'CONSUMED'
                              ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                              : 'bg-slate-800 text-slate-400'
                          }`}
                        >
                          {res.status}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
