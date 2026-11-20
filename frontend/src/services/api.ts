import type { Asset, Page, RiskAssessment, RiskAssessmentDetails, TelemetryRecord } from '../types/api';
import { getToken, clearToken } from './auth';

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
    if (response.status === 401) {
      clearToken();
      window.location.href = '/login';
      return;
    }

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
  const token = getToken();
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${baseUrl}${path}`, {
    method: 'POST',
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  await handleResponse(response);
  return response.json() as Promise<T>;
}

async function get<T>(path: string): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${baseUrl}${path}`, { headers });
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
