import { useCallback, useState } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { api, date, money, type Detail, type Vehicle, type Organization } from '../rentals'
import { panel, primaryButton } from '../components/access-layout'
import { PortalLayout, ErrorNotice, Status, inputClass, secondaryButton } from '../components/portal-layout'
import { OrderForm } from '../components/order-form'
import { usePortalData } from '../components/use-portal-data'
export const Route = createFileRoute('/pedidos/$id')({ component: OrderDetail })
function OrderDetail() {
  const { profile } = Route.useRouteContext(), { id } = Route.useParams()
  const agent = profile.role === 'AGENTE'
  const load = useCallback(async () => {
    const detail = await api<Detail>('GET', '/orders/' + id)
    const [vehicles, organization, banks] = await Promise.all([api<Vehicle[]>('GET', '/vehicles'), agent ? api<Organization>('GET', '/organization') : Promise.resolve(null), agent ? api<Organization[]>('GET', '/banks') : Promise.resolve([])])
    return { ...detail, vehicles, organization, banks }
  }, [id, agent])
  const { data, error, loading, reload } = usePortalData(load)
  const [action, setAction] = useState(''), [busy, setBusy] = useState(false), [actionError, setActionError] = useState(''), [notice, setNotice] = useState('')
  const [bankId, setBankId] = useState('')
  const [result, setResult] = useState('FAVORAVEL'), [notes, setNotes] = useState(''), [creditId, setCreditId] = useState(''), [reference, setReference] = useState(''), [amount, setAmount] = useState('')
  async function mutate(path: string, body: unknown) {
    if (busy) return
    setBusy(true); setActionError(''); setNotice('')
    try { await api('POST', path, body); setAction(''); setNotes(''); setReference(''); setAmount(''); setNotice('Operação realizada com sucesso.'); reload() }
    catch (cause) { setActionError(cause instanceof Error ? cause.message : 'Não foi possível concluir a operação.') }
    finally { setBusy(false) }
  }
  const open = (value: string) => { setAction(value); setActionError(''); setNotice('') }
  const order = data?.order
  const ended = order?.status === 'CANCELADO' || order?.status === 'CONTRATADO'
  return <PortalLayout profile={profile} title="Detalhes do pedido">
    <Link to="/pedidos" className="font-semibold text-brand">← Voltar aos pedidos</Link>
    <ErrorNotice message={error} />{error && <button className={secondaryButton} onClick={reload}>Atualizar dados</button>}
    {notice && <p role="status" className="my-4 rounded-lg bg-green-50 p-4 text-green-900">{notice}</p>}
    {loading ? <p className="mt-6" role="status">Carregando pedido…</p> : data && order && <>
      <div className="my-6 flex flex-wrap items-center justify-between gap-4"><div><h1 className="text-3xl font-bold">{order.vehicleName}</h1><p className="mt-2 break-all text-sm text-muted">Pedido {id} · Revisão {order.revision}</p></div><Status value={order.status} /></div>
      <section className={panel}><dl className="grid gap-5 sm:grid-cols-3">{[['Retirada', date(order.startsOn)], ['Devolução', date(order.endsOn)], ['Placa', order.plate], ['Matrícula', data.vehicle.registration], ['Proprietário atual', data.vehicle.ownerName], ['Solicitado em', date(order.createdAt)]].map(([label, value]) => <div key={label}><dt className="text-sm text-muted">{label}</dt><dd className="mt-1 font-semibold">{value}</dd></div>)}</dl>
        {!ended && <div className="mt-6 flex flex-wrap gap-3 border-t border-line pt-5"><button className={secondaryButton} onClick={() => open('edit')}>Alterar pedido</button>{!agent && <button className={secondaryButton + ' text-red-800'} onClick={() => open('cancel')}>Cancelar pedido</button>}{agent && order.status === 'EM_ANALISE' && <button className={primaryButton} onClick={() => open('review')}>Registrar parecer</button>}{agent && order.status === 'APROVADO' && <button className={primaryButton} onClick={() => open('contract')}>Formalizar contrato</button>}{data.organization?.id === data.vehicle.organizationId && <button className={secondaryButton} onClick={() => open('bank')}>Atribuir banco</button>}{data.organization?.kind === 'BANCO' && <button className={secondaryButton} onClick={() => open('credit')}>Registrar crédito concedido</button>}</div>}
      </section>
      {!ended && action && <section className={panel + ' mt-6'} aria-label="Ação do pedido">
        <h2 className="mb-5 text-xl font-bold">{{ edit: 'Alterar pedido', cancel: 'Confirmar cancelamento', review: 'Parecer financeiro', contract: 'Formalizar contrato', credit: 'Crédito concedido', bank: 'Atribuir banco ao pedido' }[action]}</h2>
        <ErrorNotice message={actionError} />
        {action === 'edit' ? <OrderForm initial={order} vehicles={[data.vehicle, ...data.vehicles.filter(v => v.id !== data.vehicle.id && v.organizationId === data.vehicle.organizationId)]} onCancel={() => open('')} onSave={async input => { await api('PUT', '/orders/' + id, input); open(''); setNotice('Pedido atualizado e reenviado para análise.'); reload() }} /> :
          <form onSubmit={e => { e.preventDefault(); if (action === 'bank') void mutate('/orders/' + id + '/bank', { bankId }); if (action === 'cancel') void mutate('/orders/' + id + '/cancel', { revision: order.revision }); if (action === 'review') void mutate('/orders/' + id + '/reviews', { revision: order.revision, result, notes }); if (action === 'contract') void mutate('/orders/' + id + '/contract', { revision: order.revision, kind: 'LOCACAO_' + data.vehicle.ownerKind, creditId: creditId || null }); if (action === 'credit') void mutate('/credits', { orderId: id, reference, amount: Number(amount) }) }}>
            <fieldset disabled={busy} className="space-y-5">
              {action === 'bank' && <><p className="text-muted">O banco selecionado terá acesso aos dados e rendimentos do contratante para avaliar o pedido e registrar crédito concedido.</p><label className="block font-medium">Banco<select required className={inputClass} value={bankId} onChange={e => setBankId(e.target.value)}><option value="">Selecione um banco</option>{data.banks.filter(b => !data.organizations.some(o => o.id === b.id)).map(b => <option key={b.id} value={b.id}>{b.name}</option>)}</select></label></>}
              {action === 'cancel' && <p>O pedido será encerrado e permanecerá no histórico. Esta ação não pode ser desfeita.</p>}
              {action === 'review' && <><p className="text-muted">O parecer se aplica à revisão {order.revision}. Confira os dados de identificação e renda abaixo antes de concluir.</p><label className="block font-medium">Resultado<select className={inputClass} value={result} onChange={e => setResult(e.target.value)}><option value="FAVORAVEL">Favorável</option><option value="DESFAVORAVEL">Desfavorável</option></select></label><label className="block font-medium">Justificativa<textarea required maxLength={2000} rows={4} className={inputClass} value={notes} onChange={e => setNotes(e.target.value)} /></label></>}
              {action === 'contract' && <><p>Contrato de locação com titularidade de {data.vehicle.ownerKind.toLowerCase()}. O proprietário registrado será <strong>{data.vehicle.ownerName}</strong>.</p><p className="text-sm text-muted">A formalização confirma o período e encerra as alterações do pedido.</p><label className="block font-medium">Crédito associado (opcional)<select className={inputClass} value={creditId} onChange={e => setCreditId(e.target.value)}><option value="">Sem crédito</option>{data.credits.filter(c => !c.linked).map(c => <option value={c.id} key={c.id}>{c.reference} · {c.bankName} · {money(c.amount)}</option>)}</select></label></>}
              {action === 'credit' && <><p className="text-muted">Registre somente um crédito já concedido pelo seu banco ao cliente deste pedido.</p><label className="block font-medium">Referência do contrato de crédito<input required maxLength={100} className={inputClass} value={reference} onChange={e => setReference(e.target.value)} /></label><label className="block font-medium">Valor concedido (R$)<input required type="number" min="0.01" max="9999999999.99" step="0.01" className={inputClass} value={amount} onChange={e => setAmount(e.target.value)} /></label></>}
              <div className="flex flex-wrap gap-3"><button className={primaryButton}>{busy ? 'Processando…' : 'Confirmar'}</button><button type="button" className={secondaryButton} onClick={() => open('')}>Voltar</button></div>
            </fieldset>
          </form>}
      </section>}
      {agent && <section className={panel + ' mt-6'}><h2 className="text-xl font-bold">Dados do contratante</h2><dl className="mt-5 grid gap-4 sm:grid-cols-2">{[['Nome', data.client.name], ['CPF', data.client.cpf], ['RG', data.client.rg], ['Profissão', data.client.profession], ['Endereço', data.client.address]].map(([label, value]) => <div key={label}><dt className="text-sm text-muted">{label}</dt><dd className="mt-1 break-words font-medium">{value}</dd></div>)}</dl><h3 className="mt-5 font-bold">Rendimentos informados</h3>{data.client.employments.length ? <ul className="mt-3 space-y-2">{data.client.employments.map((job, index) => <li key={index} className="flex flex-wrap justify-between gap-2 rounded-lg bg-canvas p-3"><span>{job.employer}</span><strong>{money(job.income)}/mês</strong></li>)}</ul> : <p className="mt-2 text-muted">Nenhum vínculo empregatício informado.</p>}</section>}
      {data.contract && <section className={panel + ' mt-6 border-green-200'}><h2 className="text-xl font-bold">Contrato formalizado</h2><p className="mt-2 break-all text-sm text-muted">{data.contract.id}</p><dl className="mt-5 grid gap-4 sm:grid-cols-2">{[['Data', date(data.contract.createdAt)], ['Tipo', data.contract.kind.replaceAll('_', ' ')], ['Proprietário na formalização', data.contract.ownerName], ['Crédito', data.credits.find(c => c.id === data.contract?.creditId)?.reference ?? (data.contract.creditId || 'Sem crédito associado')]].map(([label, value]) => <div key={label}><dt className="text-sm text-muted">{label}</dt><dd className="mt-1 break-words font-semibold">{value}</dd></div>)}</dl></section>}
      <section className={panel + ' mt-6'}><h2 className="text-xl font-bold">Organizações responsáveis</h2><ul className="mt-3 space-y-2">{data.organizations.map(o => <li key={o.id}>{o.name} · {o.kind === 'BANCO' ? 'Banco' : 'Empresa'}</li>)}</ul></section>
      <section className={panel + ' mt-6'}><h2 className="text-xl font-bold">Histórico de pareceres</h2>{!data.reviews.length ? <p className="mt-3 text-muted">Ainda não há pareceres registrados.</p> : <ol className="mt-5 space-y-5">{data.reviews.map(review => <li key={review.id} className="border-l-2 border-brand pl-4"><p className="font-semibold">Revisão {review.revision} · {review.result === 'FAVORAVEL' ? 'Favorável' : 'Desfavorável'}{review.revision !== order.revision && <span className="ml-2 text-sm font-normal text-muted">Revisão anterior</span>}</p><p className="mt-1 text-sm text-muted">{review.representative} · {date(review.createdAt)}</p><p className="mt-2 whitespace-pre-wrap break-words">{review.notes}</p></li>)}</ol>}</section>
      {!!data.credits.length && <section className={panel + ' mt-6'}><h2 className="text-xl font-bold">Créditos concedidos</h2><ul className="mt-4 space-y-3">{data.credits.map(c => <li key={c.id} className="rounded-lg bg-canvas p-4"><p className="font-semibold">{c.reference} · {money(c.amount)}</p><p className="mt-1 text-sm text-muted">{c.bankName} · {c.linked ? 'Vinculado a contrato' : 'Disponível para vínculo'}</p></li>)}</ul></section>}
    </>}
  </PortalLayout>
}
