import { useEffect, useMemo, useState } from 'react';
import { Bar, Line } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, BarElement, Tooltip, Legend } from 'chart.js';
import { api } from '../services/api';
import type { Asset, TelemetryRecord } from '../types/api';
import { AssetTable } from '../components/AssetTable';
import { Heatmap } from '../components/Heatmap';
ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Tooltip, Legend);

export function DashboardPage() {
  const [assets, setAssets] = useState<Asset[]>([]);
  const [telemetry, setTelemetry] = useState<TelemetryRecord[]>([]);

  useEffect(() => {
    api.getAssets().then((res) => {
      setAssets(res.content);
      if (res.content[0]) api.getTelemetry(res.content[0].id).then((t) => setTelemetry(t.content));
    });
  }, []);

  const telemetryChart = useMemo(() => ({
    labels: telemetry.map((t) => new Date(t.timestamp).toLocaleTimeString()),
    datasets: [
      { label: 'Temperature °C', data: telemetry.map((t) => t.temperatureCelsius), borderColor: '#f43f5e' },
      { label: 'Load %', data: telemetry.map((t) => t.loadPercentage), borderColor: '#60a5fa' }
    ]
  }), [telemetry]);

  const criticalityChart = useMemo(() => {
    const groups = assets.reduce<Record<string, number>>((acc, a) => ({ ...acc, [a.criticality]: (acc[a.criticality] || 0) + 1 }), {});
    return { labels: Object.keys(groups), datasets: [{ label: 'Assets', data: Object.values(groups), backgroundColor: ['#22c55e', '#f59e0b', '#ef4444'] }] };
  }, [assets]);

  return (
    <>
      <section className="grid gap-4 md:grid-cols-4">
        {['Total Assets', 'Online', 'High Risk', 'Regions'].map((k, i) => (
          <div key={k} className="rounded-xl border border-slate-800 bg-slate-900 p-4"><p className="text-xs text-slate-400">{k}</p><p className="text-2xl font-bold">{[assets.length, assets.filter((a) => a.status === 'ACTIVE').length, assets.filter((a) => a.criticality === 'CRITICAL').length, new Set(assets.map((a) => a.location)).size][i]}</p></div>
        ))}
      </section>
      <section className="grid gap-4 lg:grid-cols-2">
        <div className="rounded-xl border border-slate-800 bg-slate-900 p-4"><h3 className="mb-2 text-sm font-semibold">Telemetry Trend</h3><Line data={telemetryChart} /></div>
        <div className="rounded-xl border border-slate-800 bg-slate-900 p-4"><h3 className="mb-2 text-sm font-semibold">Asset Criticality</h3><Bar data={criticalityChart} /></div>
      </section>
      <Heatmap assets={assets} />
      <section>
        <h2 className="mb-2 text-sm font-semibold text-slate-300">Asset Registry</h2>
        <AssetTable assets={assets} />
      </section>
    </>
  );
}
