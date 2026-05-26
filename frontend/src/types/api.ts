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

export interface RiskAssessment {
  id: string;
  assetId: string;
  riskLevel: string;
  riskScore: number;
  assessedAt: string;
}

export interface RiskAssessmentDetails extends RiskAssessment {
  recommendations: string[];
  ruleResults: { ruleName: string; score: number; triggered: boolean; reason: string }[];
}
