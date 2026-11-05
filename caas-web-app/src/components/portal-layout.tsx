import { useState, type ReactNode } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'
import { AccessLayout } from './access-layout'
import { signOut, type Profile } from '../auth'
import { statusNames } from '../rentals'

export const inputClass = 'mt-1 w-full min-h-12 rounded-lg border border-line bg-white px-3 py-2 focus:outline-2 focus:outline-brand'
export const secondaryButton = 'inline-flex min-h-12 items-center justify-center rounded-lg border border-line bg-white px-4 py-2 font-semibold hover:bg-canvas disabled:opacity-50'
export function Status({ value }: { value: string }) {
  const color = value === 'APROVADO' || value === 'CONTRATADO' ? 'bg-green-50 text-green-800' : value === 'EM_ANALISE' ? 'bg-amber-50 text-amber-900' : 'bg-stone-100 text-stone-700'
  return <span className={'inline-flex rounded-full px-3 py-1 text-sm font-semibold ' + color}>{statusNames[value] ?? value}</span>
}
export function ErrorNotice({ message }: { message: string }) {
  return message ? <p role="alert" className="my-4 rounded-lg border border-red-200 bg-red-50 p-4 text-red-900">{message}</p> : null
}
export function PortalLayout({ profile, title, children }: { profile: Profile; title: string; children: ReactNode }) {
  const navigate = useNavigate()
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  return <AccessLayout title={title} action={<button disabled={busy} className="rounded-lg border border-white/50 px-4 py-2 disabled:opacity-50" onClick={async () => {
    setBusy(true)
    try { await signOut(); await navigate({ to: '/login' }) } catch { setError('Não foi possível sair. Tente novamente.') } finally { setBusy(false) }
  }}>{busy ? 'Saindo…' : 'Sair'}</button>}>
    <nav aria-label="Navegação principal" className="mb-8 flex flex-wrap gap-2 border-b border-line pb-5">
      {([{ to: '/', label: 'Visão geral' }, { to: '/pedidos', label: profile.role === 'AGENTE' ? 'Pedidos atribuídos' : 'Meus pedidos' }, { to: '/veiculos', label: 'Veículos' }, { to: '/contratos', label: 'Contratos' }, ...(profile.role === 'CLIENTE' ? [{ to: '/cliente', label: 'Meu cadastro' }] : [])] as const).map(item =>
        <Link key={item.to} to={item.to} activeOptions={{ exact: true }} activeProps={{ className: 'bg-brand-soft text-brand' }} className="rounded-lg px-3 py-2 font-semibold text-muted hover:bg-brand-soft">{item.label}</Link>)}
    </nav>
    <ErrorNotice message={error} />{children}
  </AccessLayout>
}
