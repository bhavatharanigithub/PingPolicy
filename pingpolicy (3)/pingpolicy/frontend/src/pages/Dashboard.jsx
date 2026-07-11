import React, { useEffect, useState } from 'react';
import { monitorApi, contractsApi } from '../api/client.js';
import SeverityBadge from '../components/SeverityBadge.jsx';

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [events, setEvents] = useState([]);
  const [contractCount, setContractCount] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const [statsRes, eventsRes, contractsRes] = await Promise.all([
          monitorApi.stats(),
          monitorApi.driftEvents(),
          contractsApi.list(),
        ]);
        if (cancelled) return;
        setStats(statsRes);
        setEvents(eventsRes.slice(0, 8));
        setContractCount(contractsRes.length);
      } catch (e) {
        if (!cancelled) setError(e.message);
      }
    }

    load();
    const interval = setInterval(load, 15000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, []);

  return (
    <div>
      <h1>Dashboard</h1>
      <p className="subtitle">Live view of polling activity and recent contract drift.</p>

      {error && (
        <div className="error-banner">
          Couldn't reach the backend services ({error}). Make sure both the registry and
          polling/alerting services are running.
        </div>
      )}

      <div className="stats-row">
        <div className="stat-card">
          <div className="stat-value">{contractCount ?? '—'}</div>
          <div className="stat-label">Registered contracts</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats ? stats.totalPolls : '—'}</div>
          <div className="stat-label">Total polls executed</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">
            {stats ? `${Math.round(stats.cacheHitRatio * 100)}%` : '—'}
          </div>
          <div className="stat-label">Redis cache hit ratio (skipped diffs)</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats ? stats.driftsDetected : '—'}</div>
          <div className="stat-label">Drift events detected</div>
        </div>
      </div>

      <h2 style={{ fontSize: '1.05rem', marginBottom: 12 }}>Recent drift events</h2>
      {events.length === 0 ? (
        <div className="card empty-state">No drift detected yet — all monitored contracts are stable.</div>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Service</th>
              <th>Endpoint</th>
              <th>Changes</th>
              <th>Severity</th>
              <th>Detected</th>
            </tr>
          </thead>
          <tbody>
            {events.map((e) => (
              <tr key={e.id}>
                <td>{e.serviceName}</td>
                <td><code className="path">{e.endpointUrl}</code></td>
                <td>{e.changeCount}</td>
                <td><SeverityBadge severity={e.severity} /></td>
                <td>{new Date(e.detectedAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
