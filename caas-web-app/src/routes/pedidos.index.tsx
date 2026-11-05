import { useState } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { api, statusNames, type Order } from '../rentals'
import { primaryButton } from '../components/access-layout'
import { PortalLayout, ErrorNotice, inputClass, secondaryButton } from '../components/portal-layout'
import { OrderList } from '../components/order-list'
import { usePortalData } from '../components/use-portal-data'
export const Route = createFileRoute('/pedidos/')({ component: Orders })
const load = () => api<Order[]>('GET', '/orders')
function Orders() {
  const { profile } = Route.useRouteContext()
  const { data, error, loading, reload } = usePortalData(load)
  const [search, setSearch] = useState(''), [status, setStatus] = useState('')
  const agent = profile.role === 'AGENTE'
  const filtered = (data ?? []).filter(o => (!status || o.status === status) && `${o.vehicleName} ${o.plate} ${o.clientName} ${o.id}`.toLowerCase().includes(search.toLowerCase()))
  return <PortalLayout profile={profile} title="Pedidos">
    <div className="flex flex-wrap items-center justify-between gap-4"><h1 className="text-3xl font-bold">{agent ? 'Pedidos atribuídos' : 'Meus pedidos'}</h1>{!agent && <Link to="/pedidos/novo" className={primaryButton}>Novo pedido +</Link>}</div>
    <p className="mt-2 text-muted">Consulte o andamento e abra um pedido para ver os detalhes.</p>
    <div className="my-6 grid gap-4 sm:grid-cols-[2fr_1fr]"><label className="font-medium">Buscar<input className={inputClass} placeholder="Veículo, placa ou identificação" value={search} onChange={e => setSearch(e.target.value)} /></label><label className="font-medium">Situação<select className={inputClass} value={status} onChange={e => setStatus(e.target.value)}><option value="">Todas</option>{Object.entries(statusNames).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label></div>
    <ErrorNotice message={error} />{error && <button onClick={reload} className={secondaryButton}>Tentar novamente</button>}
    {loading ? <p role="status">Carregando pedidos…</p> : data && <OrderList orders={filtered} agent={agent} />}
  </PortalLayout>
}
