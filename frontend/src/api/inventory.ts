import { apiClient } from './client';
import type { ApiResponse, InventoryItem, InventoryReservation } from '../types';

export interface ReserveItemParam {
  inventoryItemId: number;
  quantity: number;
}

export interface ChaosSimulationResult {
  scarceItemId: number;
  partNumber: string;
  initialAvailableQuantity: number;
  threadsAttempted: number;
  successfulReservations: number;
  rejectedReservations: number;
  finalReservedQuantity: number;
  finalAvailableQuantity: number;
  durationMs: number;
  threadLogs: string[];
}

export const inventoryApi = {
  getAllItems: async (): Promise<InventoryItem[]> => {
    const response = await apiClient.get<ApiResponse<InventoryItem[]>>('/inventory/items');
    return response.data.data;
  },

  getItemById: async (id: number): Promise<InventoryItem> => {
    const response = await apiClient.get<ApiResponse<InventoryItem>>(`/inventory/items/${id}`);
    return response.data.data;
  },

  getLowStockItems: async (): Promise<InventoryItem[]> => {
    const response = await apiClient.get<ApiResponse<InventoryItem[]>>('/inventory/items/low-stock');
    return response.data.data;
  },

  getJobReservations: async (jobId: number): Promise<InventoryReservation[]> => {
    const response = await apiClient.get<ApiResponse<InventoryReservation[]>>(
      `/inventory/jobs/${jobId}/reservations`
    );
    return response.data.data;
  },

  reserveParts: async (jobId: number, items: ReserveItemParam[]): Promise<InventoryReservation[]> => {
    const response = await apiClient.post<ApiResponse<InventoryReservation[]>>('/inventory/reserve', {
      jobId,
      items,
    });
    return response.data.data;
  },

  releaseReservations: async (
    jobId: number,
    reason?: string
  ): Promise<InventoryReservation[]> => {
    const response = await apiClient.post<ApiResponse<InventoryReservation[]>>(
      `/inventory/jobs/${jobId}/release`,
      null,
      { params: { reason } }
    );
    return response.data.data;
  },

  runChaosTest: async (
    threads: number = 10,
    resetAfterTest: boolean = true
  ): Promise<ChaosSimulationResult> => {
    const response = await apiClient.post<ApiResponse<ChaosSimulationResult>>(
      '/inventory/chaos-test',
      null,
      { params: { threads, resetAfterTest } }
    );
    return response.data.data;
  },
};
