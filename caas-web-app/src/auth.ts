export type Profile = {
  id: string
  username: string
  name: string
  role: 'CLIENTE' | 'AGENTE'
}

export async function getSession(signal?: AbortSignal): Promise<Profile | null> {
  const response = await fetch('/api/auth/session', { signal, cache: 'no-store' })
  if (response.status === 401) return null
  if (!response.ok) throw new Error('Não foi possível consultar sua sessão.')
  return response.json()
}

async function postWithCsrf(path: 'login' | 'logout', body?: URLSearchParams) {
  const response = await fetch('/api/auth/csrf', { cache: 'no-store' })
  if (!response.ok) throw new Error('Não foi possível iniciar a solicitação. Tente novamente.')

  const { headerName, token }: { headerName: string; token: string } = await response.json()
  return fetch('/api/auth/' + path, { method: 'POST', headers: { [headerName]: token }, body })
}

export async function signIn(username: string, password: string) {
  const response = await postWithCsrf('login', new URLSearchParams({ username, password }))
  if (response.status === 401) return false
  if (!response.ok) throw new Error('Não foi possível entrar agora. Tente novamente.')
  return true
}

export async function signOut() {
  const response = await postWithCsrf('logout')
  if (!response.ok) throw new Error('Não foi possível sair agora. Tente novamente.')
}
