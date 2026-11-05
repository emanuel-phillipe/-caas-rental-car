import { useState } from 'react'
import { type Vehicle, type Order } from '../rentals'
import { primaryButton } from './access-layout'
import { ErrorNotice, inputClass, secondaryButton } from './portal-layout'
export function OrderForm({ vehicles, initial, selectedVehicle, onSave, onCancel }: { vehicles: Vehicle[]; initial?: Order; selectedVehicle?: string; onSave: (input: { vehicleId: string; startsOn: string; endsOn: string; revision?: number }) => Promise<void>; onCancel?: () => void }) {
  const [vehicleId, setVehicleId] = useState(initial?.vehicleId ?? selectedVehicle ?? '')
  const [startsOn, setStartsOn] = useState(initial?.startsOn ?? ''), [endsOn, setEndsOn] = useState(initial?.endsOn ?? '')
  const [busy, setBusy] = useState(false), [error, setError] = useState('')
  return <form onSubmit={async e => { e.preventDefault(); if (busy) return; setError(''); if (endsOn <= startsOn) { setError('A devolução deve ser posterior à retirada.'); return } setBusy(true); try { await onSave({ vehicleId, startsOn, endsOn, revision: initial?.revision }) } catch (cause) { setError(cause instanceof Error ? cause.message : 'Falha ao salvar pedido.') } finally { setBusy(false) } }}>
    <fieldset disabled={busy} className="space-y-5"><label className="block font-medium">Veículo<select required className={inputClass} value={vehicleId} onChange={e => setVehicleId(e.target.value)}><option value="">Selecione um veículo</option>{vehicles.filter(v => v.active).map(v => <option key={v.id} value={v.id}>{v.brand} {v.model} · {v.year} · {v.plate}</option>)}</select></label>
      <div className="grid gap-4 sm:grid-cols-2"><label className="font-medium">Retirada<input required type="date" className={inputClass} value={startsOn} onChange={e => setStartsOn(e.target.value)} /></label><label className="font-medium">Devolução<input required type="date" min={startsOn} className={inputClass} value={endsOn} onChange={e => setEndsOn(e.target.value)} /></label></div>
      <p className="text-sm text-muted">{initial ? 'Salvar cria uma nova revisão e exige uma nova avaliação financeira.' : 'O pedido será enviado para análise financeira. A reserva do veículo será confirmada na formalização do contrato.'}</p>
      <ErrorNotice message={error} /><div className="flex flex-wrap gap-3"><button className={primaryButton} disabled={!vehicles.some(v => v.active)}>{busy ? 'Salvando…' : initial ? 'Salvar alteração' : 'Enviar pedido'}</button>{onCancel && <button type="button" className={secondaryButton} onClick={onCancel}>Voltar</button>}</div>
    </fieldset>
  </form>
}
