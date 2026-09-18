import { apiClient } from './client';
import type { ApiResponse } from '../types';

export interface ScoreBreakdown {
  totalScore: number;
  skillMatchScore: number;
  distanceScore: number;
  workloadScore: number;
  shiftWindowScore: number;
  skillMatchExplanation: string;
  distanceExplanation: string;
  workloadExplanation: string;
  shiftExplanation: string;
}

export interface TechnicianCandidate {
  technicianId: number;
  technicianName: string;
  phone: string;
  status: string;
  currentLatitude: number;
  currentLongitude: number;
  proficiencyLevel: number;
  distanceKm: number;
  activeJobsToday: number;
  maxDailyJobs: number;
  rank: number;
  recommended: boolean;
  scoreBreakdown: ScoreBreakdown;
}

export interface DispatchRecommendation {
  jobId: number;
  serviceRequestId: number;
  requiredSkillCode: string;
  requiredSkillName: string;
  jobLatitude: number;
  jobLongitude: number;
  candidatesCount: number;
  recommendedCandidate: TechnicianCandidate;
  candidates: TechnicianCandidate[];
}

export interface DispatchConfirmationPayload {
  jobId: number;
  selectedTechnicianId: number;
  scheduledStartTime: string;
  scheduledEndTime: string;
  overrideReason?: string;
  notes?: string;
}

export interface DispatchConfirmationResult {
  assignmentId: number;
  jobId: number;
  technicianId: number;
  technicianName: string;
  scheduledStartTime: string;
  scheduledEndTime: string;
  isOverride: boolean;
  overrideReason?: string;
  assignedAt: string;
}

export const dispatchApi = {
  getRecommendations: async (jobId: number): Promise<DispatchRecommendation> => {
    const response = await apiClient.get<ApiResponse<DispatchRecommendation>>(
      `/dispatch/recommendations/${jobId}`
    );
    return response.data.data;
  },

  confirmDispatch: async (
    payload: DispatchConfirmationPayload
  ): Promise<DispatchConfirmationResult> => {
    const response = await apiClient.post<ApiResponse<DispatchConfirmationResult>>(
      '/dispatch/confirm',
      payload
    );
    return response.data.data;
  },
};
