import React, { type ReactNode } from 'react';
import { Header } from './Header';
import { Sidebar, type NavTab } from './Sidebar';

interface AppLayoutProps {
  activeTab: NavTab;
  onTabChange: (tab: NavTab) => void;
  children: ReactNode;
}

export const AppLayout: React.FC<AppLayoutProps> = ({
  activeTab,
  onTabChange,
  children,
}) => {
  return (
    <div className="min-h-screen bg-slate-950 flex flex-col text-slate-100 antialiased">
      {/* Top Header */}
      <Header />

      {/* Main Container */}
      <div className="flex-1 flex overflow-hidden">
        {/* Left Sidebar */}
        <Sidebar activeTab={activeTab} onTabChange={onTabChange} />

        {/* Dynamic Main Content */}
        <main className="flex-1 overflow-y-auto bg-slate-950 p-6">
          <div className="max-w-7xl mx-auto space-y-6">{children}</div>
        </main>
      </div>
    </div>
  );
};
