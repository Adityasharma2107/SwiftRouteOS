import React from 'react';
import {
  LayoutDashboard,
  MapPin,
  Package,
  Activity,
  ShieldAlert,
  HardDriveDownload,
  CheckCircle2,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export type NavTab = 'dashboard' | 'map' | 'inventory' | 'chaos' | 'policies';

interface SidebarProps {
  activeTab: NavTab;
  onTabChange: (tab: NavTab) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ activeTab, onTabChange }) => {
  const { user } = useAuth();

  const navItems: Array<{
    id: NavTab;
    label: string;
    description: string;
    icon: React.ComponentType<{ className?: string }>;
    roles: string[];
    badge?: string;
  }> = [
    {
      id: 'dashboard',
      label: 'Operations Board',
      description: 'Jobs, SLA alerts, and transitions',
      icon: LayoutDashboard,
      roles: ['ROLE_ADMIN', 'ROLE_DISPATCHER', 'ROLE_TECHNICIAN'],
    },
    {
      id: 'map',
      label: 'Live Dispatch Map',
      description: 'GPS technician & job locations',
      icon: MapPin,
      roles: ['ROLE_ADMIN', 'ROLE_DISPATCHER'],
      badge: 'GIS',
    },
    {
      id: 'inventory',
      label: 'Parts & Inventory',
      description: 'Pessimistic locking & reservations',
      icon: Package,
      roles: ['ROLE_ADMIN', 'ROLE_DISPATCHER', 'ROLE_TECHNICIAN'],
    },
    {
      id: 'chaos',
      label: 'Chaos Concurrency Lab',
      description: '10-thread race & lock demo',
      icon: Activity,
      roles: ['ROLE_ADMIN', 'ROLE_DISPATCHER'],
      badge: 'PRO',
    },
    {
      id: 'policies',
      label: 'SLA Policies & Engine',
      description: 'Thresholds & scheduled workers',
      icon: ShieldAlert,
      roles: ['ROLE_ADMIN', 'ROLE_DISPATCHER'],
    },
  ];

  return (
    <aside className="w-64 bg-slate-900/95 border-r border-slate-800 flex flex-col justify-between shrink-0 select-none">
      {/* Navigation Section */}
      <div className="p-4 space-y-6">
        <div>
          <div className="text-[10px] font-bold uppercase tracking-wider text-slate-500 px-3 mb-2">
            Command Modules
          </div>
          <nav className="space-y-1">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.id;
              const isAllowed = user ? item.roles.includes(user.role) : true;

              return (
                <button
                  key={item.id}
                  onClick={() => isAllowed && onTabChange(item.id)}
                  disabled={!isAllowed}
                  className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-left transition-all cursor-pointer ${
                    !isAllowed
                      ? 'opacity-40 cursor-not-allowed hover:bg-transparent'
                      : isActive
                      ? 'bg-blue-600 text-white font-medium shadow-lg shadow-blue-600/20'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <Icon className={`w-4 h-4 ${isActive ? 'text-white' : 'text-slate-400'}`} />
                    <div>
                      <div className="text-xs font-semibold leading-tight">{item.label}</div>
                      <div className={`text-[10px] ${isActive ? 'text-blue-100' : 'text-slate-500'}`}>
                        {item.description}
                      </div>
                    </div>
                  </div>
                  {item.badge && (
                    <span
                      className={`text-[9px] font-bold px-1.5 py-0.5 rounded uppercase tracking-wider ${
                        isActive
                          ? 'bg-blue-700 text-white'
                          : 'bg-slate-800 text-cyan-400 border border-slate-700'
                      }`}
                    >
                      {item.badge}
                    </span>
                  )}
                </button>
              );
            })}
          </nav>
        </div>
      </div>

      {/* Footer / System Status */}
      <div className="p-4 border-t border-slate-800 bg-slate-950/40">
        <div className="bg-slate-900/90 rounded-xl p-3 border border-slate-800 space-y-2">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
              </span>
              <span className="text-[11px] font-semibold text-slate-300">Backend Online</span>
            </div>
            <span className="text-[10px] font-mono text-slate-400">Port 8080</span>
          </div>

          <div className="text-[10px] text-slate-400 flex items-center justify-between pt-1 border-t border-slate-800/60">
            <span className="flex items-center gap-1">
              <CheckCircle2 className="w-3 h-3 text-cyan-400" />
              PostgreSQL 18
            </span>
            <span className="flex items-center gap-1 text-slate-400">
              <HardDriveDownload className="w-3 h-3 text-slate-400" />
              Flyway V2
            </span>
          </div>
        </div>
      </div>
    </aside>
  );
};
