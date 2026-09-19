import React, { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  X,
  AlertTriangle,
  Calendar,
  Clock,
  CheckCircle2,
  FileText,
} from 'lucide-react';
import { dispatchApi, type TechnicianCandidate } from '../../api/dispatch';

interface DispatchConfirmationModalProps {
  jobId: number;
  candidate: TechnicianCandidate | null;
  isOverride: boolean;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const DispatchConfirmationModal: React.FC<DispatchConfirmationModalProps> = ({
  jobId,
  candidate,
  isOverride,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const queryClient = useQueryClient();

  const getDefaultTimes = () => {
    const now = new Date();
    return {
      start: new Date(now.getTime() + 15 * 60000).toISOString().slice(0, 16),
      end: new Date(now.getTime() + 135 * 60000).toISOString().slice(0, 16),
    };
  };

  const [startTime, setStartTime] = useState<string>(() => getDefaultTimes().start);
  const [endTime, setEndTime] = useState<string>(() => getDefaultTimes().end);
  const [overrideReason, setOverrideReason] = useState<string>('');
  const [notes, setNotes] = useState<string>('');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const confirmMutation = useMutation({
    mutationFn: async () => {
      if (!candidate) throw new Error('No candidate selected');

      // Convert local datetime-local strings to ISO Instant strings
      const startInstant = new Date(startTime).toISOString();
      const endInstant = new Date(endTime).toISOString();

      return dispatchApi.confirmDispatch({
        jobId,
        selectedTechnicianId: candidate.technicianId,
        scheduledStartTime: startInstant,
        scheduledEndTime: endInstant,
        overrideReason: isOverride ? overrideReason.trim() : undefined,
        notes: notes.trim() || undefined,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
      queryClient.invalidateQueries({ queryKey: ['sla-metrics-summary'] });
      onSuccess();
      onClose();
    },
    onError: (err: unknown) => {
      const errorObj = err as { response?: { data?: { error?: { message?: string } } } };
      const msg =
        errorObj.response?.data?.error?.message ||
        'Failed to confirm technician assignment. Please verify schedule overlap.';
      setErrorMessage(msg);
    },
  });

  if (!isOpen || !candidate) return null;

  const isOverrideInvalid = isOverride && overrideReason.trim().length < 5;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (isOverrideInvalid) {
      setErrorMessage('A detailed override reason (min 5 characters) is strictly mandatory.');
      return;
    }
    setErrorMessage(null);
    confirmMutation.mutate();
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-lg shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="p-5 border-b border-slate-800 bg-slate-950/60 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-blue-600/20 text-blue-400 border border-blue-500/30 flex items-center justify-center font-bold text-xs">
              #{jobId}
            </div>
            <div>
              <h2 className="text-sm font-bold text-slate-100">
                Confirm Technician Assignment
              </h2>
              <p className="text-[11px] text-slate-400">
                Assigning {candidate.technicianName} (Rank #{candidate.rank})
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4 text-xs">
          {/* Override Warning Banner */}
          {isOverride && (
            <div className="bg-amber-950/40 border border-amber-500/40 rounded-xl p-3.5 space-y-2">
              <div className="flex items-center gap-2 text-amber-300 font-bold text-xs">
                <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0" />
                <span>Dispatcher Manual Override Active</span>
              </div>
              <p className="text-[11px] text-amber-200/80 leading-relaxed">
                You are bypassing the top-ranked algorithm recommendation. Corporate governance requires an explicit operational justification. This will be permanently recorded in PostgreSQL <code>audit_events</code> with action <code>MANUAL_OVERRIDE</code>.
              </p>
            </div>
          )}

          {/* Candidate Summary Card */}
          <div className="bg-slate-950 p-3.5 rounded-xl border border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-slate-800 flex items-center justify-center font-bold text-slate-200 text-xs">
                {candidate.technicianName.charAt(0)}
              </div>
              <div>
                <div className="font-bold text-slate-200 text-xs">{candidate.technicianName}</div>
                <div className="text-[11px] text-slate-400">{candidate.phone}</div>
              </div>
            </div>
            <div className="text-right">
              <div className="text-[10px] uppercase text-slate-400 font-bold">Match Score</div>
              <div className="text-sm font-bold text-emerald-400 font-mono">
                {Math.round(candidate.scoreBreakdown.totalScore)} pts
              </div>
            </div>
          </div>

          {/* Schedule Window */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="block text-[11px] font-semibold text-slate-400 mb-1 flex items-center gap-1">
                <Calendar className="w-3 h-3 text-cyan-400" />
                Scheduled Start
              </label>
              <input
                type="datetime-local"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                required
                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1.5 text-slate-200 focus:outline-none focus:border-blue-500 font-mono"
              />
            </div>

            <div>
              <label className="block text-[11px] font-semibold text-slate-400 mb-1 flex items-center gap-1">
                <Clock className="w-3 h-3 text-blue-400" />
                Scheduled End
              </label>
              <input
                type="datetime-local"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                required
                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1.5 text-slate-200 focus:outline-none focus:border-blue-500 font-mono"
              />
            </div>
          </div>

          {/* Mandatory Override Reason (If Override) */}
          {isOverride && (
            <div>
              <label className="block text-[11px] font-bold text-amber-300 mb-1">
                Mandatory Override Justification *
              </label>
              <textarea
                value={overrideReason}
                onChange={(e) => setOverrideReason(e.target.value)}
                rows={2}
                placeholder="e.g., Customer explicitly requested Dave; or proximity advantage due to current traffic..."
                className="w-full bg-slate-950 border border-amber-500/40 rounded-lg p-2.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-amber-400"
              />
              <span className="text-[10px] text-slate-500">
                Minimum 5 characters required for audit compliance.
              </span>
            </div>
          )}

          {/* Dispatcher Notes */}
          <div>
            <label className="block text-[11px] font-semibold text-slate-400 mb-1 flex items-center gap-1">
              <FileText className="w-3 h-3 text-slate-400" />
              Operational Notes (Optional)
            </label>
            <input
              type="text"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="e.g., Gate code #4491, contact building superintendent upon arrival"
              className="w-full bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1.5 text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500"
            />
          </div>

          {/* Error Message Alert */}
          {errorMessage && (
            <div className="bg-rose-500/10 border border-rose-500/30 rounded-lg p-2.5 text-rose-400 text-xs flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 shrink-0" />
              <span>{errorMessage}</span>
            </div>
          )}

          {/* Actions */}
          <div className="pt-3 border-t border-slate-800 flex items-center justify-end gap-2.5">
            <button
              type="button"
              onClick={onClose}
              className="px-3.5 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold cursor-pointer"
            >
              Cancel
            </button>

            <button
              type="submit"
              disabled={confirmMutation.isPending || isOverrideInvalid}
              className={`px-4 py-2 rounded-lg font-bold flex items-center gap-1.5 transition-all cursor-pointer ${
                isOverride
                  ? 'bg-amber-500 hover:bg-amber-400 text-slate-950 shadow-lg shadow-amber-500/20'
                  : 'bg-emerald-500 hover:bg-emerald-400 text-slate-950 shadow-lg shadow-emerald-500/20'
              } disabled:opacity-40 disabled:cursor-not-allowed`}
            >
              <CheckCircle2 className={`w-4 h-4 ${confirmMutation.isPending ? 'animate-spin' : ''}`} />
              {confirmMutation.isPending
                ? 'Assigning Technician...'
                : isOverride
                ? 'Confirm Override Assignment'
                : 'Confirm Dispatch Assignment'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
