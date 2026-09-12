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
  installationDate: string;
  manufacturer: string;
  expectedServiceLifeYears: number;
  technicalParameters: Record<string, string>;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface TelemetryRecord {
  id: string;
  assetId: string;
  timestamp: string;
  temperatureCelsius: number;
  loadPercent: number;
  voltageKv: number;
  currentAmpere: number;
  vibrationMmSec: number;
  overheatingCount: number;
  sourceSensorId: string;
  externalTelemetryId: string;
  createdAt: string;
}

export interface RiskFactor {
  code: string;
  severity: string;
  description: string;
  contribution: number;
}

export interface RiskAssessmentSnapshot {
  assetType: string;
  assetStatus: string;
  criticality: string;
  assetAgeYears: number;
  latestTemperatureCelsius: number;
  latestLoadPercent: number;
  latestOverheatingCount: number;
  repairsLastYear: number;
  averageTemperatureCelsius: number;
  maxTemperatureCelsius: number;
  averageLoadPercent: number;
  maxLoadPercent: number;
  overheatingEventsLast24Hours: number;
  temperatureTrendCelsiusPerHour: number;
  loadTrendPercentPerHour: number;
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
  snapshot: RiskAssessmentSnapshot;
}

export interface RiskFeaturesResponse {
  assetId: string;
  assetType: string;
  assetStatus: string;
  criticality: string;
  assetAgeYears: number;
  latestTemperatureCelsius: number;
  latestLoadPercent: number;
  latestOverheatingCount: number;
  repairsLastYear: number;
  averageTemperatureCelsius: number;
  maxTemperatureCelsius: number;
  averageLoadPercent: number;
  maxLoadPercent: number;
  overheatingEventsLast24Hours: number;
  temperatureTrendCelsiusPerHour: number;
  loadTrendPercentPerHour: number;
}

export interface RiskAssessmentDetails {
  assessment: RiskAssessment;
  features: RiskFeaturesResponse;
}
