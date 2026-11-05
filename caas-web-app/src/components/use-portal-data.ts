import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { ApiError } from '../clients'
export function usePortalData<T>(load: () => Promise<T>) {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)
  const [attempt, setAttempt] = useState(0)
  const navigate = useNavigate()
  const reload = useCallback(() => setAttempt(n => n + 1), [])
  useEffect(() => {
    let active = true
    setLoading(true); setError('')
    load().then(value => { if (active) setData(value) }).catch(cause => {
      if (!active) return
      if (cause instanceof ApiError && cause.status === 401) void navigate({ to: '/login' })
      setError(cause instanceof Error ? cause.message : 'Não foi possível carregar os dados.')
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [load, attempt, navigate])
  return { data, error, loading, reload }
}
