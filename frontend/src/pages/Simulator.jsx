import React, { useState, useEffect } from 'react'
import { getProductionLines, runFactorySimulator } from '../api'

export default function Simulator() {
  const [lines, setLines]     = useState([])
  const [lineId, setLineId]   = useState(null)
  const [periods, setPeriods] = useState(24)
  const [result, setResult]   = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState(null)

  useEffect(() => {
    getProductionLines().then(ls => {
      setLines(ls)
      if (ls.length > 0) setLineId(ls[0].id)
    })
  }, [])

  function run() {
    if (!lineId) return
    setLoading(true); setError(null); setResult(null)
    runFactorySimulator(lineId, periods)
      .then(r => { setResult(r); setLoading(false) })
      .catch(e => { setError(e.message); setLoading(false) })
  }

  return (
    <div>
      <div className="page-header">
        <h1>🏭 Factory Event Simulator</h1>
        <p>
          Generates synthetic production records with realistic events (downtime, queue buildup,
          defects) to demonstrate how the bottleneck engine reacts.
        </p>
      </div>

      <div className="alert alert-info">
        The simulator <strong>injects one problem stage</strong> per run with elevated downtime,
        slow processing, and high wait times so you can observe the bottleneck detection in action.
        Run the simulator, then visit the <strong>Bottleneck Analysis</strong> page.
      </div>

      <div className="card">
        <div className="form-row">
          <div className="form-group">
            <label>Production Line</label>
            <select value={lineId || ''} onChange={e => setLineId(Number(e.target.value))}>
              {lines.map(l => <option key={l.id} value={l.id}>{l.name}</option>)}
            </select>
          </div>
          <div className="form-group">
            <label>Periods to Simulate (hours)</label>
            <select value={periods} onChange={e => setPeriods(Number(e.target.value))}>
              <option value={12}>12 hours</option>
              <option value={24}>24 hours</option>
              <option value={48}>48 hours</option>
              <option value={72}>72 hours</option>
            </select>
          </div>
          <div className="form-group" style={{ justifyContent: 'flex-end' }}>
            <button className="btn btn-primary" onClick={run} disabled={loading || !lineId}>
              {loading ? '⏳ Simulating…' : '▶ Run Simulation'}
            </button>
          </div>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {result && (
        <div className="card">
          <div className="alert alert-success" style={{ marginBottom: 16 }}>
            {result.message}
          </div>
          <div className="stats-row">
            <div className="stat-tile">
              <span className="label">Records Generated</span>
              <span className="value">{result.recordsGenerated}</span>
            </div>
            <div className="stat-tile">
              <span className="label">Downtime Events</span>
              <span className="value">{result.downtimeEventsGenerated}</span>
            </div>
          </div>

          <h3>Event Log</h3>
          <div style={{ background: '#1e2433', borderRadius: 8, padding: 16,
                        fontFamily: 'monospace', fontSize: 12.5,
                        color: '#a3c9f7', maxHeight: 320, overflowY: 'auto' }}>
            {result.eventLog.map((line, i) => (
              <div key={i} style={{ marginBottom: 2 }}>
                {line.startsWith('[Period') ? (
                  <span style={{ color: '#f59e0b' }}>{line}</span>
                ) : line.includes('complete') || line.includes('started') ? (
                  <span style={{ color: '#4ade80' }}>{line}</span>
                ) : (
                  <span>{line}</span>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
