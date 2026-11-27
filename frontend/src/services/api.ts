import type { Asset, Page, RiskAssessment, RiskAssessmentDetails, TelemetryRecord } from '../types/api';
import { getToken, clearToken } from './auth';

// ---------------------------------------------------------------------------
// Error
// ---------------------------------------------------------------------------

export interface ApiError extends Error {
  status: number;
  detail?: string;
}

interface ProblemDetail {
  detail?: string;
}

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '';
const FETCH_TIMEOUT_MS = 10_000;
const MAX_RETRIES = 2;
const RETRY_BASE_DELAY_MS = 300;

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Parse an HTTP error into a typed ApiError. */
async function parseError(response: Response): Promise<ApiError> {
  let detail: string | undefined;
  try {
    const problem: ProblemDetail = await response.json();
    detail = problem.detail;
  } catch {
    // not JSON — keep undefined
  }

  const error = new Error(detail ?? response.statusText) as ApiError;
  error.status = response.status;
  error.name = 'ApiError';
  if (detail) {
    error.message = detail;
  }
  return error;
}

/** Check whether a response error is retryable (network-ish / 5xx). */
function isRetryableStatus(status: number): boolean {
  return status >= 500;
}

/** Check whether an error is a network / timeout / abort error. */
function isNetworkError(error: unknown): boolean {
  if (!error || typeof error !== 'object') return false;
  const name = (error as Record<string, unknown>).name;
  return (
    name === 'AbortError' ||
    name === 'TypeError' // fetch network failure
  );
}

/** Type guard: is this a typed ApiError from our handler? */
function isApiError(error: unknown): error is ApiError {
  return (
    error instanceof Error &&
    'status' in error &&
    typeof (error as ApiError).status === 'number'
  );
}

/** Sleep for *delay* milliseconds, honouring an AbortSignal. */
function sleep(delay: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(new DOMException('Aborted', 'AbortError'));
      return;
    }
    const timer = setTimeout(resolve, delay);
    const onAbort = () => {
      clearTimeout(timer);
      reject(new DOMException('Aborted', 'AbortError'));
    };
    signal?.addEventListener('abort', onAbort, { once: true });
  });
}

// ---------------------------------------------------------------------------
// Core: fetchWithTimeout + retry
// ---------------------------------------------------------------------------

async function fetchWithRetry(
  url: string,
  options: RequestInit,
  signal?: AbortSignal,
  attempt: number = 0,
): Promise<Response> {
  const controller = new AbortController();

  // Chain external signal → internal controller so unmount cancels in-flight
  const onExternalAbort = () => controller.abort();
  signal?.addEventListener('abort', onExternalAbort, { once: true });

  const timeoutId = setTimeout(
    () => controller.abort(new DOMException('Timeout', 'TimeoutError')),
    FETCH_TIMEOUT_MS,
  );

  try {
    const response = await fetch(url, {
      ...options,
      signal: controller.signal,
    });
    return response;
  } catch (error: unknown) {
    clearTimeout(timeoutId);

    // Propagate abort from external signal immediately
    if (signal?.aborted) throw error;

    // Timeout is not retryable
    if (error && typeof error === 'object' && 'name' in error && error.name === 'TimeoutError') {
      throw error;
    }

    // Retryable?
    const canRetry =
      attempt < MAX_RETRIES &&
      (isNetworkError(error) || (isApiError(error) && isRetryableStatus(error.status)));

    if (canRetry) {
      const delay = RETRY_BASE_DELAY_MS * 2 ** attempt;
      await sleep(delay, signal);
      return fetchWithRetry(url, options, signal, attempt + 1);
    }

    throw error;
  } finally {
    clearTimeout(timeoutId);
    signal?.removeEventListener('abort', onExternalAbort);
  }
}

// ---------------------------------------------------------------------------
// Response handling
// ---------------------------------------------------------------------------

async function handleResponse(response: Response): Promise<void> {
  if (!response.ok) {
    if (response.status === 401) {
      clearToken();
      window.location.href = '/login';
      return;
    }

    const error = await parseError(response);
    throw error;
  }
}

// ---------------------------------------------------------------------------
// HTTP verbs  (public API — contracts unchanged)
// ---------------------------------------------------------------------------

async function post<T>(path: string, body?: unknown, signal?: AbortSignal): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const response = await fetchWithRetry(`${BASE_URL}${path}`, {
    method: 'POST',
    headers,
    body: body ? JSON.stringify(body) : undefined,
  }, signal);

  await handleResponse(response);
  return response.json() as Promise<T>;
}

async function get<T>(path: string, signal?: AbortSignal): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const response = await fetchWithRetry(`${BASE_URL}${path}`, { headers }, signal);
  await handleResponse(response);
  return response.json() as Promise<T>;
}

// ---------------------------------------------------------------------------
// Public API surface — contracts preserved
// ---------------------------------------------------------------------------

export const api = {
  getAssets: (signal?: AbortSignal) => get<Page<Asset>>('/api/v1/assets?size=50', signal),
  getAsset: (assetId: string, signal?: AbortSignal) => get<Asset>(`/api/v1/assets/${assetId}`, signal),
  getTelemetry: (assetId: string, signal?: AbortSignal) =>
    get<Page<TelemetryRecord>>(`/api/v1/assets/${assetId}/telemetry?size=96`, signal),
  getLatestRisk: (assetId: string, signal?: AbortSignal) =>
    get<RiskAssessment>(`/api/v1/assets/${assetId}/risk-assessments/latest`, signal),
  createAssessment: (assetId: string, signal?: AbortSignal) =>
    post<RiskAssessmentDetails>(`/api/v1/assets/${assetId}/risk-assessments`, undefined, signal),
  getTopRisks: (signal?: AbortSignal) =>
    get<Page<RiskAssessment>>('/api/v1/risk-assessments/top-risky?size=15', signal),
};
