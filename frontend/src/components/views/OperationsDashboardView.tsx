import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Search,
  Filter,
  RefreshCw,
  Clock,
  Wrench,
  AlertCircle,
  ExternalLink,
  ChevronRight,
} from 'lucide-react';
import { jobsApi } from '../../api/jobs';
import { slaApi } from '../../api/sla';
import { SlaStatusBadge } from '../common/SlaStatusBadge';
import { PriorityBadge } from '../common/PriorityBadge';
import { JobStatusBadge } from '../common/JobStatusBadge';
import { DispatchRecommendationModal } from '../dispatch/DispatchRecommendationModal';
import { DispatchConfirmationModal } from '../dispatch/DispatchConfirmationModal';
import { JobDetailDrawer } from '../dispatch/JobDetailDrawer';
import type { JobStatus, Priority, Job } from '../../types';
import type { TechnicianCandidate } from '../../api/dispatch';

export const OperationsDashboardView: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<JobStatus | 'ALL'>('ALL');
  const [priorityFilter, setPriorityFilter] = useState<Priority | 'ALL'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Modal / Drawer state
  const [selectedJobIdForDetail, setSelectedJobIdForDetail] = useState<number | null>(null);
  const [dispatchJobTarget, setDispatchJobTarget] = useState<Job | null>(null);
  const [confirmationTarget, setConfirmationTarget] = useState<{
    jobId: number;
    candidate: TechnicianCandidate;
    isOverride: boolean;
  } | null>(null);

  // Fetch jobs
  const {
    data: jobs = [],
    isLoading: isJobsLoading,
    refetch: refetchJobs,
  } = useQuery({
    queryKey: ['jobs', statusFilter, priorityFilter],
    queryFn: () =>
      jobsApi.getAllJobs({
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        priority: priorityFilter === 'ALL' ? undefined : priorityFilter,
      }),
    refetchInterval: 10000,
  });

  // Fetch SLA summary
  const { data: slaMetrics } = useQuery({
    queryKey: ['sla-metrics-summary'],
    queryFn: slaApi.getMetricsSummary,
  });

  // Filter jobs by search query
  const filteredJobs = jobs.filter((job: Job) => {
    if (!searchQuery) return true;
    const query = searchQuery.toLowerCase();
    return (
      job.id.toString().includes(query) ||
      job.serviceRequest.customerName.toLowerCase().includes(query) ||
      job.serviceRequest.serviceAddress.toLowerCase().includes(query) ||
      job.serviceRequest.equipmentType.toLowerCase().includes(query)
    );
  });

  const handleOpenDispatchForJob = (jobId: number) => {
    const target = jobs.find((j: Job) => j.id === jobId);
    if (target) {
      setDispatchJobTarget(target);
    }
  };

  const handleCandidateSelected = (candidate: TechnicianCandidate, isOverride: boolean) => {
    if (!dispatchJobTarget) return;
    const jobId = dispatchJobTarget.id;
    setDispatchJobTarget(null);
    setConfirmationTarget({
      jobId,
      candidate,
      isOverride,
    });
  };

  return (
    <div className="space-y-6">
      {/* Metric Cards Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow-sm">
          <div className="text-xs font-semibold text-slate-400">Total Monitored Jobs</div>
          <div className="text-2xl font-black text-slate-100 mt-1">
            {slaMetrics?.totalMonitoredJobs ?? jobs.length}
          </div>
          <div className="text-[11px] text-slate-500 mt-1">Active lifecycle operational jobs</div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow-sm">
          <div className="text-xs font-semibold text-emerald-400 flex items-center justify-between">
            <span>Healthy SLA</span>
            <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
          </div>
          <div className="text-2xl font-black text-emerald-300 mt-1">
            {slaMetrics?.healthyJobs ?? 0}
          </div>
          <div className="text-[11px] text-slate-500 mt-1">&lt; 70% deadline consumed</div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow-sm">
          <div className="text-xs font-semibold text-amber-400 flex items-center justify-between">
            <span>Nearing Breach</span>
            <span className="w-2 h-2 rounded-full bg-amber-400 animate-pulse"></span>
          </div>
          <div className="text-2xl font-black text-amber-300 mt-1">
            {slaMetrics?.nearingBreachJobs ?? 0}
          </div>
          <div className="text-[11px] text-slate-500 mt-1">70% - 99% deadline consumed</div>
        </div>

        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow-sm">
          <div className="text-xs font-semibold text-rose-400 flex items-center justify-between">
            <span>SLA Breached</span>
            <span className="w-2 h-2 rounded-full bg-rose-500 animate-pulse-fast"></span>
          </div>
          <div className="text-2xl font-black text-rose-300 mt-1">
            {slaMetrics?.breachedJobs ?? 0}
          </div>
          <div className="text-[11px] text-slate-500 mt-1">&ge; 100% resolution exceeded</div>
        </div>
      </div>

      {/* Action Bar & Filters */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 flex flex-col md:flex-row gap-4 items-center justify-between shadow-sm">
        {/* Search */}
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search by ID, customer, address..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-slate-950 border border-slate-800 rounded-lg pl-9 pr-4 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500"
          />
        </div>

        {/* Filter Dropdowns */}
        <div className="flex flex-wrap items-center gap-2.5 w-full md:w-auto">
          <div className="flex items-center gap-1.5 text-xs text-slate-400">
            <Filter className="w-3.5 h-3.5" />
            <span>Filter:</span>
          </div>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as JobStatus | 'ALL')}
            className="bg-slate-950 border border-slate-800 text-xs rounded-lg px-2.5 py-1.5 text-slate-200 focus:outline-none focus:border-blue-500 cursor-pointer"
          >
            <option value="ALL">All Statuses</option>
            <option value="PENDING">PENDING</option>
            <option value="ASSIGNED">ASSIGNED</option>
            <option value="EN_ROUTE">EN_ROUTE</option>
            <option value="IN_PROGRESS">IN_PROGRESS</option>
            <option value="COMPLETED">COMPLETED</option>
            <option value="CANCELLED">CANCELLED</option>
          </select>

          <select
            value={priorityFilter}
            onChange={(e) => setPriorityFilter(e.target.value as Priority | 'ALL')}
            className="bg-slate-950 border border-slate-800 text-xs rounded-lg px-2.5 py-1.5 text-slate-200 focus:outline-none focus:border-blue-500 cursor-pointer"
          >
            <option value="ALL">All Priorities</option>
            <option value="CRITICAL">CRITICAL</option>
            <option value="HIGH">HIGH</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="LOW">LOW</option>
          </select>

          <button
            onClick={() => refetchJobs()}
            className="p-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs flex items-center gap-1 transition-colors cursor-pointer"
            title="Refresh jobs"
          >
            <RefreshCw className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Jobs Table */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
        <div className="p-4 border-b border-slate-800 flex items-center justify-between">
          <div className="text-sm font-bold text-slate-200">Operational Jobs Roster</div>
          <div className="text-xs text-slate-400">
            Showing <span className="font-semibold text-slate-200">{filteredJobs.length}</span> jobs
          </div>
        </div>

        {isJobsLoading ? (
          <div className="p-12 text-center text-xs text-slate-400 flex flex-col items-center justify-center gap-2">
            <RefreshCw className="w-5 h-5 animate-spin text-blue-500" />
            Loading operational jobs from database...
          </div>
        ) : filteredJobs.length === 0 ? (
          <div className="p-12 text-center text-xs text-slate-500 flex flex-col items-center justify-center gap-2">
            <AlertCircle className="w-6 h-6 text-slate-600" />
            No jobs found matching the selected filters.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-300">
              <thead className="bg-slate-950/60 text-[11px] font-semibold text-slate-400 uppercase tracking-wider border-b border-slate-800">
                <tr>
                  <th className="px-4 py-3">Job ID</th>
                  <th className="px-4 py-3">Customer & Location</th>
                  <th className="px-4 py-3">Priority</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">SLA Health</th>
                  <th className="px-4 py-3">Required Skill</th>
                  <th className="px-4 py-3">Resolution Deadline</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {filteredJobs.map((job) => (
                  <tr
                    key={job.id}
                    onClick={() => setSelectedJobIdForDetail(job.id)}
                    className="hover:bg-slate-800/40 transition-colors cursor-pointer"
                  >
                    <td className="px-4 py-3 font-mono font-bold text-cyan-400">
                      #{job.id}
                    </td>
                    <td className="px-4 py-3">
                      <div className="font-semibold text-slate-200">
                        {job.serviceRequest.customerName}
                      </div>
                      <div className="text-[11px] text-slate-400 truncate max-w-xs">
                        {job.serviceRequest.serviceAddress}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <PriorityBadge priority={job.priority} />
                    </td>
                    <td className="px-4 py-3">
                      <JobStatusBadge status={job.status} />
                    </td>
                    <td className="px-4 py-3">
                      <SlaStatusBadge status={job.slaStatus} />
                    </td>
                    <td className="px-4 py-3">
                      <span className="inline-flex items-center gap-1 text-[11px] font-medium bg-slate-800 text-slate-300 px-2 py-0.5 rounded border border-slate-700">
                        <Wrench className="w-3 h-3 text-slate-400" />
                        {job.serviceRequest.requiredSkill?.name || 'General'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-[11px] text-slate-400">
                      <div className="flex items-center gap-1 font-mono">
                        <Clock className="w-3 h-3 text-slate-500" />
                        {new Date(job.slaResolutionDeadline).toLocaleTimeString([], {
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </div>
                    </td>
                    <td className="px-4 py-3 text-right" onClick={(e) => e.stopPropagation()}>
                      <div className="flex items-center justify-end gap-2">
                        {job.status === 'PENDING' && (
                          <button
                            onClick={() => setDispatchJobTarget(job)}
                            className="px-2.5 py-1 rounded bg-blue-600 hover:bg-blue-500 text-white font-semibold text-[11px] flex items-center gap-1 shadow-sm cursor-pointer"
                          >
                            Dispatch <ChevronRight className="w-3 h-3" />
                          </button>
                        )}
                        <button
                          onClick={() => setSelectedJobIdForDetail(job.id)}
                          className="p-1 rounded text-slate-400 hover:text-slate-200 hover:bg-slate-800 cursor-pointer"
                          title="View Job Details"
                        >
                          <ExternalLink className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* 1. Dispatch Recommendation Modal */}
      {dispatchJobTarget && (
        <DispatchRecommendationModal
          jobId={dispatchJobTarget.id}
          customerName={dispatchJobTarget.serviceRequest.customerName}
          serviceAddress={dispatchJobTarget.serviceRequest.serviceAddress}
          requiredSkillName={dispatchJobTarget.serviceRequest.requiredSkill?.name || 'General'}
          priority={dispatchJobTarget.priority}
          isOpen={!!dispatchJobTarget}
          onClose={() => setDispatchJobTarget(null)}
          onSelectCandidate={handleCandidateSelected}
        />
      )}

      {/* 2. Dispatch Confirmation & Override Modal */}
      {confirmationTarget && (
        <DispatchConfirmationModal
          jobId={confirmationTarget.jobId}
          candidate={confirmationTarget.candidate}
          isOverride={confirmationTarget.isOverride}
          isOpen={!!confirmationTarget}
          onClose={() => setConfirmationTarget(null)}
          onSuccess={() => refetchJobs()}
        />
      )}

      {/* 3. Job Detail & Audit Drawer */}
      <JobDetailDrawer
        jobId={selectedJobIdForDetail}
        isOpen={selectedJobIdForDetail !== null}
        onClose={() => setSelectedJobIdForDetail(null)}
        onOpenDispatchModal={handleOpenDispatchForJob}
      />
    </div>
  );
};
