import { Link, useLocation } from 'react-router-dom';
import type { PropsWithChildren } from 'react';

const links = [
  { to: '/', label: 'Operations Dashboard' },
  { to: '/risk', label: 'Risk Command Center' }
];

export function Layout({ children }: PropsWithChildren) {
  const location = useLocation();

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
          <div>
            <h1 className="text-lg font-semibold">Power Asset Intelligence</h1>
            <p className="text-xs text-slate-400">AI Grid Asset Control Plane</p>
          </div>
          <nav className="flex gap-2">
            {links.map((link) => (
              <Link
                key={link.to}
                to={link.to}
                className={`rounded-md px-3 py-2 text-sm ${
                  location.pathname === link.to
                    ? 'bg-brand-700 text-white'
                    : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                }`}
              >
                {link.label}
              </Link>
            ))}
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-7xl space-y-6 px-6 py-6">{children}</main>
    </div>
  );
}
