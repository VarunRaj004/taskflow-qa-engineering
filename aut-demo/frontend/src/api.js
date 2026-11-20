const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// Thrown when the server responds with a non-2xx status. Callers can read
// `status` and `message` (the API's own `error` text, when present).
export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

// Set by AuthProvider so this module can clear the session on a 401
// without importing the context (which would create a cycle).
let onUnauthorized = () => {}
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler
}

function getToken() {
  return localStorage.getItem('token')
}

async function request(path, { method = 'GET', body, auth = false } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (auth) {
    const token = getToken()
    if (token) headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (response.status === 401 && auth) {
    onUnauthorized()
  }

  const text = await response.text()
  const data = text ? JSON.parse(text) : null

  if (!response.ok) {
    throw new ApiError(response.status, data?.error || 'Request failed')
  }

  return data
}

export const api = {
  register: (name, email, password) =>
    request('/api/auth/register', { method: 'POST', body: { name, email, password } }),

  login: (email, password) =>
    request('/api/auth/login', { method: 'POST', body: { email, password } }),

  me: () => request('/api/users/me', { auth: true }),

  dashboard: () => request('/api/dashboard', { auth: true }),

  listTasks: (params = {}) => {
    const query = new URLSearchParams()
    Object.entries(params).forEach(([key, value]) => {
      if (value) query.set(key, value)
    })
    const qs = query.toString()
    return request(`/api/tasks${qs ? `?${qs}` : ''}`, { auth: true })
  },

  createTask: (task) => request('/api/tasks', { method: 'POST', body: task, auth: true }),

  updateTask: (id, task) =>
    request(`/api/tasks/${id}`, { method: 'PUT', body: task, auth: true }),

  deleteTask: (id) => request(`/api/tasks/${id}`, { method: 'DELETE', auth: true }),
}
