// Every call to the Spring Boot API goes through this file.
// Components never call fetch() directly, which keeps them easy to test.

export class ApiError extends Error {
  constructor(message, fields = {}, status = 0) {
    super(message)
    this.fields = fields
    this.status = status
  }
}

// Locally this is '' and Vite forwards /api to the backend.
// When deployed, set VITE_API_URL to your backend's address, e.g. https://second-stitch-api.onrender.com
const BASE = import.meta.env.VITE_API_URL ?? ''

export const apiUrl = (path) => (path ? BASE + path : path)

// --- owner password ---
// Saved in this browser after you unlock editing, and sent with every change.
// The server checks it; looking around never needs it.
const PASSWORD_KEY = 'second-stitch-owner-password'
const OWNER_HEADER = 'X-Owner-Password'

function readSaved() {
  try {
    return localStorage.getItem(PASSWORD_KEY)
  } catch {
    return null // storage can be blocked (private windows); just ask again next time
  }
}

let ownerPassword = readSaved()

export const hasOwnerPassword = () => !!ownerPassword

export function setOwnerPassword(password) {
  ownerPassword = password
  try {
    localStorage.setItem(PASSWORD_KEY, password)
  } catch {
    // remembered for this visit only
  }
}

export function clearOwnerPassword() {
  ownerPassword = null
  try {
    localStorage.removeItem(PASSWORD_KEY)
  } catch {
    // nothing saved
  }
}

const isChange = (method = 'GET') => method !== 'GET'

async function request(path, options = {}) {
  const headers = { ...options.headers }
  if (ownerPassword && isChange(options.method)) headers[OWNER_HEADER] = ownerPassword

  let response
  try {
    response = await fetch(BASE + path, { ...options, headers })
  } catch {
    throw new ApiError(
      BASE
        ? "Can't reach the server. It may be waking up, which takes about a minute. Try again shortly."
        : "Can't reach the server. Is the backend running on port 8080?",
    )
  }

  if (response.status === 204) return null

  const body = await response.json().catch(() => null)
  if (!response.ok) {
    // The saved password stopped working (e.g. it was changed on the server): forget it
    if (response.status === 401 && ownerPassword) {
      clearOwnerPassword()
      window.dispatchEvent(new Event('owner-locked'))
    }
    throw new ApiError(body?.message ?? `Request failed (${response.status})`, body?.fields ?? {}, response.status)
  }
  return body
}

const json = (method, data) => ({
  method,
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(data),
})

// --- items ---
export const listItems = (category) =>
  request(category ? `/api/items?category=${encodeURIComponent(category)}` : '/api/items')
export const createItem = (data) => request('/api/items', json('POST', data))
export const updateItem = (id, data) => request(`/api/items/${id}`, json('PUT', data))
export const deleteItem = (id) => request(`/api/items/${id}`, { method: 'DELETE' })
export const wearItem = (id) => request(`/api/items/${id}/wear`, { method: 'POST' })
export const unwearItem = (id) => request(`/api/items/${id}/wear`, { method: 'DELETE' })

export function uploadPhoto(id, file) {
  const form = new FormData()
  form.append('file', file)
  // No Content-Type header: the browser adds the multipart boundary itself.
  return request(`/api/items/${id}/photo`, { method: 'POST', body: form })
}

// --- outfits ---
export const listOutfits = () => request('/api/outfits')
export const createOutfit = (data) => request('/api/outfits', json('POST', data))
export const wearOutfit = (id) => request(`/api/outfits/${id}/wear`, { method: 'POST' })
export const unwearOutfit = (id) => request(`/api/outfits/${id}/wear`, { method: 'DELETE' })
export const deleteOutfit = (id) => request(`/api/outfits/${id}`, { method: 'DELETE' })

// --- stats ---
export const getStats = () => request('/api/stats')

// --- owner password ---
/** { locked: true } when the server needs a password for changes. */
export const getAuthStatus = () => request('/api/auth')

/** Resolves if the password is right, throws an ApiError (401) if not. Doesn't save it. */
export function checkOwnerPassword(password) {
  return request('/api/auth/check', { method: 'POST', headers: { [OWNER_HEADER]: password } })
}
