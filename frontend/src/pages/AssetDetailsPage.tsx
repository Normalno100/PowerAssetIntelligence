import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Line } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Tooltip } from 'chart.js';
import { api } from '../services/api';
import type { Asset, RiskAssessment, TelemetryRecord } from '../types/api';
ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip);

export function AssetDetailsPage() {
  const { id = '' } = useParams();
  const [asset, setAsset] = useState<Asset | null>(null);
  const [telemetry, setTelemetry] = useState<TelemetryRecord[]>([]);
  const [risk, setRisk] = useState<RiskAssessment | null>(null);

  useEffect(() => {
    api.getAsset(id).then(setAsset);
    api.getTelemetry(id).then((res) => setTelemetry(res.content));
    api.getLatestRisk(id).then(setRisk);
  }, [id]);

  return (
    <div className="space-y-4">
      <section className="rounded-xl border border-slate-800 bg-slate-900 p-4">
        <h2 className="text-xl font-semibold">{asset?.name}</h2>
        <p className="text-sm text-slate-400">{asset?.type} • {asset?.location} • {asset?.status}</p>
      </section>
      <section className="rounded-xl border border-slate-800 bg-slate-900 p-4"><h3 className="mb-3 font-semibold">Detailed Telemetry</h3><Line data={{ labels: telemetry.map((t) => new Date(t.timestamp).toLocaleTimeString()), datasets: [{ label: 'Vibration mm/s', data: telemetry.map((t) => t.vibrationMmS), borderColor: '#a78bfa' }, { label: 'Voltage kV', data: telemetry.map((t) => t.voltageKv), borderColor: '#34d399' }] }} /></section>
      <section className="rounded-xl border border-slate-800 bg-slate-900 p-4">
        <h3 className="mb-3 font-semibold">AI Risk Explainability</h3>
        <p className="mb-3 text-sm text-slate-300">Risk score: <span className="font-bold text-red-400">{risk?.riskScore?.toFixed(1) ?? '-'}</span></p>
        <ul className="space-y-2 text-sm text-slate-300">{risk?.riskFactors?.map((r) => <li key={r.code} className="rounded border border-slate-700 p-2">{r.code}: {r.severity} ({r.contribution.toFixed(2)}) — {r.description}</li>)}</ul>
      </section>
    </div>
  );
}
