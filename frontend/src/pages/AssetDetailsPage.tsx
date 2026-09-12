import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Line } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Tooltip } from 'chart.js';
import { api } from '../services/api';
import type { Asset, RiskAssessmentDetails, TelemetryRecord } from '../types/api';
ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip);

export function AssetDetailsPage() {
  const { id = '' } = useParams();
  const [asset, setAsset] = useState<Asset | null>(null);
  const [telemetry, setTelemetry] = useState<TelemetryRecord[]>([]);
  const [risk, setRisk] = useState<RiskAssessmentDetails | null>(null);

  useEffect(() => {
    api.getAsset(id)
      .then(setAsset)
      .catch(console.error);
    api.getTelemetry(id)
      .then((res) => setTelemetry(res.content))
      .catch(console.error);
    api.createAssessment(id)
      .then(setRisk)
      .catch(console.error);
  }, [id]);

  return (
    <div className="space-y-4">
      <section className="rounded-xl border border-slate-800 bg-slate-900 p-4">
        <h2 className="text-xl font-semibold">{asset?.name}</h2>
        <p className="text-sm text-slate-400">{asset?.type} • {asset?.location} • {asset?.status}</p>
      </section>
      <section className="rounded-xl border border-slate-800 bg-slate-900 p-4"><h3 className="mb-3 font-semibold">Detailed Telemetry</h3><Line data={{ labels: telemetry.map((t) => new Date(t.timestamp).toLocaleTimeString()), datasets: [{ label: 'Vibration mm/s', data: telemetry.map((t) => t.vibrationMmSec), borderColor: '#a78bfa' }, { label: 'Temperature °C', data: telemetry.map((t) => t.temperatureCelsius), borderColor: '#f87171' }, { label: 'Load %', data: telemetry.map((t) => t.loadPercent), borderColor: '#fbbf24' }] }} /></section>
      <section className="rounded-xl border border-slate-800 bg-slate-900 p-4">
        <h3 className="mb-3 font-semibold">AI Risk Explainability</h3>
        {risk === null ? (
          <p className="text-sm text-slate-500">Loading risk assessment…</p>
        ) : (
          <>
            <p className="mb-3 text-sm text-slate-300">
              Risk score:{' '}
              <span className="font-bold text-red-400">
                {risk.assessment.riskScore.toFixed(1)}
              </span>
            </p>
            <p className="mb-3 text-sm text-slate-300">
              Risk level:{' '}
              <span className="font-semibold text-yellow-400">{risk.assessment.riskLevel}</span>
            </p>
            <h4 className="mb-2 text-sm font-medium text-slate-200">Risk factors</h4>
            <ul className="space-y-2 text-sm text-slate-300">
              {risk.assessment.riskFactors.map((f) => (
                <li key={f.code} className="rounded border border-slate-700 p-2">
                  {f.code}: {f.severity} (contribution {f.contribution.toFixed(2)}) — {f.description}
                </li>
              ))}
            </ul>
            <h4 className="mb-2 mt-4 text-sm font-medium text-slate-200">Recommendations</h4>
            <ul className="list-inside list-disc text-sm text-slate-300">
              {risk.assessment.recommendations.map((rec, idx) => (
                <li key={idx}>{rec}</li>
              ))}
            </ul>
          </>
        )}
      </section>
    </div>
  );
}
