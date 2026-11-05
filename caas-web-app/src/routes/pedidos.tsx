import { Outlet, createFileRoute, redirect } from '@tanstack/react-router'
import { getSession } from '../auth'
export const Route = createFileRoute('/pedidos')({ beforeLoad: async () => {
  const profile = await getSession()
  if (!profile) throw redirect({ to: '/login' })
  return { profile }
}, component: Outlet })
