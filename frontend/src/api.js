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

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(BASE + path, options)
  } catch {
    throw new ApiError("Can't reach the server. Is the backend running on port 8080?")
  }

  if (response.status === 204) return null

  const body = await response.json().catch(() => null)
  if (!response.ok) {
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
