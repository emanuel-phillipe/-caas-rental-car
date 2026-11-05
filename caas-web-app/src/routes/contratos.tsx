import { createFileRoute, Link, redirect } from '@tanstack/react-router'
import { getSession } from '../auth'
import { api, date, type Contract } from '../rentals'
import { panel } from '../components/access-layout'
import { PortalLayout, ErrorNotice, secondaryButton } from '../components/portal-layout'
import { usePortalData } from '../components/use-portal-data'
export const Route = createFileRoute('/contratos')({ beforeLoad: async () => { const profile = await getSession(); if (!profile) throw redirect({ to: '/login' }); return { profile } }, component: Contracts })
const load = () => api<Contract[]>('GET', '/contracts')
function Contracts() {
  const { profile } = Route.useRouteContext(), { data, error, loading, reload } = usePortalData(load)
  return <PortalLayout profile={profile} title="Contratos"><h1 className="text-3xl font-bold">Contratos de aluguel</h1><p className="mt-2 text-muted">Consulte as locações formalizadas e seus créditos associados.</p><ErrorNotice message={error} />{error && <button className={secondaryButton} onClick={reload}>Tentar novamente</button>}{loading ? <p role="status" className="mt-6">Carregando contratos…</p> : data && <div className="mt-6 space-y-4">{!data.length && <p className={panel + ' text-muted'}>Nenhum contrato formalizado. Os pedidos aprovados podem ser formalizados pelo agente responsável.</p>}{data.map(c => <article key={c.id} className={panel}><div className="flex flex-wrap items-start justify-between gap-4"><div><h2 className="text-lg font-bold">Locação · {date(c.createdAt)}</h2><p className="mt-1 break-all text-sm text-muted">{c.id}</p></div><Link to="/pedidos/$id" params={{ id: c.orderId }} className="font-semibold text-brand">Ver contrato e pedido →</Link></div><dl className="mt-5 grid gap-4 sm:grid-cols-3"><div><dt className="text-sm text-muted">Proprietário na formalização</dt><dd className="mt-1 font-semibold">{c.ownerName}</dd></div><div><dt className="text-sm text-muted">Titularidade</dt><dd className="mt-1 font-semibold">{c.ownerKind}</dd></div><div><dt className="text-sm text-muted">Crédito</dt><dd className="mt-1 font-semibold">{c.creditId ? 'Crédito vinculado' : 'Sem crédito'}</dd></div></dl></article>)}</div>}</PortalLayout>
}
