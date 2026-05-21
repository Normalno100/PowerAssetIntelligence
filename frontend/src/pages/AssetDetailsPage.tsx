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
    api.getAsset(id).then(setAsset);
    api.getTelemetry(id).then((res) => setTelemetry(res.content));
    api.getRiskDetails(id).then(setRisk);
  }, [id]);

  return (
    <div className="space-y-4">
      <section className="rounded-xl border border-slate-800 bg-slate-900 p-4">
        <h2 className="text-xl font-semibold">{asset?.name}</h2>
        <p className="text-sm text-slate-400">{asset?.type} • {asset?.location} • {asset?.status}</p>
      </section>
      <section className="rounded-xl border border-slate-800 bg-slate-900 p-4"><h3 className="mb-3 font-semibold">Detailed Telemetry</h3><Line data={{ labels: telemetry.map((t) => new Date(t.timestamp).toLocaleTimeString()), datasets: [{ label: 'Vibration mm/s', data: telemetry.map((t) => t.vibrationMmS), borderColor: '#a78bfa' }, { label: 'Oil Level %', data: telemetry.map((t) => t.oilLevelPercentage), borderColor: '#34d399' }] }} /></section>
      <section className="rounded-xl border border-slate-800 bg-slate-900 p-4">
        <h3 className="mb-3 font-semibold">AI Risk Explainability</h3>
        <p className="mb-3 text-sm text-slate-300">Risk score: <span className="font-bold text-red-400">{risk?.riskScore.toFixed(1) ?? '-'}</span></p>
        <ul className="space-y-2 text-sm text-slate-300">{risk?.ruleResults?.map((r) => <li key={r.ruleName} className="rounded border border-slate-700 p-2">{r.ruleName}: {r.triggered ? 'Triggered' : 'Normal'} ({r.score.toFixed(2)}) — {r.reason}</li>)}</ul>
      </section>
    </div>
  );
}
