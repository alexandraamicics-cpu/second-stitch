import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createItem, forgotPassword, hasToken, listItems, logIn, logOut, resetPassword, signUp, verifyEmail } from './api.js'

const ok = (body, status = 200) => Promise.resolve({ ok: true, status, json: () => Promise.resolve(body) })
const fail = (status, message) =>
  Promise.resolve({ ok: false, status, json: () => Promise.resolve({ message, fields: {} }) })
const sentHeaders = () => fetch.mock.calls.at(-1)[1].headers
const user = { id: 1, email: 'ana@example.com', name: 'Ana' }

describe('login token in api.js', () => {
  beforeEach(() => {
    globalThis.fetch = vi.fn(() => ok([]))
    logOut()
  })
  afterEach(() => vi.restoreAllMocks())

  it('keeps the token after logging in and sends it with every request', async () => {
    fetch.mockImplementationOnce(() => ok({ token: 'abc.def.ghi', user }))
    expect(await logIn({ email: 'ana@example.com', password: 'thrift-forever' })).toEqual(user)
    expect(hasToken()).toBe(true)
    expect(localStorage.getItem('second-stitch-token')).toBe('abc.def.ghi')

    await listItems()
    expect(sentHeaders().Authorization).toBe('Bearer abc.def.ghi')
    await createItem({ name: 'Scarf' })
    expect(sentHeaders().Authorization).toBe('Bearer abc.def.ghi')
    expect(sentHeaders()['Content-Type']).toBe('application/json')
  })

  it('signing up does not log you in (you confirm your email first)', async () => {
    fetch.mockImplementationOnce(() => ok({ message: "We've sent a link to ana@example.com." }, 201))
    const result = await signUp({ email: 'ana@example.com', password: 'thrift-forever', name: 'Ana' })
    expect(result.message).toContain('sent a link')
    expect(hasToken()).toBe(false)
  })

  it('confirming your email logs you in', async () => {
    fetch.mockImplementationOnce(() => ok({ token: 'from.verify', user }))
    expect(await verifyEmail('code-from-email')).toEqual(user)
    expect(JSON.parse(fetch.mock.calls.at(-1)[1].body)).toEqual({ token: 'code-from-email' })
    expect(hasToken()).toBe(true)
  })

  it('resetting your password logs you in', async () => {
    fetch.mockImplementationOnce(() => ok({ token: 'from.reset', user }))
    await resetPassword('reset-code', 'brand-new-pass')
    expect(JSON.parse(fetch.mock.calls.at(-1)[1].body)).toEqual({ token: 'reset-code', password: 'brand-new-pass' })
    expect(hasToken()).toBe(true)
  })

  it('forgot password just sends the email', async () => {
    fetch.mockImplementationOnce(() => ok({ message: "If there's an account with that email, we've sent a link." }))
    await forgotPassword('ana@example.com')
    expect(hasToken()).toBe(false)
  })

  it('passes along error codes like EMAIL_NOT_VERIFIED', async () => {
    fetch.mockImplementation(() =>
      Promise.resolve({
        ok: false,
        status: 403,
        json: () => Promise.resolve({ message: 'Confirm your email first.', fields: {}, code: 'EMAIL_NOT_VERIFIED' }),
      }),
    )
    await expect(logIn({ email: 'ana@example.com', password: 'thrift-forever' })).rejects.toMatchObject({
      status: 403,
      code: 'EMAIL_NOT_VERIFIED',
    })
  })

  it('sends no Authorization header when logged out', async () => {
    await listItems()
    expect(sentHeaders().Authorization).toBeUndefined()
  })

  it('logging out forgets the token', async () => {
    fetch.mockImplementationOnce(() => ok({ token: 'abc.def.ghi', user }))
    await logIn({ email: 'ana@example.com', password: 'thrift-forever' })
    logOut()
    expect(hasToken()).toBe(false)
    expect(localStorage.getItem('second-stitch-token')).toBeNull()
  })

  it('forgets an expired token and tells the app', async () => {
    fetch.mockImplementationOnce(() => ok({ token: 'old.token', user }))
    await logIn({ email: 'ana@example.com', password: 'thrift-forever' })
    fetch.mockImplementation(() => fail(401, 'Log in to continue'))
    const onExpired = vi.fn()
    window.addEventListener('session-expired', onExpired)

    await expect(listItems()).rejects.toThrow('Log in to continue')
    expect(hasToken()).toBe(false)
    expect(onExpired).toHaveBeenCalled()
    window.removeEventListener('session-expired', onExpired)
  })

  it('a wrong password shows the message and saves nothing', async () => {
    fetch.mockImplementation(() => fail(401, "That email and password don't match"))
    await expect(logIn({ email: 'ana@example.com', password: 'nope' })).rejects.toThrow(
      "That email and password don't match",
    )
    expect(hasToken()).toBe(false)
  })
})
