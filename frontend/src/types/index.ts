export type SeverityLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type EventType =
  | 'AUTH_FAILURE'
  | 'BRUTE_FORCE'
  | 'RATE_LIMIT_EXCEEDED'
  | 'UNAUTHORIZED_ACCESS'
  | 'SUSPICIOUS_IP'
  | 'ABNORMAL_REQUEST_RATE';

export interface SystemHealth {
  status: string;
  timestamp: string;
  service: string;
  components: {
    redis: string;
    database: string;
    gateway: string;
  };
}

export interface SecurityEvent {
  id: number;
  eventUuid: string;
  eventType: EventType;
  severity: SeverityLevel;
  sourceIp: string;
  clientIdentity?: string;
  endpoint: string;
  httpMethod: string;
  description: string;
  ruleTriggered?: string;
  actionTaken: string;
  timestamp: string;
}

/** Matches the backend SecurityMetricsOverview DTO exactly. */
export interface SecurityMetricsOverview {
  totalSecurityEvents: number;
  blockedRequests: number;
  authFailures: number;
  bruteForceEvents: number;
  suspiciousIps: number;
  rateLimitHits: number;
  recentEventsLastHour: number;
  requestsPerSecond: number;
  activeRoutes: number;
  activeApiKeys: number;
}

/** One 5-minute window in the traffic timeline. */
export interface TrafficBucket {
  windowStart: string;
  totalEvents: number;
  blockedEvents: number;
  authFailures: number;
}

export interface GatewayRoute {
  id: number;
  routeId: string;
  pathPattern: string;
  isActive: boolean;
  requiresAuth: boolean;
  allowedRoles: string;
  rateLimitPerMin: number;
  service: {
    name: string;
    baseUrl: string;
    status: string;
  };
}

export interface SecurityRule {
  id: number;
  name: string;
  description?: string;
  isEnabled: boolean;
  threshold?: number;
  windowSeconds?: number;
}
