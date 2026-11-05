import { createFileRoute, Link, redirect } from '@tanstack/react-router'
import { getSession } from '../auth'
import { api, type Order } from '../rentals'
import { panel, primaryButton } from '../components/access-layout'
import { PortalLayout, ErrorNotice, secondaryButton } from '../components/portal-layout'
import { OrderList } from '../components/order-list'
import { usePortalData } from '../components/use-portal-data'
export const Route = createFileRoute('/')({ beforeLoad: async () => {
  const profile = await getSession()
  if (!profile) throw redirect({ to: '/login' })
  return { profile }
}, component: Home })
const load = () => api<Order[]>('GET', '/orders')
function Home() {
  const { profile } = Route.useRouteContext()
  const { data, loading, error, reload } = usePortalData(load)
  const agent = profile.role === 'AGENTE'
  return <PortalLayout profile={profile} title="Visão geral">
    <div className="flex flex-wrap items-end justify-between gap-4"><div><p className="text-sm font-semibold uppercase tracking-widest text-brand">Portal de locações · {agent ? 'Agente' : 'Cliente'}</p><h1 className="mt-2 text-3xl font-bold">Olá, {profile.name}</h1><p className="mt-2 text-muted">{agent ? 'Acompanhe as solicitações e conduza as próximas etapas.' : 'Seu próximo aluguel começa aqui. Acompanhe cada etapa.'}</p></div><Link to={agent ? '/pedidos' : '/pedidos/novo'} className={primaryButton}>{agent ? 'Avaliar pedidos' : 'Novo pedido'} →</Link></div>
    <ErrorNotice message={error} />{error && <button className={secondaryButton} onClick={reload}>Tentar novamente</button>}
    {loading ? <p className="mt-8" role="status">Carregando visão geral…</p> : data && <>
      <div className="my-8 grid gap-4 sm:grid-cols-3">{[['Pedidos', data.length], ['Em análise', data.filter(o => o.status === 'EM_ANALISE').length], ['Contratados', data.filter(o => o.status === 'CONTRATADO').length]].map(([label, value]) => <div key={label} className={panel}><p className="text-muted">{label}</p><p className="mt-3 text-4xl font-bold">{value}</p></div>)}</div>
      <div className="mb-4 flex justify-between"><h2 className="text-xl font-bold">Pedidos recentes</h2><Link to="/pedidos" className="font-semibold text-brand">Ver todos</Link></div><OrderList orders={data.slice(0, 5)} agent={agent} />
    </>}
  </PortalLayout>
}
