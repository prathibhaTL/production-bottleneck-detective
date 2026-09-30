import React, { useState, useEffect } from 'react'
import {
  getProductionLines, createProductionLine, updateProductionLine, deleteProductionLine,
  getStages, createStage, deleteStage, getMachines
} from '../api'

export default function Lines() {
  const [lines, setLines]       = useState([])
  const [stages, setStages]     = useState([])
  const [machines, setMachines] = useState([])
  const [selectedLine, setSelected] = useState(null)
  const [error, setError]       = useState(null)

  // New line form
  const [newLine, setNewLine] = useState({ name: '', description: '', status: 'Active' })
  // New stage form
  const [newStage, setNewStage] = useState({
    name: '', stageOrder: 1, capacityPerHour: 60,
    standardProcessingTimeMinutes: 3, machineId: '', status: 'Active'
  })

  useEffect(() => { loadLines() }, [])
  useEffect(() => { getMachines().then(setMachines) }, [])
  useEffect(() => {
    if (selectedLine) getStages(selectedLine.id).then(setStages)
    else setStages([])
  }, [selectedLine])

  function loadLines() {
    getProductionLines().then(setLines).catch(e => setError(e.message))
  }

  function addLine() {
    createProductionLine(newLine)
      .then(() => { loadLines(); setNewLine({ name: '', description: '', status: 'Active' }) })
      .catch(e => setError(e.message))
  }

  function removeLine(id) {
    if (!window.confirm('Delete this production line and all its stages?')) return
    deleteProductionLine(id).then(loadLines).catch(e => setError(e.message))
  }

  function addStage() {
    const payload = {
      ...newStage,
      productionLineId: selectedLine.id,
      machineId: newStage.machineId ? Number(newStage.machineId) : null,
    }
    createStage(payload)
      .then(() => {
        getStages(selectedLine.id).then(setStages)
        setNewStage({ name: '', stageOrder: stages.length + 2, capacityPerHour: 60,
                      standardProcessingTimeMinutes: 3, machineId: '', status: 'Active' })
      })
      .catch(e => setError(e.message))
  }

  function removeStage(id) {
    deleteStage(id)
      .then(() => getStages(selectedLine.id).then(setStages))
      .catch(e => setError(e.message))
  }

  return (
    <div>
      <div className="page-header">
        <h1>⚙️ Production Lines & Stages</h1>
        <p>Create and manage production lines and their stages.</p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="two-col">
        {/* ── Left: lines list ── */}
        <div className="card">
          <h2>Production Lines</h2>

          {/* Add new line */}
          <div style={{ marginBottom: 16 }}>
            <div className="form-group" style={{ marginBottom: 8 }}>
              <label>Name</label>
              <input value={newLine.name}
                onChange={e => setNewLine(p => ({ ...p, name: e.target.value }))}
                placeholder="e.g. Line C – Plastics" />
            </div>
            <div className="form-group" style={{ marginBottom: 8 }}>
              <label>Description</label>
              <input value={newLine.description}
                onChange={e => setNewLine(p => ({ ...p, description: e.target.value }))}
                placeholder="Optional" />
            </div>
            <button className="btn btn-primary" onClick={addLine}
              disabled={!newLine.name.trim()}>
              + Add Line
            </button>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>Name</th><th>Status</th><th>Stages</th><th></th></tr>
              </thead>
              <tbody>
                {lines.map(l => (
                  <tr key={l.id}
                    style={{ cursor: 'pointer',
                             background: selectedLine?.id === l.id ? '#eff6ff' : '' }}
                    onClick={() => setSelected(l)}>
                    <td><strong>{l.name}</strong></td>
                    <td><span className={`badge badge-${l.status === 'Active' ? 'LOW' : 'MEDIUM'}`}>{l.status}</span></td>
                    <td>{l.stageCount}</td>
                    <td>
                      <button className="btn btn-danger"
                        style={{ padding: '3px 10px', fontSize: 12 }}
                        onClick={e => { e.stopPropagation(); removeLine(l.id) }}>✕</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* ── Right: stages for selected line ── */}
        <div className="card">
          {selectedLine ? (
            <>
              <h2>Stages: {selectedLine.name}</h2>

              {/* Add stage form */}
              <div style={{ marginBottom: 16 }}>
                <div className="form-row">
                  <div className="form-group">
                    <label>Stage Name</label>
                    <input value={newStage.name}
                      onChange={e => setNewStage(p=>({...p, name: e.target.value}))}
                      placeholder="e.g. Assembly" style={{ width: 150 }} />
                  </div>
                  <div className="form-group">
                    <label>Order</label>
                    <input type="number" min="1" value={newStage.stageOrder}
                      onChange={e => setNewStage(p=>({...p, stageOrder: e.target.value}))}
                      style={{ width: 60 }} />
                  </div>
                  <div className="form-group">
                    <label>Capacity/hr</label>
                    <input type="number" min="1" value={newStage.capacityPerHour}
                      onChange={e => setNewStage(p=>({...p, capacityPerHour: e.target.value}))}
                      style={{ width: 80 }} />
                  </div>
                </div>
                <div className="form-row">
                  <div className="form-group">
                    <label>Std. Process Time (min)</label>
                    <input type="number" min="0.1" step="0.5" value={newStage.standardProcessingTimeMinutes}
                      onChange={e => setNewStage(p=>({...p, standardProcessingTimeMinutes: e.target.value}))}
                      style={{ width: 100 }} />
                  </div>
                  <div className="form-group">
                    <label>Machine (optional)</label>
                    <select value={newStage.machineId}
                      onChange={e => setNewStage(p=>({...p, machineId: e.target.value}))}
                      style={{ width: 180 }}>
                      <option value="">– none –</option>
                      {machines.map(m => <option key={m.id} value={m.id}>{m.name}</option>)}
                    </select>
                  </div>
                  <div className="form-group" style={{ justifyContent: 'flex-end' }}>
                    <button className="btn btn-primary"
                      onClick={addStage} disabled={!newStage.name.trim()}>
                      + Add Stage
                    </button>
                  </div>
                </div>
              </div>

              {/* Stages table */}
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr><th>#</th><th>Name</th><th>Capacity/hr</th><th>Std. Time</th><th>Machine</th><th></th></tr>
                  </thead>
                  <tbody>
                    {stages.sort((a,b) => a.stageOrder - b.stageOrder).map(s => (
                      <tr key={s.id}>
                        <td>{s.stageOrder}</td>
                        <td>{s.name}</td>
                        <td>{s.capacityPerHour}</td>
                        <td>{s.standardProcessingTimeMinutes} min</td>
                        <td>{s.machineName || '–'}</td>
                        <td>
                          <button className="btn btn-danger"
                            style={{ padding: '3px 10px', fontSize: 12 }}
                            onClick={() => removeStage(s.id)}>✕</button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          ) : (
            <div style={{ color: '#57606a', textAlign: 'center', marginTop: 40 }}>
              ← Select a production line to view its stages.
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
