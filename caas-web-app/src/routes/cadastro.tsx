import { useState } from 'react'
import { createFileRoute, Link, redirect } from '@tanstack/react-router'
import { getSession } from '../auth'
import { createClient } from '../clients'
import { ClientForm } from '../components/client-form'
import { AccessLayout, panel, primaryButton } from '../components/access-layout'

export const Route = createFileRoute('/cadastro')({
  beforeLoad: async () => {
    if (await getSession()) throw redirect({ to: '/' })
  },
  component: Registration,
})

function Registration() {
  const [created, setCreated] = useState(false)
  return <AccessLayout title="Cadastro de cliente" action={<Link to="/login" className="underline">Entrar</Link>}>
    <section className="mx-auto max-w-3xl">
      <h1 className="text-3xl font-bold">{created ? 'Conta criada' : 'Cadastre-se como cliente'}</h1>
      <p className="mt-2 text-muted">{created ? 'Agora você pode entrar com o usuário e a senha cadastrados.' : 'Preencha seus dados para acessar o portal de locações.'}</p>
      <div className={panel + ' mt-6'}>
        {created ? <Link to="/login" className={primaryButton}>Ir para o login</Link> :
          <ClientForm registration onSave={async (details, username, password) => {
            await createClient(username, password, details)
            setCreated(true)
          }} />}
      </div>
    </section>
  </AccessLayout>
}
