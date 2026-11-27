import { useEffect, useState, useCallback, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { Line } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Tooltip } from 'chart.js';
import { api } from '../services/api';
import type { Asset, RiskAssessment, TelemetryRecord } from '../types/api';
import { LoadingState } from '../components/ui/LoadingState';
import { ErrorState } from '../components/ui/ErrorState';
import { EmptyState } from '../components/ui/EmptyState';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip);

export function AssetDetailsPage() {
  const { id = '' } = useParams();
  const [asset, setAsset] = useState<Asset | null>(null);
  const [telemetry, setTelemetry] = useState<TelemetryRecord[]>([]);
  const [risk, setRisk] = useState<RiskAssessment | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [riskLoading, setRiskLoading] = useState(false);
  const abortRef = useRef<AbortController | null>(null);

  const loadData = useCallback(() => {
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    setLoading(true);
    setError(null);

    let hasError = false;
    let errorMessages: string[] = [];

    const p1 = api.getAsset(id, controller.signal).then((a) => setAsset(a)).catch((err: Error) => {
      if (err.name !== 'AbortError') {
        hasError = true;
        errorMessages.push(`Asset: ${err.message ?? 'unknown error'}`);
      }
    });

    const p2 = api.getTelemetry(id, controller.signal).then((res) => setTelemetry(res.content)).catch((err: Error) => {
      if (err.name !== 'AbortError') {
        errorMessages.push(`Telemetry: ${err.message ?? 'unknown error'}`);
      }
    });

    const p3 = api.getLatestRisk(id, controller.signal).then((r) => setRisk(r)).catch((err: Error) => {
      if (err.name !== 'AbortError') {
        errorMessages.push(`Risk: ${err.message ?? 'unknown error'}`);
      }
    });

    Promise.all([p1, p2, p3]).finally(() => {
      if (hasError) {
        setError(errorMessages.join('; '));
      }
      setLoading(false);
    });
  }, [id]);

  useEffect(() => {
    loadData();
    return () => abortRef.current?.abort();
  }, [loadData]);

  const handleRunAssessment = () => {
    setRiskLoading(true);
    api.createAssessment(id)
      .then((details) => setRisk(details.assessment))
      .catch((err: Error) => {
        setError(err.message ?? 'Не удалось создать оценку риска');
      })
      .finally(() => setRiskLoading(false));
  };

  if (loading) {
    return <LoadingState message="Загрузка данных об оборудовании…" />;
  }

  if (error) {
    return <ErrorState message={error} onRetry={loadData} />;
  }

  if (!asset) {
    return <EmptyState message="Актив не найден" description="Убедитесь, что ID актива указан верно." />;
  }

  return (
    <div className="space-y-4">
      <section className="rounded-xl border border-slate-800 bg-slate-900 p-4">
        <h2 className="text-xl font-semibold">{asset.name}</h2>
        <p className="text-sm text-slate-400">{asset.type} • {asset.location} • {asset.status}</p>
      </section>
      <section className="rounded-xl border border-slate-800 bg-slate-900 p-4"><h3 className="mb-3 font-semibold">Detailed Telemetry</h3><Line data={{ labels: telemetry.map((t) => new Date(t.timestamp).toLocaleTimeString()), datasets: [{ label: 'Vibration mm/s', data: telemetry.map((t) => t.vibrationMmSec), borderColor: '#a78bfa' }, { label: 'Temperature °C', data: telemetry.map((t) => t.temperatureCelsius), borderColor: '#f87171' }, { label: 'Load %', data: telemetry.map((t) => t.loadPercent), borderColor: '#fbbf24' }] }} /></section>
      <section className="rounded-xl border border-slate-800 bg-slate-900 p-4">
        <h3 className="mb-3 font-semibold">AI Risk Explainability</h3>
        {risk === null ? (
          riskLoading ? (
            <LoadingState message="Создание оценки риска…" />
          ) : (
            <div className="space-y-3">
              <p className="text-sm text-slate-500">Risk assessment not yet available for this asset.</p>
              <button
                onClick={handleRunAssessment}
                disabled={riskLoading}
                className="rounded-lg bg-violet-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-violet-500 disabled:opacity-50"
              >
                {riskLoading ? 'Running…' : 'Run Risk Assessment'}
              </button>
            </div>
          )
        ) : (
          <>
            <p className="mb-3 text-sm text-slate-300">
              Risk score:{' '}
              <span className="font-bold text-red-400">
                {risk.riskScore !== null && risk.riskScore !== undefined
                  ? risk.riskScore.toFixed(1)
                  : '—'}
              </span>
            </p>
            <p className="mb-3 text-sm text-slate-300">
              Risk level:{' '}
              <span className="font-semibold text-yellow-400">{risk.riskLevel ?? '—'}</span>
            </p>
            <h4 className="mb-2 text-sm font-medium text-slate-200">Risk factors</h4>
            <ul className="space-y-2 text-sm text-slate-300">
              {risk.riskFactors.map((f) => (
                <li key={f.code} className="rounded border border-slate-700 p-2">
                  {f.code}: {f.severity} (contribution{' '}
                  {f.contribution !== null && f.contribution !== undefined
                    ? f.contribution.toFixed(2)
                    : '—'}
                  ) — {f.description}
                </li>
              ))}
            </ul>
            <h4 className="mb-2 mt-4 text-sm font-medium text-slate-200">Recommendations</h4>
            <ul className="list-inside list-disc text-sm text-slate-300">
              {risk.recommendations.map((rec, idx) => (
                <li key={idx}>{rec}</li>
              ))}
            </ul>
          </>
        )}
      </section>
    </div>
  );
}
