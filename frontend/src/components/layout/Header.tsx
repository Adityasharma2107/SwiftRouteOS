import React, { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Compass,
  ShieldCheck,
  AlertTriangle,
  Flame,
  RefreshCw,
  ChevronDown,
  UserCheck,
  LogOut,
  Sparkles,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { slaApi } from '../../api/sla';

export const Header: React.FC = () => {
  const { user, activePersona, demoPersonas, switchPersona, logout } = useAuth();
  const [isPersonaMenuOpen, setIsPersonaMenuOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const queryClient = useQueryClient();

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsPersonaMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Fetch real-time SLA metrics summary
  const { data: slaMetrics, isLoading: isSlaLoading, refetch: refetchSla } = useQuery({
    queryKey: ['sla-metrics-summary'],
    queryFn: slaApi.getMetricsSummary,
    refetchInterval: 15000, // Poll every 15 seconds
  });

  // Sweep SLA mutation
  const sweepMutation = useMutation({
    mutationFn: slaApi.triggerBatchEvaluation,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sla-metrics-summary'] });
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
      refetchSla();
    },
  });

  const complianceRate = slaMetrics?.complianceRatePercent ?? 100;
  const isHealthyRate = complianceRate >= 85;
  const isWarningRate = complianceRate < 85 && complianceRate >= 70;

  return (
    <header className="h-16 bg-slate-900 border-b border-slate-800 px-6 flex items-center justify-between sticky top-0 z-40">
      {/* Brand & Platform Identity */}
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-cyan-600 via-blue-600 to-indigo-600 flex items-center justify-center shadow-lg shadow-blue-500/20 ring-1 ring-white/10">
          <Compass className="w-6 h-6 text-white animate-spin-slow" />
        </div>
        <div>
          <div className="flex items-center gap-2">
            <span className="font-black tracking-tight text-lg text-slate-100">
              SwiftRoute<span className="text-cyan-400">OS</span>
            </span>
            <span className="px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-wider rounded bg-cyan-950/80 text-cyan-400 border border-cyan-800/40">
              v2.0
            </span>
          </div>
          <p className="text-[11px] text-slate-400 -mt-0.5 hidden sm:block">
            Intelligent SLA-Aware Dispatch & Concurrency Engine
          </p>
        </div>
      </div>

      {/* SLA Live Health Banner */}
      <div className="hidden md:flex items-center gap-3 bg-slate-950/60 border border-slate-800/80 rounded-lg px-3.5 py-1.5 shadow-inner">
        <div className="flex items-center gap-2">
          {isHealthyRate ? (
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
          ) : isWarningRate ? (
            <AlertTriangle className="w-4 h-4 text-amber-400 animate-pulse" />
          ) : (
            <Flame className="w-4 h-4 text-rose-500 animate-pulse-fast" />
          )}
          <span className="text-xs font-semibold text-slate-300">SLA Compliance:</span>
          <span
            className={`text-xs font-black px-2 py-0.5 rounded ${
              isHealthyRate
                ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                : isWarningRate
                ? 'bg-amber-500/15 text-amber-300 border border-amber-500/30'
                : 'bg-rose-500/20 text-rose-400 border border-rose-500/40'
            }`}
          >
            {isSlaLoading ? '...' : `${complianceRate.toFixed(1)}%`}
          </span>
        </div>

        <div className="h-4 w-px bg-slate-800" />

        {/* Micro Counters */}
        <div className="flex items-center gap-2.5 text-[11px]">
          <span className="text-emerald-400 font-medium">
            {slaMetrics?.healthyJobs ?? 0} Healthy
          </span>
          <span className="text-amber-400 font-medium">
            {slaMetrics?.nearingBreachJobs ?? 0} At Risk
          </span>
          <span className="text-rose-400 font-medium">
            {slaMetrics?.breachedJobs ?? 0} Breached
          </span>
        </div>

        {/* Manual SLA Trigger */}
        {(user?.role === 'ROLE_ADMIN' || user?.role === 'ROLE_DISPATCHER') && (
          <button
            onClick={() => sweepMutation.mutate()}
            disabled={sweepMutation.isPending}
            title="Trigger Immediate SLA Evaluation Sweep"
            className="ml-1 p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-cyan-400 transition-colors cursor-pointer"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${sweepMutation.isPending ? 'animate-spin text-cyan-400' : ''}`} />
          </button>
        )}
      </div>

      {/* Role Switcher & Persona Menu */}
      <div className="relative" ref={dropdownRef}>
        <button
          onClick={() => setIsPersonaMenuOpen(!isPersonaMenuOpen)}
          className="flex items-center gap-2.5 bg-slate-800/80 hover:bg-slate-800 border border-slate-700/60 rounded-xl px-3 py-1.5 transition-all text-left cursor-pointer"
        >
          <div
            className={`w-7 h-7 rounded-lg ${
              activePersona ? activePersona.avatarColor : 'bg-slate-700'
            } flex items-center justify-center text-white text-xs font-bold shadow-sm`}
          >
            {user?.fullName?.charAt(0) || 'U'}
          </div>
          <div className="hidden sm:block">
            <div className="text-xs font-bold text-slate-200 leading-tight flex items-center gap-1.5">
              {activePersona ? activePersona.displayName : user?.fullName || 'User'}
              <span className="text-[9px] px-1.5 py-0.2 rounded font-semibold bg-slate-700 text-slate-300">
                {user?.role?.replace('ROLE_', '') || 'ROLE'}
              </span>
            </div>
            <div className="text-[10px] text-slate-400 leading-tight">
              {activePersona ? activePersona.title : user?.email || 'Logged in'}
            </div>
          </div>
          <ChevronDown className="w-3.5 h-3.5 text-slate-400 ml-1" />
        </button>

        {/* Dropdown Menu */}
        {isPersonaMenuOpen && (
          <div className="absolute right-0 mt-2 w-72 bg-slate-900 border border-slate-700/80 rounded-xl shadow-2xl py-2 z-50 animate-in fade-in slide-in-from-top-1 duration-150">
            <div className="px-3.5 py-2 border-b border-slate-800">
              <div className="flex items-center justify-between text-[11px] font-semibold text-slate-400">
                <span className="flex items-center gap-1">
                  <Sparkles className="w-3.5 h-3.5 text-amber-400" />
                  Instant Persona Switcher
                </span>
                <span className="text-[10px] bg-slate-800 text-slate-400 px-1.5 py-0.5 rounded">Demo Mode</span>
              </div>
              <p className="text-[10px] text-slate-500 mt-0.5">
                Switch operational role in 1-click to test RBAC & workflows.
              </p>
            </div>

            <div className="p-1 space-y-1">
              {demoPersonas.map((persona) => {
                const isActive = user?.username === persona.username;
                return (
                  <button
                    key={persona.key}
                    onClick={async () => {
                      await switchPersona(persona.key);
                      setIsPersonaMenuOpen(false);
                    }}
                    className={`w-full flex items-center justify-between p-2 rounded-lg text-left transition-colors cursor-pointer ${
                      isActive
                        ? 'bg-blue-600/15 border border-blue-500/30'
                        : 'hover:bg-slate-800/70'
                    }`}
                  >
                    <div className="flex items-center gap-2.5">
                      <div
                        className={`w-7 h-7 rounded-md ${persona.avatarColor} flex items-center justify-center text-white text-xs font-bold`}
                      >
                        {persona.displayName.charAt(0)}
                      </div>
                      <div>
                        <div className="text-xs font-semibold text-slate-200 flex items-center gap-1.5">
                          {persona.displayName}
                          {isActive && <UserCheck className="w-3.5 h-3.5 text-blue-400" />}
                        </div>
                        <div className="text-[10px] text-slate-400">{persona.title}</div>
                      </div>
                    </div>
                    <span className="text-[9px] px-1.5 py-0.5 rounded font-mono font-medium bg-slate-800 text-slate-300 border border-slate-700">
                      {persona.role.replace('ROLE_', '')}
                    </span>
                  </button>
                );
              })}
            </div>

            <div className="mt-1 pt-1 border-t border-slate-800 px-1">
              <button
                onClick={() => {
                  logout();
                  setIsPersonaMenuOpen(false);
                }}
                className="w-full flex items-center gap-2 px-3 py-2 text-xs text-rose-400 hover:bg-rose-500/10 rounded-lg transition-colors cursor-pointer"
              >
                <LogOut className="w-3.5 h-3.5" />
                Sign Out
              </button>
            </div>
          </div>
        )}
      </div>
    </header>
  );
};
