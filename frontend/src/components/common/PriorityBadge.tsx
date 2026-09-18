import React from 'react';
import type { Priority } from '../../types';

interface PriorityBadgeProps {
  priority: Priority;
  className?: string;
}

export const PriorityBadge: React.FC<PriorityBadgeProps> = ({ priority, className = '' }) => {
  switch (priority) {
    case 'CRITICAL':
      return (
        <span
          className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-bold bg-rose-500/15 text-rose-400 border border-rose-500/30 ${className}`}
        >
          CRITICAL
        </span>
      );
    case 'HIGH':
      return (
        <span
          className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-orange-500/15 text-orange-400 border border-orange-500/30 ${className}`}
        >
          HIGH
        </span>
      );
    case 'MEDIUM':
      return (
        <span
          className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-amber-500/10 text-amber-400 border border-amber-500/20 ${className}`}
        >
          MEDIUM
        </span>
      );
    case 'LOW':
      return (
        <span
          className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-slate-500/15 text-slate-300 border border-slate-600/30 ${className}`}
        >
          LOW
        </span>
      );
    default:
      return (
        <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs text-slate-400 ${className}`}>
          {priority}
        </span>
      );
  }
};
