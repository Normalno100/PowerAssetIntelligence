export function ErrorState({
  message,
  onRetry,
}: {
  message?: string;
  onRetry?: () => void;
}) {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-red-900/50 bg-slate-900 p-12">
      <svg
        className="mb-4 h-10 w-10 text-red-500"
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
        strokeWidth={1.5}
        stroke="currentColor"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z"
        />
      </svg>
      <p className="mb-4 max-w-md text-center text-sm font-medium text-slate-300">
        {message ?? 'Не удалось загрузить данные. Попробуйте ещё раз.'}
      </p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="rounded-lg bg-violet-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-violet-500"
        >
          Повторить
        </button>
      )}
    </div>
  );
}
