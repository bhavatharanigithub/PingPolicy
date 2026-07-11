import React, { useEffect, useState } from 'react';
import { contractsApi } from '../api/client.js';

const emptyForm = {
  serviceName: '',
  endpointUrl: '',
  httpMethod: 'GET',
  expectedSchemaJson: '{\n  "id": 1,\n  "name": "example"\n}',
  pollIntervalSeconds: 60,
  active: true,
};

export default function Contracts() {
  const [contracts, setContracts] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  async function load() {
    try {
      setLoading(true);
      const data = await contractsApi.list();
      setContracts(data);
      setError(null);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  function updateField(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    try {
      JSON.parse(form.expectedSchemaJson); // validate before sending
      await contractsApi.create({
        ...form,
        pollIntervalSeconds: Number(form.pollIntervalSeconds),
      });
      setForm(emptyForm);
      setShowForm(false);
      load();
    } catch (err) {
      setError(err.message.includes('JSON') ? 'Expected schema must be valid JSON' : err.message);
    }
  }

  async function handleDelete(id) {
    if (!confirm('Remove this contract from monitoring?')) return;
    await contractsApi.remove(id);
    load();
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1>Contracts</h1>
          <p className="subtitle">Registered API endpoints and their expected JSON shape.</p>
        </div>
        <button onClick={() => setShowForm((s) => !s)}>
          {showForm ? 'Cancel' : '+ Register contract'}
        </button>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {showForm && (
        <form className="card" onSubmit={handleSubmit}>
          <div className="form-grid">
            <div>
              <label>Service name</label>
              <input
                required
                value={form.serviceName}
                onChange={(e) => updateField('serviceName', e.target.value)}
                placeholder="orders-service"
              />
            </div>
            <div>
              <label>HTTP method</label>
              <select value={form.httpMethod} onChange={(e) => updateField('httpMethod', e.target.value)}>
                <option>GET</option>
                <option>POST</option>
              </select>
            </div>
            <div className="full">
              <label>Endpoint URL</label>
              <input
                required
                value={form.endpointUrl}
                onChange={(e) => updateField('endpointUrl', e.target.value)}
                placeholder="https://api.example.com/orders/123"
              />
            </div>
            <div>
              <label>Poll interval (seconds)</label>
              <input
                type="number"
                min="5"
                value={form.pollIntervalSeconds}
                onChange={(e) => updateField('pollIntervalSeconds', e.target.value)}
              />
            </div>
            <div>
              <label>Active</label>
              <select
                value={form.active ? 'true' : 'false'}
                onChange={(e) => updateField('active', e.target.value === 'true')}
              >
                <option value="true">Yes</option>
                <option value="false">No</option>
              </select>
            </div>
            <div className="full">
              <label>Expected schema (JSON)</label>
              <textarea
                required
                value={form.expectedSchemaJson}
                onChange={(e) => updateField('expectedSchemaJson', e.target.value)}
              />
            </div>
          </div>
          <button type="submit">Save contract</button>
        </form>
      )}

      {loading ? (
        <div className="card empty-state">Loading…</div>
      ) : contracts.length === 0 ? (
        <div className="card empty-state">No contracts registered yet.</div>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Service</th>
              <th>Endpoint</th>
              <th>Method</th>
              <th>Interval</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {contracts.map((c) => (
              <tr key={c.id}>
                <td>{c.serviceName}</td>
                <td><code className="path">{c.endpointUrl}</code></td>
                <td>{c.httpMethod}</td>
                <td>{c.pollIntervalSeconds}s</td>
                <td>{c.active ? 'Active' : 'Paused'}</td>
                <td>
                  <button className="secondary" onClick={() => handleDelete(c.id)}>
                    Remove
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
