const REGISTRY_BASE_URL = import.meta.env.VITE_REGISTRY_URL || 'http://localhost:8081';
const MONITOR_BASE_URL = import.meta.env.VITE_MONITOR_URL || 'http://localhost:8082';

async function handleResponse(res) {
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`Request failed (${res.status}): ${text || res.statusText}`);
  }
  if (res.status === 204) return null;
  return res.json();
}

export const contractsApi = {
  list: () => fetch(`${REGISTRY_BASE_URL}/api/contracts`).then(handleResponse),
  get: (id) => fetch(`${REGISTRY_BASE_URL}/api/contracts/${id}`).then(handleResponse),
  create: (payload) =>
    fetch(`${REGISTRY_BASE_URL}/api/contracts`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }).then(handleResponse),
  update: (id, payload) =>
    fetch(`${REGISTRY_BASE_URL}/api/contracts/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }).then(handleResponse),
  remove: (id) =>
    fetch(`${REGISTRY_BASE_URL}/api/contracts/${id}`, { method: 'DELETE' }).then(handleResponse),
};

export const monitorApi = {
  driftEvents: () => fetch(`${MONITOR_BASE_URL}/api/drift-events`).then(handleResponse),
  driftEventsForContract: (contractId) =>
    fetch(`${MONITOR_BASE_URL}/api/drift-events/contract/${contractId}`).then(handleResponse),
  stats: () => fetch(`${MONITOR_BASE_URL}/api/stats`).then(handleResponse),
};
