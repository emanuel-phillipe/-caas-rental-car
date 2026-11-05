import { Outlet, createRootRoute, useRouter } from '@tanstack/react-router'
import { AccessLayout, panel, primaryButton } from '../components/access-layout'
import '../styles.css'

export const Route = createRootRoute({
  component: Outlet,
  errorComponent: SessionError,
  pendingComponent: Loading,
})

function Loading() {
  return (
    <AccessLayout title="Acesso ao sistema">
      <p className={panel} role="status">Verificando sua sessão…</p>
    </AccessLayout>
  )
}

function SessionError() {
  const router = useRouter()
  return (
    <AccessLayout title="Acesso ao sistema">
      <section className={panel}>
        <h1 className="text-2xl font-bold">Não foi possível conectar ao portal</h1>
        <p className="mt-2 text-muted">Tente novamente para verificar sua sessão.</p>
        <button type="button" className={primaryButton + ' mt-5'} onClick={() => void router.invalidate()}>
          Tentar novamente
        </button>
      </section>
    </AccessLayout>
  )
}
