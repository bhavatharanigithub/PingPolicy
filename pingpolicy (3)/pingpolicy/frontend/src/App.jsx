import React from 'react';
import { Routes, Route, NavLink } from 'react-router-dom';
import Dashboard from './pages/Dashboard.jsx';
import Contracts from './pages/Contracts.jsx';
import DriftEvents from './pages/DriftEvents.jsx';

export default function App() {
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">PingPolicy</div>
        <nav>
          <NavLink to="/" end className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
            Dashboard
          </NavLink>
          <NavLink to="/contracts" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
            Contracts
          </NavLink>
          <NavLink to="/drift-events" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
            Drift Events
          </NavLink>
        </nav>
      </aside>
      <main className="main">
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/contracts" element={<Contracts />} />
          <Route path="/drift-events" element={<DriftEvents />} />
        </Routes>
      </main>
    </div>
  );
}
