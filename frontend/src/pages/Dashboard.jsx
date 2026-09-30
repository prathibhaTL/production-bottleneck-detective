import React, { useEffect, useState } from 'react'
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
  LineChart, Line, CartesianGrid, Legend
} from 'recharts'
import { getDashboard, getProductionLines } from '../api'

export default function Dashboard() {
  const [lines, setLines]     = useState([])
  const [lineId, setLineId]   = useState(null)
  const [data, setData]       = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState(null)

  // Load production lines for the selector
  useEffect(() => {
    getProductionLines()
      .then(ls => {
        setLines(ls)
        if (ls.length > 0) setLineId(ls[0].id)
      })
      .catch(e => setError(e.message))
  }, [])

  // Load dashboard whenever the selected line changes
  useEffect(() => {
    if (!lineId) return
    setLoading(true)
    setError(null)
    getDashboard(lineId)
      .then(d => { setData(d); setLoading(false) })
      .catch(e => { setError(e.message); setLoading(false) })
  }, [lineId])

  // Build chart data from stageUtilization map
  const utilChartData = data
    ? Object.entries(data.stageUtilization).map(([name, util]) => ({
        name: name.length > 14 ? name.slice(0, 13) + '…' : name,
        utilization: util,
        queue: data.stageQueueTime[name] ?? 0,
      }))
    : []

  const severityBadgeClass = (sev) => `badge badge-${sev || 'LOW'}`

  return (
    <div>
      <div className="page-header">
        <h1>📊 Production Dashboard</h1>
        <p>Live overview of production metrics, efficiency, and bottleneck status.</p>
      </div>

      {/* Line selector */}
      <div className="card" style={{ paddingBottom: 14 }}>
        <div className="form-row">
          <div className="form-group">
            <label>Production Line</label>
            <select value={lineId || ''} onChange={e => setLineId(Number(e.target.value))}>
              {lines.map(l => <option key={l.id} value={l.id}>{l.name}</option>)}
            </select>
          </div>
        </div>
      </div>

      {loading && <div className="loader">Loading dashboard…</div>}
      {error   && <div className="alert alert-error">{error}</div>}

      {data && (
        <>
          {/* ── KPI tiles ── */}
          <div className="stats-row">
            <Tile label="Total Units Produced" value={data.totalUnitsProduced.toLocaleString()} />
            <Tile label="Defective Units"      value={data.totalDefectiveUnits.toLocaleString()} unit={`${data.overallDefectRate}%`} />
            <Tile label="Production Efficiency" value={`${data.productionEfficiency}%`} />
            <Tile label="Overall Downtime"     value={`${data.overallDowntimePercent}%`} />
            <div className="stat-tile">
              <span className="label">Current Bottleneck</span>
              <span className="value" style={{ fontSize: 16 }}>{data.currentBottleneckStage}</span>
              <span className={severityBadgeClass(data.bottleneckSeverity)}>{data.bottleneckSeverity}</span>
            </div>
          </div>

          <div className="two-col">
            {/* ── Stage utilization bar chart ── */}
            <div className="card">
              <h2>Stage Capacity Utilization (%)</h2>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={utilChartData} margin={{ top: 4, right: 16, left: 0, bottom: 4 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f2f5" />
                  <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                  <YAxis domain={[0, 100]} tick={{ fontSize: 11 }} unit="%" />
                  <Tooltip formatter={v => `${v}%`} />
                  <Bar dataKey="utilization" fill="#3b82d4" radius={[4, 4, 0, 0]}
                       label={{ position: 'top', fontSize: 11, formatter: v => `${v}%` }} />
                </BarChart>
              </ResponsiveContainer>
            </div>

            {/* ── Average queue (wait) time ── */}
            <div className="card">
              <h2>Average Wait Time per Stage (min)</h2>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart data={utilChartData} margin={{ top: 4, right: 16, left: 0, bottom: 4 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f2f5" />
                  <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                  <YAxis tick={{ fontSize: 11 }} unit=" min" />
                  <Tooltip formatter={v => `${v} min`} />
                  <Bar dataKey="queue" fill="#f59e0b" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* ── Production trend ── */}
          <div className="card">
            <h2>Production Trend (recent periods)</h2>
            <ResponsiveContainer width="100%" height={220}>
              <LineChart data={data.productionTrend}
                         margin={{ top: 4, right: 16, left: 0, bottom: 4 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f2f5" />
                <XAxis dataKey="label" tick={{ fontSize: 10 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip />
                <Legend />
                <Line type="monotone" dataKey="units" stroke="#3b82d4"
                      dot={false} strokeWidth={2} name="Units Produced" />
              </LineChart>
            </ResponsiveContainer>
          </div>

          {/* ── Pipeline visualization ── */}
          <div className="card">
            <h2>Production Line Flow</h2>
            <div className="pipeline">
              {utilChartData.map((s, i) => (
                <React.Fragment key={s.name}>
                  {i > 0 && <div className="pipeline-arrow">→</div>}
                  <div className={`pipeline-stage${s.name === data.currentBottleneckStage || data.currentBottleneckStage.startsWith(s.name.replace('…','')) ? ' bottleneck' : ''}`}>
                    <div className="stage-name">{s.name}</div>
                    <div className="stage-util">{s.utilization}%</div>
                    {(s.name === data.currentBottleneckStage || data.currentBottleneckStage.startsWith(s.name.replace('…',''))) &&
                      <div style={{ fontSize: 10, color: '#ef4444', marginTop: 4 }}>⚠ BOTTLENECK</div>}
                  </div>
                </React.Fragment>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  )
}

function Tile({ label, value, unit }) {
  return (
    <div className="stat-tile">
      <span className="label">{label}</span>
      <span className="value">{value}</span>
      {unit && <span className="unit">{unit}</span>}
    </div>
  )
}
