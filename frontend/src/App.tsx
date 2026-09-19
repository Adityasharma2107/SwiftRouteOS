import React, { useState } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './context/AuthContext';
import { WebSocketProvider } from './context/WebSocketContext';
import { NotificationToast } from './components/common/NotificationToast';
import { AppLayout } from './components/layout/AppLayout';
import type { NavTab } from './components/layout/Sidebar';
import { OperationsDashboardView } from './components/views/OperationsDashboardView';
import { LiveMapView } from './components/views/LiveMapView';
import { InventoryView } from './components/views/InventoryView';
import { ChaosLabView } from './components/views/ChaosLabView';
import { SlaPoliciesView } from './components/views/SlaPoliciesView';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

const MainContent: React.FC = () => {
  const [activeTab, setActiveTab] = useState<NavTab>('dashboard');

  return (
    <AppLayout activeTab={activeTab} onTabChange={setActiveTab}>
      {activeTab === 'dashboard' && <OperationsDashboardView />}
      {activeTab === 'map' && <LiveMapView />}
      {activeTab === 'inventory' && <InventoryView />}
      {activeTab === 'chaos' && <ChaosLabView />}
      {activeTab === 'policies' && <SlaPoliciesView />}
      <NotificationToast />
    </AppLayout>
  );
};

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <WebSocketProvider>
          <MainContent />
        </WebSocketProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;
