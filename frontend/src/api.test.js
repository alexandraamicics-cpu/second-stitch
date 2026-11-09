import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { checkOwnerPassword, clearOwnerPassword, createItem, hasOwnerPassword, listItems, setOwnerPassword } from './api.js'

const ok = (body, status = 200) => Promise.resolve({ ok: true, status, json: () => Promise.resolve(body) })
const fail = (status, message) =>
  Promise.resolve({ ok: false, status, json: () => Promise.resolve({ message, fields: {} }) })
const sentHeaders = () => fetch.mock.calls.at(-1)[1].headers

describe('owner password in api.js', () => {
  beforeEach(() => {
    globalThis.fetch = vi.fn(() => ok([]))
    clearOwnerPassword()
  })
  afterEach(() => vi.restoreAllMocks())

  it('sends the saved password with changes', async () => {
    setOwnerPassword('secret')
    await createItem({ name: 'Scarf' })
    expect(sentHeaders()['X-Owner-Password']).toBe('secret')
    expect(sentHeaders()['Content-Type']).toBe('application/json')
  })

  it('never sends the password when just looking', async () => {
    setOwnerPassword('secret')
    await listItems()
    expect(sentHeaders()['X-Owner-Password']).toBeUndefined()
  })

  it('remembers the password in this browser', () => {
    setOwnerPassword('secret')
    expect(localStorage.getItem('second-stitch-owner-password')).toBe('secret')
    expect(hasOwnerPassword()).toBe(true)
  })

  it('forgets a password the server rejects, and tells the app', async () => {
    setOwnerPassword('old-password')
    fetch.mockImplementation(() => fail(401, "That password isn't right"))
    const onLocked = vi.fn()
    window.addEventListener('owner-locked', onLocked)

    await expect(createItem({ name: 'Scarf' })).rejects.toThrow("That password isn't right")
    expect(hasOwnerPassword()).toBe(false)
    expect(onLocked).toHaveBeenCalled()
    window.removeEventListener('owner-locked', onLocked)
  })

  it('checks a password without saving it', async () => {
    fetch.mockImplementation(() => ok({ ok: true }))
    await checkOwnerPassword('try-this')
    expect(sentHeaders()['X-Owner-Password']).toBe('try-this')
    expect(hasOwnerPassword()).toBe(false)
  })
})
