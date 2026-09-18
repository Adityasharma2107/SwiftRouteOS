import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { MapPin, Navigation } from 'lucide-react';
import { jobsApi } from '../../api/jobs';
import { SlaStatusBadge } from '../common/SlaStatusBadge';
import { PriorityBadge } from '../common/PriorityBadge';
import type { Job } from '../../types';

export const LiveMapView: React.FC = () => {
  const { data: jobs = [] } = useQuery({
    queryKey: ['jobs'],
    queryFn: () => jobsApi.getAllJobs(),
  });

  const activeJobs = jobs.filter(
    (j: Job) => j.status !== 'COMPLETED' && j.status !== 'CANCELLED'
  );

  return (
    <div className="space-y-6">
      {/* Header Info */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
            <Navigation className="w-5 h-5 text-cyan-400" />
            Live Dispatch GIS Map
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Geodesic Haversine spatial tracking for New York metro service operations.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <span className="text-xs bg-slate-800 border border-slate-700 px-3 py-1.5 rounded-lg text-slate-300 font-medium flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
            {activeJobs.length} Active Dispatch Coordinates
          </span>
        </div>
      </div>

      {/* Map Board Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left / Main Map Area */}
        <div className="lg:col-span-2 bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow-sm h-[520px] relative flex flex-col">
          {/* Simulated Dark OpenStreetMap Container */}
          <div className="flex-1 bg-slate-950 relative overflow-hidden flex items-center justify-center border-b border-slate-800">
            {/* Grid pattern */}
            <div className="absolute inset-0 opacity-15 bg-[radial-gradient(#38bdf8_1px,transparent_1px)] [background-size:16px_16px]"></div>

            {/* Metro Center Marker */}
            <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 flex flex-col items-center pointer-events-none">
              <div className="w-24 h-24 rounded-full bg-blue-500/10 border border-blue-500/30 flex items-center justify-center animate-ping"></div>
              <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 text-center">
                <div className="w-4 h-4 rounded-full bg-blue-500 border-2 border-white shadow-lg mx-auto"></div>
                <span className="text-[10px] font-bold text-slate-400 bg-slate-900/90 px-1.5 py-0.5 rounded mt-1 inline-block border border-slate-800">
                  NYC Dispatch HQ
                </span>
              </div>
            </div>

            {/* Active Job Marker Pins */}
            {activeJobs.slice(0, 8).map((job, idx) => {
              // Deterministic spatial offsets for visual distribution
              const topOffset = 25 + (idx * 14) % 65;
              const leftOffset = 20 + (idx * 23) % 70;
              const isBreached = job.slaStatus === 'BREACHED';
              const isNearing = job.slaStatus === 'NEARING_BREACH';

              return (
                <div
                  key={job.id}
                  style={{ top: `${topOffset}%`, left: `${leftOffset}%` }}
                  className="absolute -translate-x-1/2 -translate-y-1/2 group cursor-pointer"
                >
                  <div className="relative">
                    <span
                      className={`w-3.5 h-3.5 rounded-full flex items-center justify-center shadow-lg ${
                        isBreached
                          ? 'bg-rose-500 animate-pulse-fast ring-4 ring-rose-500/30'
                          : isNearing
                          ? 'bg-amber-400 animate-pulse ring-4 ring-amber-400/30'
                          : 'bg-cyan-500 ring-2 ring-cyan-500/30'
                      }`}
                    >
                      <MapPin className="w-2.5 h-2.5 text-slate-950" />
                    </span>
                    {/* Hover Tooltip */}
                    <div className="hidden group-hover:block absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-48 bg-slate-900 border border-slate-700 rounded-lg p-2 text-xs shadow-2xl z-30 pointer-events-none">
                      <div className="font-bold text-slate-200">
                        Job #{job.id} ({job.priority})
                      </div>
                      <div className="text-[11px] text-slate-400 truncate">
                        {job.serviceRequest.customerName}
                      </div>
                      <div className="text-[10px] text-slate-500 truncate">
                        {job.serviceRequest.serviceAddress}
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}

            {/* Legend Overlay */}
            <div className="absolute bottom-3 left-3 bg-slate-900/90 backdrop-blur border border-slate-800 rounded-lg p-2.5 text-[11px] space-y-1.5 z-20">
              <div className="font-bold text-slate-300 text-[10px] uppercase tracking-wider">
                Map Pin Legend
              </div>
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-cyan-400"></span>
                <span className="text-slate-400">Healthy Job SLA</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-amber-400 animate-pulse"></span>
                <span className="text-slate-400">Nearing Breach (&ge;70%)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-rose-500 animate-pulse-fast"></span>
                <span className="text-slate-400">Breached SLA</span>
              </div>
            </div>
          </div>

          <div className="p-3 bg-slate-900 text-xs text-slate-400 flex items-center justify-between">
            <span>Projection: WGS 84 (Spherical Haversine Distance)</span>
            <span className="text-cyan-400 font-mono text-[11px]">Lat: 40.7128° N, Lon: 74.0060° W</span>
          </div>
        </div>

        {/* Right Active Jobs List */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow-sm flex flex-col h-[520px]">
          <div className="font-bold text-sm text-slate-200 pb-3 border-b border-slate-800 flex items-center justify-between">
            <span>Dispatched Queue</span>
            <span className="text-xs font-mono text-slate-400">{activeJobs.length} jobs</span>
          </div>

          <div className="flex-1 overflow-y-auto divide-y divide-slate-800/60 mt-2 space-y-1 pr-1">
            {activeJobs.map((job) => (
              <div
                key={job.id}
                className="p-2.5 rounded-lg hover:bg-slate-800/50 transition-colors space-y-1.5"
              >
                <div className="flex items-center justify-between">
                  <span className="font-mono font-bold text-xs text-cyan-400">#{job.id}</span>
                  <SlaStatusBadge status={job.slaStatus} />
                </div>
                <div className="text-xs font-semibold text-slate-200">
                  {job.serviceRequest.customerName}
                </div>
                <div className="text-[11px] text-slate-400 truncate">
                  {job.serviceRequest.serviceAddress}
                </div>
                <div className="flex items-center justify-between pt-1 text-[11px]">
                  <PriorityBadge priority={job.priority} />
                  <span className="text-slate-400 font-mono text-[10px]">
                    {job.serviceRequest.requiredSkill?.name}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
