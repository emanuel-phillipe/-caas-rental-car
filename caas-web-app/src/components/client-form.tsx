import { useState, type FormEvent } from 'react'
import type { ClientDetails } from '../clients'
import { primaryButton } from './access-layout'

const input = 'mt-2 min-h-12 w-full rounded-lg border border-line bg-canvas px-3.5 focus-visible:outline-2 focus-visible:outline-brand'
const blank: ClientDetails = { name: '', rg: '', cpf: '', address: '', profession: '', employments: [] }

export function ClientForm({ initial = blank, registration = false, onSave, onCancel }: {
  initial?: ClientDetails
  registration?: boolean
  onSave: (details: ClientDetails, username: string, password: string) => Promise<void>
  onCancel?: () => void
}) {
  const [details, setDetails] = useState(initial)
  const [jobs, setJobs] = useState(initial.employments.map(job => ({ employer: job.employer, income: String(job.income) })))
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy) return
    const data = new FormData(event.currentTarget)
    setBusy(true)
    setError('')
    try {
      await onSave({ ...details, employments: jobs.map(job => ({ employer: job.employer, income: Number(job.income) })) },
        String(data.get('username') ?? ''), String(data.get('password') ?? ''))
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Não foi possível salvar o cadastro.')
    } finally {
      setBusy(false)
    }
  }

  return <form onSubmit={submit} aria-busy={busy}>
    <fieldset disabled={busy} className="space-y-5 disabled:opacity-70">
      {registration && <div className="grid gap-5 sm:grid-cols-2">
        <label className="font-semibold">Usuário
          <input name="username" autoComplete="username" autoCapitalize="none" className={input} required maxLength={64}
            pattern="[a-zA-Z0-9][a-zA-Z0-9._\-]*" title="Use letras, números, ponto, hífen ou sublinhado." />
        </label>
        <label className="font-semibold">Senha
          <input name="password" type="password" autoComplete="new-password" className={input} required minLength={8} maxLength={72} />
          <span className="mt-1 block text-sm font-normal text-muted">Mínimo de 8 caracteres; limite de 72 bytes.</span>
        </label>
      </div>}
      <div className="grid gap-5 sm:grid-cols-2">
        {([
          ['name', 'Nome completo', 160], ['rg', 'RG', 30], ['cpf', 'CPF', 14],
          ['profession', 'Profissão', 120], ['address', 'Endereço completo', 300],
        ] as const).map(([key, label, max]) => <label key={key} className={'font-semibold ' + (key === 'address' ? 'sm:col-span-2' : '')}>
          {label}
          <input className={input} required maxLength={max} value={details[key]}
            autoComplete={key === 'name' ? 'name' : key === 'address' ? 'street-address' : 'off'}
            onChange={event => setDetails({ ...details, [key]: event.target.value })} />
        </label>)}
      </div>
      <section className="border-t border-line pt-5" aria-labelledby="jobs-title">
        <h2 id="jobs-title" className="text-lg font-semibold">Vínculos empregatícios</h2>
        <p className="mt-1 text-sm text-muted">Opcional. Informe até três empregadores e seus rendimentos mensais.</p>
        {jobs.map((job, index) => <div key={index} className="mt-4 grid items-end gap-3 rounded-lg border border-line p-4 sm:grid-cols-[1fr_1fr_auto]">
          <label className="font-semibold">Empregador {index + 1}
            <input className={input} required maxLength={160} value={job.employer}
              onChange={event => setJobs(jobs.map((item, i) => i === index ? { ...item, employer: event.target.value } : item))} />
          </label>
          <label className="font-semibold">Rendimento mensal (R$)
            <input className={input} required type="number" min="0" max="9999999999.99" step="0.01" value={job.income}
              onChange={event => setJobs(jobs.map((item, i) => i === index ? { ...item, income: event.target.value } : item))} />
          </label>
          <button type="button" className="min-h-12 rounded-lg px-3 text-red-800 underline" aria-label={'Remover vínculo ' + (index + 1)}
            onClick={() => setJobs(jobs.filter((_, i) => i !== index))}>Remover</button>
        </div>)}
        <button type="button" disabled={jobs.length >= 3} className="mt-4 min-h-10 rounded-lg border border-line px-4 font-semibold disabled:opacity-50"
          onClick={() => setJobs([...jobs, { employer: '', income: '' }])}>Adicionar vínculo ({jobs.length}/3)</button>
      </section>
      {error && <p role="alert" className="rounded-lg bg-red-50 p-3 text-red-900">{error}</p>}
      <div className="flex flex-wrap gap-3">
        <button className={primaryButton} type="submit">{busy ? 'Salvando…' : registration ? 'Criar minha conta' : 'Salvar alterações'}</button>
        {onCancel && <button type="button" className="min-h-12 rounded-lg border border-line px-4" onClick={onCancel}>Cancelar</button>}
      </div>
    </fieldset>
  </form>
}
