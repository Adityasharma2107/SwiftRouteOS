import React, { createContext, useContext, useEffect, useState, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useQueryClient } from '@tanstack/react-query';
import type { WebSocketMessage, LiveNotification, WebSocketEventType } from '../types';

interface WebSocketContextType {
  isConnected: boolean;
  notifications: LiveNotification[];
  dismissNotification: (id: string) => void;
  clearNotifications: () => void;
  lastMessage: WebSocketMessage | null;
  publish: (destination: string, body: unknown) => void;
}

const WebSocketContext = createContext<WebSocketContextType | undefined>(undefined);

export const WebSocketProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [isConnected, setIsConnected] = useState(false);
  const [notifications, setNotifications] = useState<LiveNotification[]>([]);
  const [lastMessage, setLastMessage] = useState<WebSocketMessage | null>(null);
  const stompClientRef = useRef<Client | null>(null);
  const queryClient = useQueryClient();

  const dismissNotification = useCallback((id: string) => {
    setNotifications((prev) => prev.filter((n) => n.id !== id));
  }, []);

  const clearNotifications = useCallback(() => {
    setNotifications([]);
  }, []);

  const addNotification = useCallback((notification: Omit<LiveNotification, 'id' | 'timestamp'>) => {
    const id = `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
    const newNotif: LiveNotification = {
      ...notification,
      id,
      timestamp: new Date().toISOString(),
    };

    setNotifications((prev) => [newNotif, ...prev.slice(0, 19)]);

    // Auto-dismiss notification after 8 seconds (unless it's an error/breach)
    const timeout = notification.type === 'error' ? 12000 : 7000;
    setTimeout(() => {
      dismissNotification(id);
    }, timeout);
  }, [dismissNotification]);

  const handleMessage = useCallback((msg: WebSocketMessage) => {
    setLastMessage(msg);
    const eventType = msg.eventType as WebSocketEventType;
    const payload = msg.payload as Record<string, unknown> | null;

    switch (eventType) {
      case 'JOB_ASSIGNED': {
        const jobId = payload?.id ?? payload?.jobId ?? 'Unknown';
        const techId = payload?.assignedTechnicianId ?? payload?.technicianId;
        addNotification({
          type: 'info',
          title: `Job #${jobId} Assigned`,
          message: techId ? `Dispatched to Technician #${techId}` : (msg.message || 'Job has been assigned.'),
          eventType,
          payload,
        });
        queryClient.invalidateQueries({ queryKey: ['jobs'] });
        queryClient.invalidateQueries({ queryKey: ['job', jobId] });
        queryClient.invalidateQueries({ queryKey: ['sla-metrics-summary'] });
        queryClient.invalidateQueries({ queryKey: ['technicians'] });
        break;
      }
      case 'JOB_STATUS_CHANGED': {
        const jobId = payload?.id ?? payload?.jobId ?? 'Unknown';
        const status = payload?.status ?? 'Updated';
        addNotification({
          type: 'info',
          title: `Job #${jobId} Status: ${status}`,
          message: msg.message || `Job status moved to ${status}`,
          eventType,
          payload,
        });
        queryClient.invalidateQueries({ queryKey: ['jobs'] });
        queryClient.invalidateQueries({ queryKey: ['job', jobId] });
        queryClient.invalidateQueries({ queryKey: ['sla-metrics-summary'] });
        break;
      }
      case 'SLA_WARNING': {
        const jobId = payload?.jobId ?? payload?.id ?? 'Unknown';
        addNotification({
          type: 'warning',
          title: `SLA Alert: Job #${jobId} At Risk`,
          message: msg.message || 'Job has exceeded 80% SLA response or resolution window.',
          eventType,
          payload,
        });
        queryClient.invalidateQueries({ queryKey: ['sla-metrics-summary'] });
        queryClient.invalidateQueries({ queryKey: ['jobs'] });
        queryClient.invalidateQueries({ queryKey: ['job', jobId] });
        break;
      }
      case 'SLA_BREACHED': {
        const jobId = payload?.jobId ?? payload?.id ?? 'Unknown';
        addNotification({
          type: 'error',
          title: `CRITICAL: SLA Breached - Job #${jobId}`,
          message: msg.message || 'Job resolution SLA has breached service threshold.',
          eventType,
          payload,
        });
        queryClient.invalidateQueries({ queryKey: ['sla-metrics-summary'] });
        queryClient.invalidateQueries({ queryKey: ['jobs'] });
        queryClient.invalidateQueries({ queryKey: ['job', jobId] });
        break;
      }
      case 'INVENTORY_RESERVED':
      case 'INVENTORY_RELEASED':
      case 'INVENTORY_CONSUMED':
      case 'INVENTORY_UPDATED': {
        const partNumber = payload?.partNumber ?? '';
        addNotification({
          type: 'info',
          title: `Inventory Event: ${eventType}`,
          message: msg.message || `Inventory allocation updated for part ${partNumber}`,
          eventType,
          payload,
        });
        queryClient.invalidateQueries({ queryKey: ['inventory-items'] });
        queryClient.invalidateQueries({ queryKey: ['inventory-item'] });
        queryClient.invalidateQueries({ queryKey: ['inventory-reservations'] });
        break;
      }
      case 'CHAOS_EVENT': {
        addNotification({
          type: 'warning',
          title: 'Chaos Simulator Event',
          message: msg.message || 'Live concurrency scenario triggered.',
          eventType,
          payload,
        });
        queryClient.invalidateQueries({ queryKey: ['inventory-items'] });
        queryClient.invalidateQueries({ queryKey: ['inventory-reservations'] });
        break;
      }
      default: {
        if (msg.message) {
          addNotification({
            type: 'info',
            title: `System Alert: ${msg.eventType}`,
            message: msg.message,
            eventType: msg.eventType,
            payload,
          });
        }
        break;
      }
    }
  }, [addNotification, queryClient]);

  useEffect(() => {
    // Factory function for SockJS connection
    const createSockJSInstance = () => {
      const SockJSConstructor = (SockJS as unknown as { default?: typeof SockJS }).default ?? SockJS;
      const wsUrl = import.meta.env.VITE_WS_URL || '/ws';
      return new SockJSConstructor(wsUrl);
    };

    const client = new Client({
      webSocketFactory: createSockJSInstance,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (msg: string) => {
        if (import.meta.env.DEV) {
          console.debug('[STOMP]', msg);
        }
      },
      onConnect: () => {
        setIsConnected(true);
        console.log('[STOMP] Connected to WebSocket broker');

        // Subscribe to real-time channels
        client.subscribe('/topic/jobs', (message) => {
          try {
            const parsed: WebSocketMessage = JSON.parse(message.body);
            handleMessage(parsed);
          } catch (e) {
            console.error('Failed to parse /topic/jobs WebSocket message', e);
          }
        });

        client.subscribe('/topic/sla-alerts', (message) => {
          try {
            const parsed: WebSocketMessage = JSON.parse(message.body);
            handleMessage(parsed);
          } catch (e) {
            console.error('Failed to parse /topic/sla-alerts WebSocket message', e);
          }
        });

        client.subscribe('/topic/inventory', (message) => {
          try {
            const parsed: WebSocketMessage = JSON.parse(message.body);
            handleMessage(parsed);
          } catch (e) {
            console.error('Failed to parse /topic/inventory WebSocket message', e);
          }
        });

        client.subscribe('/topic/chaos', (message) => {
          try {
            const parsed: WebSocketMessage = JSON.parse(message.body);
            handleMessage(parsed);
          } catch (e) {
            console.error('Failed to parse /topic/chaos WebSocket message', e);
          }
        });
      },
      onDisconnect: () => {
        setIsConnected(false);
        console.log('[STOMP] Disconnected from WebSocket broker');
      },
      onStompError: (frame) => {
        console.error('[STOMP Error]', frame.headers['message'], frame.body);
        setIsConnected(false);
      },
      onWebSocketClose: () => {
        setIsConnected(false);
      },
    });

    client.activate();
    stompClientRef.current = client;

    return () => {
      if (stompClientRef.current) {
        stompClientRef.current.deactivate();
      }
    };
  }, [handleMessage]);

  const publish = useCallback((destination: string, body: unknown) => {
    if (stompClientRef.current && stompClientRef.current.connected) {
      stompClientRef.current.publish({
        destination,
        body: JSON.stringify(body),
      });
    } else {
      console.warn('[STOMP] Cannot publish: client not connected to broker');
    }
  }, []);

  return (
    <WebSocketContext.Provider
      value={{
        isConnected,
        notifications,
        dismissNotification,
        clearNotifications,
        lastMessage,
        publish,
      }}
    >
      {children}
    </WebSocketContext.Provider>
  );
};

export const useWebSocket = (): WebSocketContextType => {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocket must be used within a WebSocketProvider');
  }
  return context;
};
