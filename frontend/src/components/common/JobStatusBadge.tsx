import React from 'react';
import type { JobStatus } from '../../types';

interface JobStatusBadgeProps {
  status: JobStatus;
  className?: string;
}

export const JobStatusBadge: React.FC<JobStatusBadgeProps> = ({ status, className = '' }) => {
  switch (status) {
    case 'PENDING':
      return (
        <span
          className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-amber-500/10 text-amber-300 border border-amber-500/20 ${className}`}
        >
          PENDING
        </span>
      );
    case 'ASSIGNED':
      return (
        <span
          className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-sky-500/10 text-sky-300 border border-sky-500/20 ${className}`}
        >
          ASSIGNED
        </span>
      );
    case 'EN_ROUTE':
      return (
        <span
          className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-indigo-500/10 text-indigo-300 border border-indigo-500/20 ${className}`}
        >
          EN ROUTE
        </span>
      );
    case 'IN_PROGRESS':
      return (
        <span
          className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-500/10 text-blue-300 border border-blue-500/20 ${className}`}
        >
          IN PROGRESS
        </span>
      );
    case 'COMPLETED':
      return (
        <span
          className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-300 border border-emerald-500/20 ${className}`}
        >
          COMPLETED
        </span>
      );
    case 'CANCELLED':
      return (
        <span
          className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-slate-700/40 text-slate-400 border border-slate-700 ${className}`}
        >
          CANCELLED
        </span>
      );
    default:
      return (
        <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs text-slate-400 ${className}`}>
          {status}
        </span>
      );
  }
};
