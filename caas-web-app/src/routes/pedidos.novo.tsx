import { createFileRoute, redirect, useNavigate } from '@tanstack/react-router'
import { api, type Vehicle, type Order } from '../rentals'
import { panel } from '../components/access-layout'
import { PortalLayout, ErrorNotice, secondaryButton } from '../components/portal-layout'
import { OrderForm } from '../components/order-form'
import { usePortalData } from '../components/use-portal-data'
export const Route = createFileRoute('/pedidos/novo')({ beforeLoad: ({ context }) => { if (context.profile.role !== 'CLIENTE') throw redirect({ to: '/pedidos' }) }, validateSearch: (search: Record<string, unknown>): { vehicle?: string } => ({ vehicle: typeof search.vehicle === 'string' ? search.vehicle : undefined }), component: NewOrder })
const load = () => api<Vehicle[]>('GET', '/vehicles')
function NewOrder() {
  const { profile } = Route.useRouteContext(), { vehicle } = Route.useSearch()
  const navigate = useNavigate(), { data, error, loading, reload } = usePortalData(load)
  return <PortalLayout profile={profile} title="Novo pedido"><h1 className="text-3xl font-bold">Solicitar aluguel</h1><p className="mt-2 text-muted">Escolha o veículo e o período desejado.</p><ErrorNotice message={error} />{error && <button className={secondaryButton} onClick={reload}>Tentar novamente</button>}<section className={panel + ' mt-6'}>{loading ? <p role="status">Carregando veículos…</p> : data && <>{!data.length && <p className="mb-5 text-muted">Não há veículos disponíveis para solicitar.</p>}<OrderForm vehicles={data} selectedVehicle={vehicle} onSave={async input => { const order = await api<Order>('POST', '/orders', input); await navigate({ to: '/pedidos/$id', params: { id: order.id } }) }} onCancel={() => void navigate({ to: '/pedidos' })} /></>}</section></PortalLayout>
}
