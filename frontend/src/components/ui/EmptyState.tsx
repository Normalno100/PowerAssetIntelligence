export function EmptyState({
  message = 'Данные отсутствуют',
  description,
}: {
  message?: string;
  description?: string;
}) {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-slate-800 bg-slate-900 p-12">
      <svg
        className="mb-4 h-10 w-10 text-slate-600"
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
        strokeWidth={1.5}
        stroke="currentColor"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"
        />
      </svg>
      <p className="text-sm font-medium text-slate-400">{message}</p>
      {description && (
        <p className="mt-1 max-w-md text-center text-xs text-slate-500">
          {description}
        </p>
      )}
    </div>
  );
}
