// Every call to the Spring Boot API goes through this file.
// Components never call fetch() directly, which keeps them easy to test.

export class ApiError extends Error {
  constructor(message, fields = {}, status = 0, code = null) {
    super(message)
    this.fields = fields
    this.status = status
    this.code = code // e.g. 'EMAIL_NOT_VERIFIED'
  }
}

// Locally this is '' and Vite forwards /api to the backend.
// When deployed, set VITE_API_URL to your backend's address, e.g. https://second-stitch-api.onrender.com
const BASE = import.meta.env.VITE_API_URL ?? ''

export const apiUrl = (path) => (path ? BASE + path : path)

// --- login token ---
// After you sign up or log in, the server sends a token. It's kept in this browser
// and sent with every request as "Authorization: Bearer <token>".
const TOKEN_KEY = 'second-stitch-token'

function readSaved() {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null // storage can be blocked (private windows); you'll just log in again next time
  }
}

let token = readSaved()

export const hasToken = () => !!token

function saveToken(value) {
  token = value
  try {
    localStorage.setItem(TOKEN_KEY, value)
  } catch {
    // kept for this visit only
  }
}

export function logOut() {
  token = null
  try {
    localStorage.removeItem(TOKEN_KEY)
  } catch {
    // nothing saved
  }
}

async function request(path, options = {}) {
  const headers = { ...options.headers }
  if (token) headers.Authorization = `Bearer ${token}`

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
    // The token expired or stopped working: forget it, and tell the app to show the log-in page
    if (response.status === 401 && token) {
      logOut()
      window.dispatchEvent(new Event('session-expired'))
    }
    throw new ApiError(
      body?.message ?? `Request failed (${response.status})`,
      body?.fields ?? {},
      response.status,
      body?.code ?? null,
    )
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

// --- accounts ---
/** Creates an account and emails a confirmation link. Resolves to { message }. Doesn't log in. */
export const signUp = ({ email, password, name }) =>
  request('/api/auth/signup', json('POST', { email, password, name }))

/** Uses the code from a "Confirm your email" link. Logs in; resolves to the user. */
export async function verifyEmail(code) {
  const result = await request('/api/auth/verify', json('POST', { token: code }))
  saveToken(result.token)
  return result.user
}

/** Sends the confirmation link again. Resolves to { message }. */
export const resendVerification = (email) => request('/api/auth/resend-verification', json('POST', { email }))

/** Logs in. Resolves to the user ({ id, email, name }). */
export async function logIn({ email, password }) {
  const result = await request('/api/auth/login', json('POST', { email, password }))
  saveToken(result.token)
  return result.user
}

/** Emails a "Reset your password" link. Resolves to { message }. */
export const forgotPassword = (email) => request('/api/auth/forgot-password', json('POST', { email }))

/** Uses the code from a reset link to set a new password. Logs in; resolves to the user. */
export async function resetPassword(code, password) {
  const result = await request('/api/auth/reset-password', json('POST', { token: code, password }))
  saveToken(result.token)
  return result.user
}

/** Who is logged in (checks the saved token is still good). */
export const getMe = () => request('/api/auth/me')
