import React, { useState, useEffect } from 'react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
         ReferenceLine, CartesianGrid } from 'recharts'
import { getProductionLines, runWhatIf } from '../api'

const DEFAULT = {
  downtimeReductionPercent: 0,
  processingTimeReductionPercent: 0,
  additionalCapacityUnitsPerHour: 0,
  defectRateReductionPercent: 0,
}

export default function WhatIf() {
  const [lines, setLines]   = useState([])
  const [lineId, setLineId] = useState(null)
  const [params, setParams] = useState({ ...DEFAULT })
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError]   = useState(null)

  useEffect(() => {
    getProductionLines().then(ls => {
      setLines(ls)
      if (ls.length > 0) setLineId(ls[0].id)
    })
  }, [])

  function set(key, val) { setParams(p => ({ ...p, [key]: Number(val) })) }

  function simulate() {
    if (!lineId) return
    setLoading(true); setError(null)
    runWhatIf(lineId, params)
      .then(r => { setResult(r); setLoading(false) })
      .catch(e => { setError(e.message); setLoading(false) })
  }

  const compareData = result ? [
    { name: 'Throughput (u/hr)', baseline: result.baselineThroughput,  estimated: result.estimatedThroughput },
    { name: 'Downtime %',        baseline: result.baselineDowntimePercent, estimated: result.estimatedDowntimePercent },
    { name: 'Processing (min)',  baseline: result.baselineProcessingTime, estimated: result.estimatedProcessingTime },
    { name: 'Defect Rate %',     baseline: result.baselineDefectRate,   estimated: result.estimatedDefectRate },
  ] : []

  return (
    <div>
      <div className="page-header">
        <h1>💡 What-If Simulator</h1>
        <p>Estimate the impact of improvements to the current bottleneck stage.</p>
      </div>

      <div className="alert alert-info">
        <strong>Note:</strong> All results are <strong>ESTIMATES</strong> based on simplified linear
        projections. Real outcomes will vary.
      </div>

      <div className="card">
        <div className="form-row">
          <div className="form-group">
            <label>Production Line</label>
            <select value={lineId || ''} onChange={e => setLineId(Number(e.target.value))}>
              {lines.map(l => <option key={l.id} value={l.id}>{l.name}</option>)}
            </select>
          </div>
        </div>

        <h3>Adjustment Parameters</h3>

        <Slider label="Downtime Reduction (%)"
          value={params.downtimeReductionPercent}
          onChange={v => set('downtimeReductionPercent', v)} />

        <Slider label="Processing Time Reduction (%)"
          value={params.processingTimeReductionPercent}
          onChange={v => set('processingTimeReductionPercent', v)} />

        <Slider label="Defect Rate Reduction (%)"
          value={params.defectRateReductionPercent}
          onChange={v => set('defectRateReductionPercent', v)} />

        <div className="form-row" style={{ alignItems: 'center', marginTop: 8 }}>
          <div className="form-group">
            <label>Additional Capacity (units/hr)</label>
            <input type="number" min="0" max="100" step="1"
              value={params.additionalCapacityUnitsPerHour}
              onChange={e => set('additionalCapacityUnitsPerHour', e.target.value)}
              style={{ width: 100 }}
            />
          </div>
          <button className="btn btn-primary" onClick={simulate} disabled={loading || !lineId}
            style={{ marginTop: 18 }}>
            {loading ? 'Simulating…' : '▶ Run Simulation'}
          </button>
          <button className="btn btn-secondary" onClick={() => { setParams({...DEFAULT}); setResult(null) }}
            style={{ marginTop: 18 }}>
            Reset
          </button>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {result && (
        <>
          {/* ── Key result ── */}
          <div className="card" style={{ borderLeft: '4px solid #3b82d4' }}>
            <h2>Estimated Improvement for: {result.targetStageName}</h2>
            <div className="stats-row" style={{ marginBottom: 0 }}>
              <div className="stat-tile">
                <span className="label">Baseline Throughput</span>
                <span className="value">{result.baselineThroughput}</span>
                <span className="unit">units/hr</span>
              </div>
              <div className="stat-tile">
                <span className="label">Estimated Throughput</span>
                <span className="value" style={{ color: '#22c55e' }}>{result.estimatedThroughput}</span>
                <span className="unit">units/hr</span>
              </div>
              <div className="stat-tile">
                <span className="label">Improvement</span>
                <span className="value" style={{ color: '#22c55e' }}>
                  +{result.throughputImprovementUnitsPerHour}
                </span>
                <span className="unit">{result.throughputImprovementPercent}% gain (ESTIMATE)</span>
              </div>
            </div>
          </div>

          {/* ── Comparison chart ── */}
          <div className="card">
            <h2>Before vs. After Comparison (ESTIMATE)</h2>
            <ResponsiveContainer width="100%" height={240}>
              <BarChart data={compareData} margin={{ top: 4, right: 16, left: 0, bottom: 4 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f2f5" />
                <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="baseline"  name="Baseline"  fill="#9ca3af" radius={[4,4,0,0]} />
                <Bar dataKey="estimated" name="Estimated" fill="#3b82d4" radius={[4,4,0,0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>

          {/* ── Disclaimer ── */}
          <div className="alert alert-warning">{result.disclaimer}</div>
        </>
      )}
    </div>
  )
}

function Slider({ label, value, onChange }) {
  return (
    <div className="slider-row">
      <label>{label}</label>
      <input type="range" min="0" max="100" step="5"
        value={value} onChange={e => onChange(e.target.value)} />
      <span className="val">{value}%</span>
    </div>
  )
}
