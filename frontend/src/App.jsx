import React from 'react'
import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom'
import Dashboard     from './pages/Dashboard'
import Analysis      from './pages/Analysis'
import WhatIf        from './pages/WhatIf'
import Simulator     from './pages/Simulator'
import Lines         from './pages/Lines'
import Machines      from './pages/Machines'
import './App.css'

export default function App() {
  return (
    <BrowserRouter>
      <div className="app-layout">
        {/* ── Sidebar navigation ── */}
        <nav className="sidebar">
          <div className="sidebar-logo">
            <span className="logo-icon">🔍</span>
            <span className="logo-text">Bottleneck<br/>Detective</span>
          </div>
          <ul className="nav-list">
            <li><NavLink to="/"            end className={navCls}>📊 Dashboard</NavLink></li>
            <li><NavLink to="/analysis"        className={navCls}>🔬 Bottleneck Analysis</NavLink></li>
            <li><NavLink to="/whatif"          className={navCls}>💡 What-If Simulator</NavLink></li>
            <li><NavLink to="/simulator"       className={navCls}>🏭 Factory Simulator</NavLink></li>
            <li><NavLink to="/lines"           className={navCls}>⚙️  Production Lines</NavLink></li>
            <li><NavLink to="/machines"        className={navCls}>🔧 Machines</NavLink></li>
          </ul>
        </nav>

        {/* ── Main content area ── */}
        <main className="main-content">
          <Routes>
            <Route path="/"          element={<Dashboard />} />
            <Route path="/analysis"  element={<Analysis />} />
            <Route path="/whatif"    element={<WhatIf />} />
            <Route path="/simulator" element={<Simulator />} />
            <Route path="/lines"     element={<Lines />} />
            <Route path="/machines"  element={<Machines />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}

// Active link class helper
const navCls = ({ isActive }) => `nav-link${isActive ? ' nav-link--active' : ''}`
