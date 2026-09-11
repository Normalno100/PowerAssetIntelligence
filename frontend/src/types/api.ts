export interface Page<T> {
  content: T[];
  totalElements: number;
}

export interface Asset {
  id: string;
  name: string;
  type: string;
  status: string;
  criticality: string;
  location: string;
  commissionDate: string;
}

export interface TelemetryRecord {
  id: string;
  assetId: string;
  temperatureCelsius: number;
  loadPercentage: number;
  oilLevelPercentage: number;
  vibrationMmS: number;
  timestamp: string;
}

export interface RiskFactor {
  code: string;
  severity: string;
  description: string;
  contribution: number;
}

export interface RiskAssessment {
  id: string;
  assetId: string;
  assessedAt: string;
  riskScore: number;
  riskLevel: string;
  riskFactors: RiskFactor[];
  recommendations: string[];
  modelVersion: string;
  explanation: string;
  createdAt: string;
  snapshot: Record<string, unknown>;
}

export interface RiskAssessmentDetails {
  assessment: RiskAssessment;
  features: Record<string, unknown>;
}
