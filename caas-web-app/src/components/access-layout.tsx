import type { ReactNode } from 'react'

const paths = {
  car: <><path d="m4 11 2-5h12l2 5M5 18v2m14-2v2" /><rect x="3" y="11" width="18" height="7" rx="2" /><path d="M7 14.5h.01m9.99 0h.01" /></>,
  user: <><circle cx="12" cy="8" r="3" /><path d="M5 20v-2a4 4 0 0 1 4-4h6a4 4 0 0 1 4 4v2" /></>,
  building: <path d="M4 21h16M6 21V4h12v17M10 21v-4h4v4m-4-13h.01m3.99 0h.01M10 12h.01m3.99 0h.01" />,
  lock: <><rect x="5" y="10" width="14" height="11" rx="2" /><path d="M8 10V7a4 4 0 1 1 8 0v3m-4 5v2" /></>,
  eye: <><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6Z" /><circle cx="12" cy="12" r="2.5" /></>,
  eyeOff: <path d="m3 3 18 18M10.5 6.1 12 6c6.5 0 10 6 10 6a16 16 0 0 1-3.3 3.6M6.4 7.6A16 16 0 0 0 2 12s3.5 6 10 6c1.4 0 2.7-.3 3.8-.7" />,
  exit: <path d="M10 4H5v16h5m-1-8h12m-4-4 4 4-4 4" />,
  arrow: <path d="M4 12h15m-6-6 6 6-6 6" />,
}

export function Icon({ name, className = 'size-5' }: { name: keyof typeof paths; className?: string }) {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7"
      strokeLinecap="round" strokeLinejoin="round" className={className}>
      {paths[name]}
    </svg>
  )
}

export function AccessLayout({ title, action, children }: { title: string; action?: ReactNode; children: ReactNode }) {
  return (
    <div className="flex min-h-dvh flex-col bg-canvas font-sans text-ink">
      <header className="bg-brand text-white">
        <div className="mx-auto grid min-h-18 max-w-[100rem] grid-cols-[1fr_auto] items-center gap-4 px-5 sm:grid-cols-[1fr_auto_1fr] sm:px-8">
          <div className="flex items-center gap-2.5 font-bold tracking-tight sm:text-lg">
            <Icon name="car" className="size-6 shrink-0" />
            <span>Caas Car Rental</span>
          </div>
          <span className="hidden font-semibold sm:block sm:text-lg">{title}</span>
          <div className="justify-self-end text-right text-sm font-medium">{action ?? 'Portal de locações'}</div>
        </div>
      </header>
      <main className="mx-auto w-full max-w-5xl flex-1 px-5 py-9 sm:px-8 sm:py-13">{children}</main>
      <footer className="border-t border-line">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-2 px-5 py-5 text-sm text-muted sm:px-8">
          <span className="font-medium">Caas Car Rental</span>
          <span className="flex items-center gap-2"><Icon name="lock" className="size-4" /> Acesso restrito</span>
        </div>
      </footer>
    </div>
  )
}

export const panel = 'rounded-2xl border border-line bg-white p-6 sm:p-8'
export const primaryButton = 'inline-flex min-h-12 items-center justify-center gap-2 rounded-lg bg-brand px-5 py-2.5 font-semibold text-white hover:bg-brand-hover focus-visible:outline-2 focus-visible:outline-offset-3 focus-visible:outline-brand disabled:cursor-wait disabled:opacity-65'
