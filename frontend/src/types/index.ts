// Domain models & API envelopes for SwiftRouteOS

export type UserRole = 'ROLE_ADMIN' | 'ROLE_DISPATCHER' | 'ROLE_TECHNICIAN';

export type JobStatus = 'PENDING' | 'ASSIGNED' | 'EN_ROUTE' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export type Priority = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';

export type SlaStatus = 'HEALTHY' | 'NEARING_BREACH' | 'BREACHED';

export type EscalationStage = 'NEARING_BREACH' | 'BREACHED';

export type AuditAction = 
  | 'CREATED' 
  | 'ASSIGNED' 
  | 'STATUS_CHANGED' 
  | 'INVENTORY_RESERVED' 
  | 'INVENTORY_RELEASED' 
  | 'INVENTORY_CONSUMED' 
  | 'ESCALATED' 
  | 'MANUAL_OVERRIDE' 
  | 'CANCELLED';

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  error?: {
    errorCode: string;
    message: string;
    details?: string;
    timestamp: string;
  };
  timestamp: string;
}

export interface User {
  id: number;
  username: string;
  email: string;
  fullName: string;
  role: UserRole;
  enabled: boolean;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface Skill {
  id: number;
  code: string;
  name: string;
  description?: string;
}

export interface ServiceRequest {
  id: number;
  customerName: string;
  customerPhone: string;
  serviceAddress: string;
  latitude: number;
  longitude: number;
  equipmentType: string;
  issueDescription: string;
  priority: Priority;
  requiredSkill: Skill;
  estimatedDurationMinutes: number;
  status: string;
  createdAt: string;
}

export interface Job {
  id: number;
  serviceRequest: ServiceRequest;
  priority: Priority;
  status: JobStatus;
  slaResponseDeadline: string;
  slaResolutionDeadline: string;
  slaStatus: SlaStatus;
  actualResponseAt?: string;
  actualResolutionAt?: string;
  scheduledStart?: string;
  scheduledEnd?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Technician {
  id: number;
  user: User;
  currentLatitude: number;
  currentLongitude: number;
  currentStatus: 'AVAILABLE' | 'BUSY' | 'OFF_DUTY';
  shiftStart: string;
  shiftEnd: string;
  activeJobsCount: number;
  skills: Skill[];
}

export interface ScoreFactor {
  factor: string;
  weight: number;
  rawScore: number;
  weightedScore: number;
  explanation: string;
}

export interface CandidateScore {
  technicianId: number;
  technicianName: string;
  skills: string[];
  distanceKm: number;
  activeJobsCount: number;
  totalScore: number;
  factors: ScoreFactor[];
  shiftEndingSoon: boolean;
}

export interface InventoryItem {
  id: number;
  partNumber: string;
  name: string;
  description?: string;
  quantityOnHand: number;
  quantityReserved: number;
  quantityAvailable: number;
  reorderThreshold: number;
  unitCost: number;
  isLowStock: boolean;
}

export interface InventoryReservation {
  id: number;
  jobId: number;
  inventoryItemId: number;
  partNumber: string;
  partName: string;
  quantityReserved: number;
  status: 'RESERVED' | 'CONSUMED' | 'RELEASED';
  reservedAt: string;
  reason?: string;
}

export interface SlaPolicy {
  id: number;
  priority: Priority;
  responseDeadlineMinutes: number;
  resolutionDeadlineMinutes: number;
}

export interface SlaMetricsSummary {
  totalMonitoredJobs: number;
  healthyJobs: number;
  nearingBreachJobs: number;
  breachedJobs: number;
  complianceRatePercent: number;
}

export interface SlaEscalationEvent {
  id: number;
  jobId: number;
  thresholdStage: EscalationStage;
  triggeredAt: string;
  details?: string;
}

export interface SlaEvaluationResult {
  jobId: number;
  priority: Priority;
  currentSlaStatus: SlaStatus;
  responseElapsedPercent: number;
  resolutionElapsedPercent: number;
  responseRemainingSeconds: number;
  resolutionRemainingSeconds: number;
  responseBreached: boolean;
  resolutionBreached: boolean;
  evaluatedAt: string;
}

export interface AuditEvent {
  id: number;
  entityType: string;
  entityId: number;
  action: AuditAction;
  performedBy: string;
  details?: string;
  timestamp: string;
}

export interface DemoPersona {
  key: string;
  username: string;
  displayName: string;
  title: string;
  role: UserRole;
  avatarColor: string;
}

export type WebSocketEventType =
  | 'JOB_STATUS_CHANGED'
  | 'JOB_ASSIGNED'
  | 'SLA_WARNING'
  | 'SLA_BREACHED'
  | 'INVENTORY_RESERVED'
  | 'INVENTORY_RELEASED'
  | 'INVENTORY_CONSUMED'
  | 'INVENTORY_UPDATED'
  | 'CHAOS_EVENT';

export interface WebSocketMessage<T = unknown> {
  eventType: WebSocketEventType | string;
  destination: string;
  timestamp: string;
  payload: T;
  message?: string;
}

export interface LiveNotification {
  id: string;
  type: 'info' | 'warning' | 'error' | 'success';
  title: string;
  message: string;
  timestamp: string;
  eventType: string;
  payload?: unknown;
}

