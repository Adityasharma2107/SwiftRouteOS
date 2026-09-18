import { apiClient } from './client';
import type { ApiResponse, Job, JobStatus, Priority, SlaStatus } from '../types';

export interface JobFilterParams {
  status?: JobStatus;
  priority?: Priority;
  slaStatus?: SlaStatus;
}

export const jobsApi = {
  getAllJobs: async (params?: JobFilterParams): Promise<Job[]> => {
    const response = await apiClient.get<ApiResponse<Job[]>>('/jobs', { params });
    return response.data.data;
  },

  getJobById: async (id: number): Promise<Job> => {
    const response = await apiClient.get<ApiResponse<Job>>(`/jobs/${id}`);
    return response.data.data;
  },

  transitionJob: async (
    id: number,
    targetStatus: JobStatus,
    notes?: string,
    cancellationReason?: string
  ): Promise<Job> => {
    const response = await apiClient.patch<ApiResponse<Job>>(`/jobs/${id}/transition`, {
      targetStatus,
      notes,
      cancellationReason,
    });
    return response.data.data;
  },
};
