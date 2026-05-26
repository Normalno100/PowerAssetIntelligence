import type { Asset, Page, RiskAssessment, RiskAssessmentDetails, TelemetryRecord } from '../types/api';

const baseUrl = import.meta.env.VITE_API_BASE_URL || '';

async function get<T>(path: string): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`);
  if (!response.ok) throw new Error(`API error ${response.status}`);
  return response.json() as Promise<T>;
}

export const api = {
  getAssets: () => get<Page<Asset>>('/api/v1/assets?size=50'),
  getAsset: (assetId: string) => get<Asset>(`/api/v1/assets/${assetId}`),
  getTelemetry: (assetId: string) => get<Page<TelemetryRecord>>(`/api/v1/assets/${assetId}/telemetry?size=96`),
  getLatestRisk: (assetId: string) => get<RiskAssessment>(`/api/v1/assets/${assetId}/risk-assessments/latest`),
  getRiskDetails: (assetId: string) => get<RiskAssessmentDetails>(`/api/v1/risk-analysis/${assetId}`),
  getTopRisks: () => get<Page<RiskAssessment>>('/api/v1/risk-assessments/top-risky?size=15')
};
