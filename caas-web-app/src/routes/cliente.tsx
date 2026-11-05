import { useEffect, useState } from 'react'
import { createFileRoute, redirect, useNavigate } from '@tanstack/react-router'
import { getSession } from '../auth'
import { ApiError, deleteClient, getClient, updateClient, type Client } from '../clients'
import { ClientForm } from '../components/client-form'
import { panel, primaryButton } from '../components/access-layout'
import { PortalLayout } from '../components/portal-layout'

export const Route = createFileRoute('/cliente')({
  beforeLoad: async () => {
    const profile = await getSession()
    if (!profile) throw redirect({ to: '/login' })
    if (profile.role !== 'CLIENTE') throw redirect({ to: '/' })
    return { profile }
  },
  component: ClientPage,
})

function ClientPage() {
  const { profile } = Route.useRouteContext()
  const navigate = useNavigate()
  const [client, setClient] = useState<Client | null>(null)
  const [editing, setEditing] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    getClient().then(result => { if (active) setClient(result) }).catch(cause => {
      if (!active) return
      if (cause instanceof ApiError && cause.status === 401) void navigate({ to: '/login' })
      setError(cause.message)
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [attempt, navigate])

  async function remove() {
    if (busy) return
    setBusy(true)
    setError('')
    try {
      await deleteClient()
      await navigate({ to: '/login', replace: true })
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Não foi possível excluir.')
    } finally { setBusy(false) }
  }

  return <PortalLayout profile={profile} title="Meu cadastro">
    <section className="mx-auto max-w-3xl">
      <h1 className="text-3xl font-bold">Meu cadastro</h1>
      <p className="mt-2 text-muted">Consulte e mantenha seus dados de cliente atualizados.</p>
      {notice && <p role="status" className="mt-4 rounded-lg bg-green-50 p-3 text-green-900">{notice}</p>}
      {error && <div role="alert" className="mt-4 rounded-lg bg-red-50 p-3 text-red-900">
        <p>{error}</p>
        {!client && <button className="mt-2 underline" onClick={() => setAttempt(attempt + 1)}>Tentar novamente</button>}
      </div>}
      <div className={panel + ' mt-6'}>
        {loading ? <p role="status">Carregando cadastro…</p> : client && (editing ?
          <ClientForm initial={client} onCancel={() => setEditing(false)} onSave={async details => {
            try {
              const updated = await updateClient(details)
              setClient(updated)
              setEditing(false)
              setNotice('Cadastro atualizado com sucesso.')
            } catch (cause) {
              if (cause instanceof ApiError && cause.status === 401) void navigate({ to: '/login' })
              throw cause
            }
          }} /> : <>
            <dl className="grid gap-5 sm:grid-cols-2">
              {([
                ['Usuário', client.username], ['Nome', client.name], ['RG', client.rg], ['CPF', client.cpf],
                ['Endereço', client.address], ['Profissão', client.profession],
              ]).map(([label, value]) => <div key={label}>
                <dt className="text-sm text-muted">{label}</dt><dd className="mt-1 break-words font-semibold">{value}</dd>
              </div>)}
            </dl>
            <h2 className="mt-6 border-t border-line pt-5 text-lg font-semibold">Vínculos empregatícios</h2>
            {client.employments.length === 0 ? <p className="mt-2 text-muted">Nenhum vínculo informado.</p> :
              <ul className="mt-3 space-y-3">{client.employments.map((job, index) => <li key={index} className="flex flex-wrap justify-between gap-2 rounded-lg bg-canvas p-3">
                <span className="break-words">{job.employer}</span>
                <span>{job.income.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })}/mês</span>
              </li>)}</ul>}
            <div className="mt-6 flex flex-wrap gap-3">
              <button className={primaryButton} onClick={() => { setEditing(true); setConfirming(false); setNotice(''); setError('') }}>Editar cadastro</button>
              <button className="min-h-12 rounded-lg border border-red-200 px-4 font-semibold text-red-800" onClick={() => { setConfirming(true); setNotice('') }}>Excluir minha conta</button>
            </div>
            {confirming && <section aria-label="Confirmar exclusão" className="mt-5 rounded-lg border border-red-200 bg-red-50 p-4">
              <h2 className="font-bold text-red-900">Excluir sua conta permanentemente?</h2>
              <p className="mt-2">Seu cadastro, vínculos e acesso serão removidos. Contas com pedidos registrados não podem ser excluídas, para preservar o histórico. Esta ação não pode ser desfeita.</p>
              <div className="mt-4 flex flex-wrap gap-3">
                <button disabled={busy} className="min-h-12 rounded-lg bg-red-800 px-4 font-semibold text-white disabled:opacity-60" onClick={remove}>{busy ? 'Excluindo…' : 'Confirmar exclusão'}</button>
                <button disabled={busy} className="min-h-12 rounded-lg border border-line px-4" onClick={() => setConfirming(false)}>Cancelar</button>
              </div>
            </section>}
          </>)}
      </div>
    </section>
  </PortalLayout>
}
