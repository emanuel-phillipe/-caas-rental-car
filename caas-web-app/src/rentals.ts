import { request } from './clients'
import type { Client } from './clients'
export const api = <T,>(method: string, path: string, body?: unknown) => request<T>(method, '/api' + path, body)
export type Organization = { id: string; name: string; kind: 'EMPRESA' | 'BANCO' }
export type Vehicle = { id: string; registration: string; year: number; brand: string; model: string; plate: string; ownerId: string; ownerName: string; ownerKind: string; organizationId: string; active: boolean }
export type Order = { id: string; clientId: string; clientName: string; vehicleId: string; vehicleName: string; plate: string; startsOn: string; endsOn: string; revision: number; status: string; createdAt: string }
export type Review = { id: string; revision: number; representative: string; result: string; notes: string; createdAt: string }
export type Contract = { id: string; orderId: string; kind: string; ownerId: string; ownerName: string; ownerKind: string; creditId: string | null; createdAt: string }
export type Credit = { id: string; reference: string; bankId: string; bankName: string; clientId: string; amount: number; linked: boolean }
export type Detail = { order: Order; vehicle: Vehicle; client: Client; reviews: Review[]; contract: Contract | null; credits: Credit[]; organizations: Organization[] }
export const statusNames: Record<string, string> = { EM_ANALISE: 'Em análise', APROVADO: 'Aprovado', REPROVADO: 'Reprovado', CANCELADO: 'Cancelado', CONTRATADO: 'Contratado' }
export const date = (value: string) => new Date(value.length === 10 ? value + 'T12:00:00' : value).toLocaleDateString('pt-BR')
export const money = (value: number) => value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
