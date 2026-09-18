import { apiClient } from './client';
import type {
  ApiResponse,
  SlaPolicy,
  SlaMetricsSummary,
  SlaEscalationEvent,
  SlaEvaluationResult,
} from '../types';

export const slaApi = {
  getPolicies: async (): Promise<SlaPolicy[]> => {
    const response = await apiClient.get<ApiResponse<SlaPolicy[]>>('/sla/policies');
    return response.data.data;
  },

  getMetricsSummary: async (): Promise<SlaMetricsSummary> => {
    const response = await apiClient.get<ApiResponse<SlaMetricsSummary>>('/sla/metrics/summary');
    return response.data.data;
  },

  getJobEscalations: async (jobId: number): Promise<SlaEscalationEvent[]> => {
    const response = await apiClient.get<ApiResponse<SlaEscalationEvent[]>>(
      `/sla/jobs/${jobId}/escalations`
    );
    return response.data.data;
  },

  triggerBatchEvaluation: async (): Promise<number> => {
    const response = await apiClient.post<ApiResponse<number>>('/sla/evaluate');
    return response.data.data;
  },

  evaluateSingleJob: async (jobId: number): Promise<SlaEvaluationResult> => {
    const response = await apiClient.post<ApiResponse<SlaEvaluationResult>>(
      `/sla/jobs/${jobId}/evaluate`
    );
    return response.data.data;
  },
};
