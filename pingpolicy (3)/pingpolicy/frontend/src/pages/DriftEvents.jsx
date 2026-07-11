import React, { useEffect, useState } from 'react';
import { monitorApi } from '../api/client.js';
import SeverityBadge from '../components/SeverityBadge.jsx';

export default function DriftEvents() {
  const [events, setEvents] = useState([]);
  const [expandedId, setExpandedId] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    monitorApi
      .driftEvents()
      .then(setEvents)
      .catch((e) => setError(e.message));
  }, []);

  return (
    <div>
      <h1>Drift Events</h1>
      <p className="subtitle">Full history of detected schema drift across monitored services.</p>

      {error && <div className="error-banner">{error}</div>}

      {events.length === 0 ? (
        <div className="card empty-state">No drift events recorded.</div>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Service</th>
              <th>Changes</th>
              <th>Severity</th>
              <th>Detected</th>
              <th>Alert sent</th>
            </tr>
          </thead>
          <tbody>
            {events.map((e) => (
              <React.Fragment key={e.id}>
                <tr
                  onClick={() => setExpandedId(expandedId === e.id ? null : e.id)}
                  style={{ cursor: 'pointer' }}
                >
                  <td>{e.serviceName}</td>
                  <td>{e.changeCount}</td>
                  <td><SeverityBadge severity={e.severity} /></td>
                  <td>{new Date(e.detectedAt).toLocaleString()}</td>
                  <td>{e.alertSent ? 'Yes' : 'No'}</td>
                </tr>
                {expandedId === e.id && (
                  <tr>
                    <td colSpan={5}>
                      <pre style={{ margin: 0, whiteSpace: 'pre-wrap', fontSize: '0.8rem', color: '#9096a8' }}>
                        {JSON.stringify(JSON.parse(e.diffSummaryJson), null, 2)}
                      </pre>
                    </td>
                  </tr>
                )}
              </React.Fragment>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
