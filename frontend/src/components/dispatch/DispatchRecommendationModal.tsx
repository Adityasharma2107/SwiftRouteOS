import React from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  X,
  Sparkles,
  Award,
  Navigation,
  Briefcase,
  Clock,
  AlertTriangle,
  ChevronRight,
  ShieldCheck,
  RefreshCw,
} from 'lucide-react';
import { dispatchApi, type TechnicianCandidate } from '../../api/dispatch';

interface DispatchRecommendationModalProps {
  jobId: number;
  customerName: string;
  serviceAddress: string;
  requiredSkillName: string;
  priority: string;
  isOpen: boolean;
  onClose: () => void;
  onSelectCandidate: (candidate: TechnicianCandidate, isOverride: boolean) => void;
}

export const DispatchRecommendationModal: React.FC<DispatchRecommendationModalProps> = ({
  jobId,
  customerName,
  serviceAddress,
  requiredSkillName,
  priority,
  isOpen,
  onClose,
  onSelectCandidate,
}) => {
  const {
    data: recommendation,
    isLoading,
    isError,
    refetch,
  } = useQuery({
    queryKey: ['dispatch-recommendations', jobId],
    queryFn: () => dispatchApi.getRecommendations(jobId),
    enabled: isOpen && !!jobId,
  });

  if (!isOpen) return null;

  const candidates = recommendation?.candidates || [];
  const recommended = recommendation?.recommendedCandidate;

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-slate-700/80 rounded-2xl w-full max-w-4xl max-h-[90vh] flex flex-col shadow-2xl overflow-hidden">
        {/* Modal Header */}
        <div className="p-5 border-b border-slate-800 bg-slate-950/60 flex items-start justify-between">
          <div>
            <div className="flex items-center gap-2">
              <span className="font-mono font-bold text-sm text-cyan-400">Job #{jobId}</span>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-rose-500/20 text-rose-400 border border-rose-500/30 uppercase">
                {priority}
              </span>
              <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-slate-800 text-slate-300 border border-slate-700">
                {requiredSkillName}
              </span>
            </div>
            <h2 className="text-lg font-bold text-slate-100 mt-1">{customerName}</h2>
            <p className="text-xs text-slate-400">{serviceAddress}</p>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {isLoading ? (
            <div className="py-20 flex flex-col items-center justify-center text-center gap-3">
              <RefreshCw className="w-8 h-8 animate-spin text-cyan-400" />
              <div className="text-sm font-semibold text-slate-200">
                Running Multi-Factor Haversine Scoring...
              </div>
              <p className="text-xs text-slate-400 max-w-sm">
                Evaluating geodesic distance, technician skill proficiencies, active workloads, and shift windows.
              </p>
            </div>
          ) : isError ? (
            <div className="py-16 text-center text-xs text-rose-400 space-y-2">
              <AlertTriangle className="w-8 h-8 mx-auto text-rose-500" />
              <div className="font-bold text-sm">Failed to generate dispatch recommendations</div>
              <p className="text-slate-400">Ensure the backend dispatch service is active.</p>
              <button
                onClick={() => refetch()}
                className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-lg font-semibold mt-2 cursor-pointer"
              >
                Retry Calculation
              </button>
            </div>
          ) : candidates.length === 0 ? (
            <div className="py-16 text-center text-xs text-slate-400 space-y-2">
              <AlertTriangle className="w-8 h-8 mx-auto text-amber-500" />
              <div className="font-bold text-sm text-slate-300">No Eligible Technicians Available</div>
              <p className="max-w-md mx-auto text-slate-500">
                All technicians are either off duty, currently exceed daily job capacity, or lack the required certified skill for this job.
              </p>
            </div>
          ) : (
            <>
              {/* Algorithm Notice */}
              <div className="bg-blue-950/30 border border-blue-800/40 rounded-xl p-3.5 flex items-center justify-between text-xs text-blue-300">
                <div className="flex items-center gap-2.5">
                  <Sparkles className="w-4 h-4 text-cyan-400 shrink-0" />
                  <span>
                    <strong>Explainable Multi-Factor Scoring:</strong> Candidates ranked based on Skill Match (35%), Workload Balance (30%), Proximity (20%), and Shift Duration (15%).
                  </span>
                </div>
                <span className="font-mono text-[11px] bg-blue-900/60 px-2 py-0.5 rounded border border-blue-700/50">
                  {candidates.length} Scored
                </span>
              </div>

              {/* Top Recommended Match Banner */}
              {recommended && (
                <div className="bg-gradient-to-r from-emerald-950/40 via-slate-900 to-cyan-950/30 border-2 border-emerald-500/50 rounded-2xl p-5 shadow-xl relative overflow-hidden">
                  <div className="absolute -top-3 -right-3 w-24 h-24 bg-emerald-500/10 rounded-full blur-xl pointer-events-none"></div>

                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-xl bg-emerald-600 flex items-center justify-center text-white text-lg font-bold shadow-lg shadow-emerald-600/30">
                        {recommended.technicianName.charAt(0)}
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="px-2 py-0.5 text-[10px] font-black uppercase tracking-wider rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center gap-1">
                            <Award className="w-3 h-3" />
                            Rank #1 Recommended Match
                          </span>
                          <span className="text-xs font-mono text-slate-400">
                            {recommended.phone}
                          </span>
                        </div>
                        <h3 className="text-base font-black text-slate-100 mt-0.5">
                          {recommended.technicianName}
                        </h3>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      <div className="text-right">
                        <div className="text-[10px] uppercase font-bold text-slate-400">
                          Composite Score
                        </div>
                        <div className="text-2xl font-black text-emerald-400 font-mono">
                          {Math.round(recommended.scoreBreakdown.totalScore)}
                          <span className="text-xs text-slate-500 font-normal">/100</span>
                        </div>
                      </div>

                      <button
                        onClick={() => onSelectCandidate(recommended, false)}
                        className="px-4 py-2 bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs rounded-xl shadow-lg shadow-emerald-500/20 flex items-center gap-1.5 transition-all cursor-pointer"
                      >
                        <ShieldCheck className="w-4 h-4" />
                        Assign Recommended
                      </button>
                    </div>
                  </div>

                  {/* Factor Breakdown Grid */}
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 mt-4">
                    <div className="bg-slate-950/60 p-2.5 rounded-lg border border-slate-800">
                      <div className="text-[10px] text-slate-400 uppercase font-semibold">
                        Skill Proficiency
                      </div>
                      <div className="text-xs font-bold text-slate-200 mt-0.5 flex items-center gap-1">
                        <Award className="w-3 h-3 text-cyan-400" />
                        Level {recommended.proficiencyLevel}/5
                      </div>
                      <div className="text-[10px] text-emerald-400 truncate mt-0.5">
                        {recommended.scoreBreakdown.skillMatchExplanation}
                      </div>
                    </div>

                    <div className="bg-slate-950/60 p-2.5 rounded-lg border border-slate-800">
                      <div className="text-[10px] text-slate-400 uppercase font-semibold">
                        Haversine Distance
                      </div>
                      <div className="text-xs font-bold text-slate-200 mt-0.5 flex items-center gap-1 font-mono">
                        <Navigation className="w-3 h-3 text-blue-400" />
                        {recommended.distanceKm.toFixed(1)} km
                      </div>
                      <div className="text-[10px] text-slate-400 truncate mt-0.5">
                        {recommended.scoreBreakdown.distanceExplanation}
                      </div>
                    </div>

                    <div className="bg-slate-950/60 p-2.5 rounded-lg border border-slate-800">
                      <div className="text-[10px] text-slate-400 uppercase font-semibold">
                        Current Workload
                      </div>
                      <div className="text-xs font-bold text-slate-200 mt-0.5 flex items-center gap-1">
                        <Briefcase className="w-3 h-3 text-amber-400" />
                        {recommended.activeJobsToday} / {recommended.maxDailyJobs} jobs
                      </div>
                      <div className="text-[10px] text-slate-400 truncate mt-0.5">
                        {recommended.scoreBreakdown.workloadExplanation}
                      </div>
                    </div>

                    <div className="bg-slate-950/60 p-2.5 rounded-lg border border-slate-800">
                      <div className="text-[10px] text-slate-400 uppercase font-semibold">
                        Shift Availability
                      </div>
                      <div className="text-xs font-bold text-slate-200 mt-0.5 flex items-center gap-1">
                        <Clock className="w-3 h-3 text-purple-400" />
                        Active On Duty
                      </div>
                      <div className="text-[10px] text-slate-400 truncate mt-0.5">
                        {recommended.scoreBreakdown.shiftExplanation}
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* Other Eligible Candidates */}
              <div className="space-y-3 pt-2">
                <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                  All Eligible Candidates (Ranked)
                </div>

                <div className="space-y-2">
                  {candidates.map((candidate) => {
                    const isTop = candidate.technicianId === recommended?.technicianId;
                    return (
                      <div
                        key={candidate.technicianId}
                        className={`p-4 rounded-xl border transition-all flex flex-col md:flex-row md:items-center justify-between gap-3 ${
                          isTop
                            ? 'bg-slate-900/90 border-emerald-500/30'
                            : 'bg-slate-900 border-slate-800 hover:border-slate-700'
                        }`}
                      >
                        <div className="flex items-center gap-3">
                          <div
                            className={`w-9 h-9 rounded-lg flex items-center justify-center font-bold text-xs ${
                              isTop
                                ? 'bg-emerald-600 text-white'
                                : 'bg-slate-800 text-slate-300'
                            }`}
                          >
                            #{candidate.rank}
                          </div>
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="font-bold text-sm text-slate-200">
                                {candidate.technicianName}
                              </span>
                              {isTop && (
                                <span className="text-[9px] font-bold px-1.5 py-0.2 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                                  Top Pick
                                </span>
                              )}
                              <span className="text-xs text-slate-400 font-mono">
                                {candidate.phone}
                              </span>
                            </div>
                            <div className="flex items-center gap-3 text-xs text-slate-400 mt-0.5">
                              <span>Skill: L{candidate.proficiencyLevel}</span>
                              <span>•</span>
                              <span>Distance: {candidate.distanceKm.toFixed(1)} km</span>
                              <span>•</span>
                              <span>
                                Active: {candidate.activeJobsToday}/{candidate.maxDailyJobs}
                              </span>
                            </div>
                          </div>
                        </div>

                        <div className="flex items-center justify-between md:justify-end gap-4 pt-2 md:pt-0 border-t md:border-t-0 border-slate-800">
                          <div className="text-right">
                            <span className="text-xs font-mono font-bold text-slate-200">
                              {Math.round(candidate.scoreBreakdown.totalScore)}
                            </span>
                            <span className="text-[10px] text-slate-400"> pts</span>
                          </div>

                          <button
                            onClick={() => onSelectCandidate(candidate, !isTop)}
                            className={`px-3 py-1.5 text-xs font-semibold rounded-lg flex items-center gap-1 transition-colors cursor-pointer ${
                              isTop
                                ? 'bg-emerald-600 hover:bg-emerald-500 text-white'
                                : 'bg-slate-800 hover:bg-slate-700 text-amber-300 border border-amber-500/20'
                            }`}
                          >
                            {isTop ? 'Assign Top Match' : 'Override & Select'}
                            <ChevronRight className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
