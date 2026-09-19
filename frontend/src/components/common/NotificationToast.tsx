import React from 'react';
import { AlertOctagon, AlertTriangle, CheckCircle2, Info, X, Radio, Trash2 } from 'lucide-react';
import { useWebSocket } from '../../context/WebSocketContext';
import type { LiveNotification } from '../../types';

export const NotificationToast: React.FC = () => {
  const { notifications, dismissNotification, clearNotifications } = useWebSocket();

  if (notifications.length === 0) {
    return null;
  }

  const getIcon = (type: LiveNotification['type']) => {
    switch (type) {
      case 'error':
        return <AlertOctagon className="w-5 h-5 text-rose-400 shrink-0 mt-0.5 animate-pulse" />;
      case 'warning':
        return <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />;
      case 'success':
        return <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />;
      case 'info':
      default:
        return <Radio className="w-5 h-5 text-cyan-400 shrink-0 mt-0.5 animate-pulse" />;
    }
  };

  const getBorderColor = (type: LiveNotification['type']) => {
    switch (type) {
      case 'error':
        return 'border-rose-500/50 shadow-rose-950/40 bg-slate-900/95';
      case 'warning':
        return 'border-amber-500/50 shadow-amber-950/40 bg-slate-900/95';
      case 'success':
        return 'border-emerald-500/50 shadow-emerald-950/40 bg-slate-900/95';
      case 'info':
      default:
        return 'border-cyan-500/40 shadow-cyan-950/30 bg-slate-900/95';
    }
  };

  const getBadge = (notification: LiveNotification) => {
    switch (notification.type) {
      case 'error':
        return (
          <span className="px-1.5 py-0.5 text-[9px] font-black uppercase rounded bg-rose-500/20 text-rose-300 border border-rose-500/40">
            SLA Breach
          </span>
        );
      case 'warning':
        return (
          <span className="px-1.5 py-0.5 text-[9px] font-black uppercase rounded bg-amber-500/20 text-amber-300 border border-amber-500/40">
            At Risk
          </span>
        );
      case 'success':
        return (
          <span className="px-1.5 py-0.5 text-[9px] font-black uppercase rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/40">
            Success
          </span>
        );
      case 'info':
      default:
        return (
          <span className="px-1.5 py-0.5 text-[9px] font-black uppercase rounded bg-cyan-500/20 text-cyan-300 border border-cyan-500/40">
            Realtime
          </span>
        );
    }
  };

  return (
    <aside aria-label="Live System Alerts" className="fixed bottom-5 right-5 z-50 flex flex-col gap-2.5 max-w-sm w-full pointer-events-none">
      {/* Header bar when multiple notifications exist */}
      {notifications.length > 2 && (
        <div className="flex items-center justify-between px-3 py-1.5 bg-slate-900/90 border border-slate-800 rounded-lg shadow-lg pointer-events-auto backdrop-blur-md">
          <span className="text-[11px] font-semibold text-slate-400 flex items-center gap-1.5">
            <Info className="w-3.5 h-3.5 text-cyan-400" />
            {notifications.length} Live Alerts
          </span>
          <button
            onClick={clearNotifications}
            className="flex items-center gap-1 text-[10px] text-slate-400 hover:text-rose-400 transition-colors cursor-pointer"
          >
            <Trash2 className="w-3 h-3" />
            Dismiss All
          </button>
        </div>
      )}

      {/* Notification Toast Stack */}
      {notifications.slice(0, 5).map((n) => (
        <div
          key={n.id}
          className={`pointer-events-auto border rounded-xl p-3.5 shadow-2xl backdrop-blur-md transition-all duration-200 animate-in fade-in slide-in-from-bottom-3 ${getBorderColor(
            n.type
          )}`}
        >
          <div className="flex items-start justify-between gap-2.5">
            <div className="flex items-start gap-2.5 flex-1 min-w-0">
              {getIcon(n.type)}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1 flex-wrap">
                  <h4 className="text-xs font-bold text-slate-100 truncate">{n.title}</h4>
                  {getBadge(n)}
                </div>
                <p className="text-xs text-slate-300 leading-snug break-words">{n.message}</p>
                <div className="mt-1.5 flex items-center justify-between text-[10px] text-slate-400">
                  <span className="font-mono text-slate-400">{n.eventType}</span>
                  <span>{new Date(n.timestamp).toLocaleTimeString()}</span>
                </div>
              </div>
            </div>

            <button
              onClick={() => dismissNotification(n.id)}
              className="p-1 rounded-md text-slate-400 hover:text-white hover:bg-slate-800/80 transition-colors cursor-pointer shrink-0"
              title="Dismiss notification"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      ))}
    </aside>
  );
};
