import type { Asset } from '../types/api';

export function Heatmap({ assets }: { assets: Asset[] }) {
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900 p-4">
      <h3 className="mb-4 text-sm font-semibold text-slate-300">Grid Health Heatmap</h3>
      <div className="grid grid-cols-5 gap-2">
        {assets.slice(0, 25).map((asset) => {
          const sev = asset.criticality === 'CRITICAL' ? 'bg-red-600' : asset.criticality === 'HIGH' ? 'bg-amber-500' : 'bg-emerald-500';
          return <div key={asset.id} title={`${asset.name} • ${asset.location}`} className={`h-12 rounded ${sev} opacity-80`} />;
        })}
      </div>
    </div>
  );
}
