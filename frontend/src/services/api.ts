import type { Asset, Page, RiskAssessment, RiskAssessmentDetails, TelemetryRecord } from '../types/api';

export interface ApiError extends Error {
  detail?: string;
  status: number;
}

interface ProblemDetail {
  detail?: string;
}

const baseUrl = import.meta.env.VITE_API_BASE_URL || '';

async function handleResponse(response: Response): Promise<void> {
  if (!response.ok) {
    let message: string;
    try {
      const problem: ProblemDetail = await response.json();
      message = problem.detail || response.statusText || `HTTP ${response.status}`;
    } catch {
      message = response.statusText || `HTTP ${response.status}`;
    }
    const error = new Error(message) as ApiError;
    error.status = response.status;
    throw error;
  }
}

async function post<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  });
  await handleResponse(response);
  return response.json() as Promise<T>;
}

async function get<T>(path: string): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`);
  await handleResponse(response);
  return response.json() as Promise<T>;
}

export const api = {
  getAssets: () => get<Page<Asset>>('/api/v1/assets?size=50'),
  getAsset: (assetId: string) => get<Asset>(`/api/v1/assets/${assetId}`),
  getTelemetry: (assetId: string) => get<Page<TelemetryRecord>>(`/api/v1/assets/${assetId}/telemetry?size=96`),
  getLatestRisk: (assetId: string) => get<RiskAssessment>(`/api/v1/assets/${assetId}/risk-assessments/latest`),
  createAssessment: (assetId: string) => post<RiskAssessmentDetails>(`/api/v1/assets/${assetId}/risk-assessments`),
  getTopRisks: () => get<Page<RiskAssessment>>('/api/v1/risk-assessments/top-risky?size=15')
};
