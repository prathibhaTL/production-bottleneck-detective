/**
 * api.js – central place for all HTTP calls to the backend.
 *
 * Using the Vite proxy, all calls to /api/* are forwarded to
 * http://localhost:8080/api/* automatically.
 */

const BASE = '/api'

async function request(method, path, body) {
  const options = {
    method,
    headers: { 'Content-Type': 'application/json' },
  }
  if (body) options.body = JSON.stringify(body)
  const res = await fetch(`${BASE}${path}`, options)
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }))
    throw new Error(err.message || `HTTP ${res.status}`)
  }
  // 204 No Content has no body
  if (res.status === 204) return null
  return res.json()
}

// ── Production Lines ──────────────────────────────────────────────────────
export const getProductionLines  = ()      => request('GET',    '/production-lines')
export const getProductionLine   = (id)    => request('GET',    `/production-lines/${id}`)
export const createProductionLine= (data)  => request('POST',   '/production-lines', data)
export const updateProductionLine= (id, d) => request('PUT',    `/production-lines/${id}`, d)
export const deleteProductionLine= (id)    => request('DELETE', `/production-lines/${id}`)

// ── Machines ──────────────────────────────────────────────────────────────
export const getMachines   = ()      => request('GET',    '/machines')
export const createMachine = (data)  => request('POST',   '/machines', data)
export const updateMachine = (id, d) => request('PUT',    `/machines/${id}`, d)
export const deleteMachine = (id)    => request('DELETE', `/machines/${id}`)

// ── Stages ────────────────────────────────────────────────────────────────
export const getStages     = (lineId) => request('GET',  `/stages?lineId=${lineId}`)
export const createStage   = (data)   => request('POST', '/stages', data)
export const updateStage   = (id, d)  => request('PUT',  `/stages/${id}`, d)
export const deleteStage   = (id)     => request('DELETE',`/stages/${id}`)

// ── Production Records ────────────────────────────────────────────────────
export const getRecordsByLine = (lineId)  => request('GET',  `/records?lineId=${lineId}`)
export const createRecord     = (data)    => request('POST', '/records', data)
export const deleteRecord     = (id)      => request('DELETE',`/records/${id}`)

// ── Bottleneck Analysis ───────────────────────────────────────────────────
export const runAnalysis = (lineId, hoursBack = 48) =>
  request('GET', `/analysis/bottleneck/${lineId}?hoursBack=${hoursBack}`)

// ── What-If Simulator ─────────────────────────────────────────────────────
export const runWhatIf = (lineId, data) =>
  request('POST', `/simulation/what-if/${lineId}`, data)

// ── Factory Simulator ─────────────────────────────────────────────────────
export const runFactorySimulator = (lineId, periods = 24) =>
  request('POST', `/simulator/run/${lineId}?periods=${periods}`)

// ── Dashboard ─────────────────────────────────────────────────────────────
export const getDashboard = (lineId) => request('GET', `/dashboard/${lineId}`)
