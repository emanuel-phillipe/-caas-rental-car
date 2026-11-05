import { useState, type FormEvent } from 'react'
import { createFileRoute, Link, redirect, useNavigate } from '@tanstack/react-router'
import { getSession, signIn } from '../auth'
import { AccessLayout, Icon, panel, primaryButton } from '../components/access-layout'

export const Route = createFileRoute('/login')({
  beforeLoad: async ({ abortController }) => {
    if (await getSession(abortController.signal)) throw redirect({ to: '/', replace: true })
  },
  component: Login,
})

const input = 'mt-2 min-h-12 w-full rounded-lg border border-line bg-canvas px-3.5 text-base placeholder:text-muted focus-visible:border-brand focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-brand'

function Login() {
  const navigate = useNavigate()
  const [visible, setVisible] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy) return

    const form = new FormData(event.currentTarget)
    setBusy(true)
    setError('')
    try {
      const signedIn = await signIn(String(form.get('username')), String(form.get('password')))
      if (!signedIn) {
        setError('Usuário ou senha inválidos.')
        return
      }
      await navigate({ to: '/', replace: true })
    } catch {
      setError('Não foi possível entrar agora. Tente novamente.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <AccessLayout title="Acesso ao sistema">
      <div className="grid items-start gap-6 md:grid-cols-[1.45fr_1fr]">
        <section>
          <h1 className="text-3xl font-bold tracking-tight">Entrar no portal</h1>
          <p className="mt-2 text-muted">Use seu usuário e sua senha para acessar o sistema de locações.</p>

          <form method="post" className={panel + ' mt-6'} onSubmit={handleSubmit} aria-busy={busy}>
            <div>
              <label className="text-sm font-semibold" htmlFor="username">Usuário</label>
              <input className={input} id="username" name="username" autoComplete="username"
                autoCapitalize="none" spellCheck={false} placeholder="Digite seu usuário" maxLength={64} required />
            </div>
            <div className="mt-5">
              <label className="text-sm font-semibold" htmlFor="password">Senha</label>
              <div className="relative">
                <input className={input + ' pr-14'} id="password" name="password" type={visible ? 'text' : 'password'}
                  autoComplete="current-password" placeholder="Digite sua senha" required />
                <button type="button" aria-label={visible ? 'Ocultar senha' : 'Mostrar senha'} aria-controls="password"
                  className="absolute top-2 right-1.5 inline-flex size-12 items-center justify-center rounded-lg text-muted hover:text-brand focus-visible:outline-2 focus-visible:outline-brand"
                  onClick={() => setVisible(!visible)}>
                  <Icon name={visible ? 'eyeOff' : 'eye'} />
                </button>
              </div>
            </div>
            {error && <p role="alert" className="mt-5 rounded-lg border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-red-900">{error}</p>}
            <button className={primaryButton + ' mt-6 w-full'} type="submit" disabled={busy}>
              {busy ? 'Entrando…' : 'Entrar'} <Icon name="arrow" className="size-4" />
            </button>
          </form>
          <p className="mt-5 text-muted">Ainda não possui conta? <Link to="/cadastro" className="font-semibold text-brand underline">Cadastre-se</Link></p>
        </section>

        <aside className={panel}>
          <h2 className="text-xl font-bold tracking-tight">Perfis de acesso</h2>
          <p className="mt-2 text-sm leading-relaxed text-muted">Seu perfil é definido na conta cadastrada.</p>
          <ul className="mt-5 space-y-3">
            <AccessProfile icon="user" name="Cliente">Solicitação e acompanhamento de pedidos de aluguel.</AccessProfile>
            <AccessProfile icon="building" name="Agente">Análise de pedidos por representantes de empresas e bancos.</AccessProfile>
          </ul>
          <p className="mt-5 flex items-center gap-2 border-t border-line pt-4 text-sm text-muted">
            <Icon name="lock" className="size-4 shrink-0 text-brand" /> Acesso exclusivo a contas cadastradas.
          </p>
        </aside>
      </div>
    </AccessLayout>
  )
}

function AccessProfile({ icon, name, children }: { icon: 'user' | 'building'; name: string; children: string }) {
  return (
    <li className="flex items-start gap-3 rounded-xl border border-line p-3.5">
      <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-brand-soft text-brand">
        <Icon name={icon} />
      </span>
      <div>
        <h3 className="text-sm font-semibold">{name}</h3>
        <p className="mt-0.5 text-sm leading-normal text-muted">{children}</p>
      </div>
    </li>
  )
}
