import { Link } from 'react-router-dom';
import type { Asset } from '../types/api';

export function AssetTable({ assets }: { assets: Asset[] }) {
  return (
    <div className="overflow-hidden rounded-xl border border-slate-800 bg-slate-900">
      <table className="min-w-full text-sm">
        <thead className="bg-slate-800 text-left text-slate-300">
          <tr>
            <th className="px-4 py-3">Asset</th><th className="px-4 py-3">Type</th><th className="px-4 py-3">Status</th><th className="px-4 py-3">Criticality</th><th className="px-4 py-3">Location</th>
          </tr>
        </thead>
        <tbody>
          {assets.map((asset) => (
            <tr key={asset.id} className="border-t border-slate-800">
              <td className="px-4 py-3 font-medium text-brand-50"><Link to={`/assets/${asset.id}`}>{asset.name}</Link></td>
              <td className="px-4 py-3">{asset.type}</td><td className="px-4 py-3">{asset.status}</td><td className="px-4 py-3">{asset.criticality}</td><td className="px-4 py-3">{asset.location}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
