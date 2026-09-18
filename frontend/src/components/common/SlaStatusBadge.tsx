import React from 'react';
import type { SlaStatus } from '../../types';

interface SlaStatusBadgeProps {
  status: SlaStatus;
  showPulse?: boolean;
  className?: string;
}

export const SlaStatusBadge: React.FC<SlaStatusBadgeProps> = ({
  status,
  showPulse = true,
  className = '',
}) => {
  switch (status) {
    case 'HEALTHY':
      return (
        <span
          className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 ${className}`}
        >
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
          Healthy
        </span>
      );
    case 'NEARING_BREACH':
      return (
        <span
          className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-500/15 text-amber-400 border border-amber-500/30 ${
            showPulse ? 'animate-pulse' : ''
          } ${className}`}
        >
          <span className="w-1.5 h-1.5 rounded-full bg-amber-400"></span>
          Nearing Breach
        </span>
      );
    case 'BREACHED':
      return (
        <span
          className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-bold bg-rose-500/20 text-rose-400 border border-rose-500/40 ${
            showPulse ? 'animate-pulse-fast' : ''
          } ${className}`}
        >
          <span className="w-1.5 h-1.5 rounded-full bg-rose-500"></span>
          Breached
        </span>
      );
    default:
      return (
        <span
          className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-slate-800 text-slate-400 border border-slate-700 ${className}`}
        >
          {status}
        </span>
      );
  }
};
