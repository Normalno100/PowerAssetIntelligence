import { useEffect, useState, useCallback } from 'react';
import { api } from '../services/api';
import type { RiskAssessment } from '../types/api';
import { LoadingState } from '../components/ui/LoadingState';
import { ErrorState } from '../components/ui/ErrorState';
import { EmptyState } from '../components/ui/EmptyState';

export function RiskPage() {
  const [topRisk, setTopRisk] = useState<RiskAssessment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(() => {
    setLoading(true);
    setError(null);
    api.getTopRisks()
      .then((res) => setTopRisk(res.content))
      .catch((err: Error) => {
        setError(err.message ?? 'Не удалось загрузить данные о рисках');
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  if (loading) {
    return <LoadingState message="Загрузка списка рисков…" />;
  }

  if (error) {
    return <ErrorState message={error} onRetry={loadData} />;
  }

  if (topRisk.length === 0) {
    return <EmptyState message="Оценок рисков нет" description="Оценки рисков появятся после запуска ML-модели." />;
  }

  return (
    <section className="rounded-xl border border-slate-800 bg-slate-900 p-4">
      <h2 className="mb-4 text-lg font-semibold">Risk Command Center</h2>
      <div className="space-y-3">
        {topRisk.map((risk) => (
          <div key={risk.id} className="flex items-center justify-between rounded-lg border border-slate-700 p-3">
            <div><p className="text-sm text-slate-300">Asset: {risk.assetId}</p><p className="text-xs text-slate-500">{new Date(risk.assessedAt).toLocaleString()}</p></div>
            <div className="text-right"><p className="text-sm font-semibold">{risk.riskLevel}</p><p className="text-xl font-bold text-red-400">{risk.riskScore.toFixed(1)}</p></div>
          </div>
        ))}
      </div>
    </section>
  );
}
