export type Employment = { employer: string; income: number }
export type ClientDetails = {
  name: string
  rg: string
  cpf: string
  address: string
  profession: string
  employments: Employment[]
}
export type Client = ClientDetails & { id: string; username: string }

export class ApiError extends Error {
  constructor(public status: number, message: string) { super(message) }
}

async function fetchApi(path: string, options: RequestInit): Promise<Response> {
  try {
    return await fetch(path, options)
  } catch {
    throw new Error('Não foi possível conectar à API. Verifique se o backend está em execução e tente novamente.')
  }
}

export async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {}
  if (method !== 'GET') {
    const csrf = await fetchApi('/api/auth/csrf', { cache: 'no-store' })
    if (!csrf.ok) throw new Error('Não foi possível iniciar a solicitação.')
    const { headerName, token } = await csrf.json()
    headers[headerName] = token
  }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const response = await fetchApi(path, {
    method, headers, body: body === undefined ? undefined : JSON.stringify(body), cache: 'no-store',
  })
  if (!response.ok) {
    const error = await response.json().catch(() => null)
    throw new ApiError(response.status, error?.message || (
      response.status === 401 ? 'Sua sessão expirou. Entre novamente.' :
        response.status === 403 ? 'Acesso recusado. Atualize a página e tente novamente.' :
          'Não foi possível concluir a solicitação. Tente novamente.'))
  }
  return response.status === 204 ? undefined as T : response.json()
}

export const getClient = () => request<Client>('GET', '/api/clients/me')
export const createClient = (username: string, password: string, details: ClientDetails) =>
  request<Client>('POST', '/api/clients', { username, password, details })
export const updateClient = (details: ClientDetails) => request<Client>('PUT', '/api/clients/me', details)
export const deleteClient = () => request<void>('DELETE', '/api/clients/me')
