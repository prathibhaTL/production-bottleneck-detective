import React, { useState, useEffect } from 'react'
import { RadarChart, Radar, PolarGrid, PolarAngleAxis,
         ResponsiveContainer, Tooltip } from 'recharts'
import { getProductionLines, runAnalysis } from '../api'

export default function Analysis() {
  const [lines, setLines]     = useState([])
  const [lineId, setLineId]   = useState(null)
  const [hoursBack, setHours] = useState(48)
  const [result, setResult]   = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState(null)

  useEffect(() => {
    getProductionLines().then(ls => {
      setLines(ls)
      if (ls.length > 0) setLineId(ls[0].id)
    })
  }, [])

  function analyze() {
    if (!lineId) return
    setLoading(true); setError(null); setResult(null)
    runAnalysis(lineId, hoursBack)
      .then(r => { setResult(r); setLoading(false) })
      .catch(e => { setError(e.message); setLoading(false) })
  }

  // Radar chart data for the bottleneck stage
  const radarData = result?.bottleneckStage ? [
    { metric: 'Utilization',   value: result.bottleneckStage.capacityUtilization },
    { metric: 'Downtime %',    value: result.bottleneckStage.downtimePercentage },
    { metric: 'Wait Time',     value: Math.min(result.bottleneckStage.avgWaitingTime * 5, 100) },
    { metric: 'Process Time',  value: Math.min(result.bottleneckStage.avgProcessingTime * 8, 100) },
    { metric: 'Defect Rate',   value: Math.min(result.bottleneckStage.defectRate * 5, 100) },
  ] : []

  return (
    <div>
      <div className="page-header">
        <h1>🔬 Bottleneck Analysis</h1>
        <p>Analyzes production records to detect the constraining stage and its root causes.</p>
      </div>

      {/* Controls */}
      <div className="card">
        <div className="form-row">
          <div className="form-group">
            <label>Production Line</label>
            <select value={lineId || ''} onChange={e => setLineId(Number(e.target.value))}>
              {lines.map(l => <option key={l.id} value={l.id}>{l.name}</option>)}
            </select>
          </div>
          <div className="form-group">
            <label>Hours Back</label>
            <select value={hoursBack} onChange={e => setHours(Number(e.target.value))}>
              <option value={24}>Last 24 h</option>
              <option value={48}>Last 48 h</option>
              <option value={168}>Last 7 days</option>
            </select>
          </div>
          <div className="form-group" style={{ justifyContent: 'flex-end' }}>
            <button className="btn btn-primary" onClick={analyze} disabled={loading || !lineId}>
              {loading ? 'Analyzing…' : '▶ Run Analysis'}
            </button>
          </div>
        </div>
      </div>

      {error  && <div className="alert alert-error">{error}</div>}

      {result && (
        <>
          {/* ── Summary ── */}
          <div className="card" style={{ borderLeft: '4px solid #ef4444' }}>
            <h2>Analysis Result</h2>
            <p style={{ marginBottom: 12 }}>{result.summary}</p>
            <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
              <span style={{ fontWeight: 600 }}>Severity:</span>
              <span className={`badge badge-${result.severityLabel}`}>{result.severityLabel}</span>
              <span style={{ fontWeight: 600, marginLeft: 16 }}>Score:</span>
              <span>{result.severityScore} / 100</span>
            </div>
          </div>

          <div className="two-col">
            {/* ── Bottleneck stage metrics ── */}
            {result.bottleneckStage && (
              <div className="card">
                <h2>Bottleneck Stage: {result.bottleneckStage.stageName}</h2>
                <MetricRow label="Capacity Utilization" value={`${result.bottleneckStage.capacityUtilization}%`} />
                <MetricRow label="Throughput"           value={`${result.bottleneckStage.throughput} units/hr`} />
                <MetricRow label="Avg Processing Time"  value={`${result.bottleneckStage.avgProcessingTime} min`} />
                <MetricRow label="Avg Waiting Time"     value={`${result.bottleneckStage.avgWaitingTime} min`} />
                <MetricRow label="Downtime"             value={`${result.bottleneckStage.downtimePercentage}%`} />
                <MetricRow label="Defect Rate"          value={`${result.bottleneckStage.defectRate}%`} />
              </div>
            )}

            {/* ── Radar chart ── */}
            <div className="card">
              <h2>Metric Profile (normalised 0-100)</h2>
              <ResponsiveContainer width="100%" height={240}>
                <RadarChart data={radarData}>
                  <PolarGrid />
                  <PolarAngleAxis dataKey="metric" tick={{ fontSize: 11 }} />
                  <Radar dataKey="value" stroke="#ef4444" fill="#ef4444" fillOpacity={0.3} />
                  <Tooltip formatter={v => v.toFixed(1)} />
                </RadarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* ── Contributing factors ── */}
          {result.bottleneckStage?.contributingFactors?.length > 0 && (
            <div className="card">
              <h2>⚠ Possible Contributing Factors</h2>
              <div className="alert alert-warning" style={{ marginBottom: 12 }}>
                These are <strong>possible contributing factors</strong> indicated by the data.
                They do not represent confirmed root causes.
              </div>
              <ul className="factors-list">
                {result.bottleneckStage.contributingFactors.map((f, i) => (
                  <li key={i}>{f}</li>
                ))}
              </ul>
            </div>
          )}

          {/* ── All stages table ── */}
          <div className="card">
            <h2>All Stage Scores</h2>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Stage</th>
                    <th>Utilization</th>
                    <th>Throughput</th>
                    <th>Avg Wait</th>
                    <th>Downtime</th>
                    <th>Defects</th>
                    <th>Score</th>
                  </tr>
                </thead>
                <tbody>
                  {result.allStageMetrics.map(s => (
                    <tr key={s.stageId}
                        style={s.stageId === result.bottleneckStage?.stageId
                          ? { background: '#fef2f2' } : {}}>
                      <td><strong>{s.stageName}</strong>
                        {s.stageId === result.bottleneckStage?.stageId &&
                          <span style={{ marginLeft: 6, color: '#ef4444' }}>⚠</span>}
                      </td>
                      <td>{s.capacityUtilization}%</td>
                      <td>{s.throughput} u/hr</td>
                      <td>{s.avgWaitingTime} min</td>
                      <td>{s.downtimePercentage}%</td>
                      <td>{s.defectRate}%</td>
                      <td><strong>{s.bottleneckScore}</strong></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  )
}

function MetricRow({ label, value }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between',
                  padding: '6px 0', borderBottom: '1px solid #f0f2f5' }}>
      <span style={{ color: '#57606a' }}>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}
