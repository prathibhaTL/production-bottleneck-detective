import React, { useState, useEffect } from 'react'
import { getMachines, createMachine, deleteMachine } from '../api'

export default function Machines() {
  const [machines, setMachines] = useState([])
  const [form, setForm]         = useState({ name: '', type: '', modelNumber: '', status: 'Running' })
  const [error, setError]       = useState(null)

  useEffect(() => { load() }, [])

  function load() { getMachines().then(setMachines).catch(e => setError(e.message)) }

  function add() {
    createMachine(form)
      .then(() => { load(); setForm({ name: '', type: '', modelNumber: '', status: 'Running' }) })
      .catch(e => setError(e.message))
  }

  function remove(id) {
    if (!window.confirm('Delete this machine?')) return
    deleteMachine(id).then(load).catch(e => setError(e.message))
  }

  const STATUS_COLORS = { Running: 'LOW', Down: 'CRITICAL', Maintenance: 'MEDIUM', Idle: 'HIGH' }

  return (
    <div>
      <div className="page-header">
        <h1>🔧 Machines</h1>
        <p>Register and manage machines assigned to production stages.</p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card">
        <h2>Add Machine</h2>
        <div className="form-row">
          <div className="form-group">
            <label>Name *</label>
            <input value={form.name} onChange={e => setForm(p=>({...p, name: e.target.value}))}
              placeholder="e.g. CNC-05" />
          </div>
          <div className="form-group">
            <label>Type</label>
            <input value={form.type} onChange={e => setForm(p=>({...p, type: e.target.value}))}
              placeholder="e.g. CNC Lathe" />
          </div>
          <div className="form-group">
            <label>Model Number</label>
            <input value={form.modelNumber} onChange={e => setForm(p=>({...p, modelNumber: e.target.value}))}
              placeholder="e.g. LT-2020" />
          </div>
          <div className="form-group">
            <label>Status</label>
            <select value={form.status} onChange={e => setForm(p=>({...p, status: e.target.value}))}>
              <option>Running</option><option>Down</option>
              <option>Maintenance</option><option>Idle</option>
            </select>
          </div>
          <div className="form-group" style={{ justifyContent: 'flex-end' }}>
            <button className="btn btn-primary" onClick={add} disabled={!form.name.trim()}>
              + Add Machine
            </button>
          </div>
        </div>
      </div>

      <div className="card">
        <h2>All Machines ({machines.length})</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr><th>Name</th><th>Type</th><th>Model</th><th>Status</th><th>Installed</th><th></th></tr>
            </thead>
            <tbody>
              {machines.map(m => (
                <tr key={m.id}>
                  <td><strong>{m.name}</strong></td>
                  <td>{m.type || '–'}</td>
                  <td>{m.modelNumber || '–'}</td>
                  <td><span className={`badge badge-${STATUS_COLORS[m.status] || 'LOW'}`}>{m.status}</span></td>
                  <td>{m.installedAt ? new Date(m.installedAt).toLocaleDateString() : '–'}</td>
                  <td>
                    <button className="btn btn-danger"
                      style={{ padding: '3px 10px', fontSize: 12 }}
                      onClick={() => remove(m.id)}>✕</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
