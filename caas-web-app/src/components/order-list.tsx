import { Link } from '@tanstack/react-router'
import { panel } from './access-layout'
import { Status } from './portal-layout'
import { date, type Order } from '../rentals'
export function OrderList({ orders, agent }: { orders: Order[]; agent: boolean }) {
  if (!orders.length) return <div className={panel + ' text-center'}><h2 className="font-semibold">Nenhum pedido encontrado</h2><p className="mt-2 text-muted">{agent ? 'Os pedidos atribuídos à sua organização aparecerão aqui.' : 'Escolha um veículo para iniciar uma solicitação de aluguel.'}</p></div>
  return <div className="space-y-3">{orders.map(order => <Link key={order.id} to="/pedidos/$id" params={{ id: order.id }} className={panel + ' flex flex-wrap items-center justify-between gap-4 transition hover:border-brand'}>
    <div><p className="font-bold">{order.vehicleName} <span className="ml-2 text-sm font-normal text-muted">{order.plate}</span></p><p className="mt-1 text-sm text-muted">{date(order.startsOn)} até {date(order.endsOn)} · Revisão {order.revision}</p>{agent && <p className="mt-2 text-sm">{order.clientName}</p>}</div>
    <div className="flex items-center gap-3"><Status value={order.status} /><span aria-hidden="true">→</span></div>
  </Link>)}</div>
}
