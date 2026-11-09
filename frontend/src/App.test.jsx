import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.jsx'
import * as api from './api.js'
import { forgetEmailLink } from './utils/emailLink.js'

vi.mock('./api.js', () => ({
  apiUrl: (path) => path,
  hasToken: vi.fn(() => false),
  getMe: vi.fn(),
  logIn: vi.fn(),
  signUp: vi.fn(),
  verifyEmail: vi.fn(),
  resendVerification: vi.fn(),
  forgotPassword: vi.fn(),
  resetPassword: vi.fn(),
  logOut: vi.fn(),
  listItems: vi.fn(),
  wearItem: vi.fn(),
  unwearItem: vi.fn(),
  deleteItem: vi.fn(),
  createItem: vi.fn(),
  updateItem: vi.fn(),
  uploadPhoto: vi.fn(),
}))

class ApiError extends Error {
  constructor(message, fields = {}, status = 0, code = null) {
    super(message)
    this.fields = fields
    this.status = status
    this.code = code
  }
}

const ana = { id: 1, email: 'ana@example.com', name: 'Ana' }
const vest = {
  id: 1,
  name: 'Crochet sweater vest',
  category: 'TOP',
  pricePaid: 120,
  savings: 0,
  costPerWear: 40,
  wearCount: 3,
  colorHex: '#D8CBB0',
}

/** Pretend the page was opened from an email link like /?verify=abc */
function openFrom(url) {
  window.history.replaceState(null, '', url)
  forgetEmailLink()
}

async function fillIn(email, password) {
  await userEvent.type(screen.getByLabelText('Email'), email)
  if (password) await userEvent.type(screen.getByLabelText('Password'), password)
}

describe('accounts in the app', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    openFrom('/')
    api.hasToken.mockReturnValue(false)
    api.listItems.mockResolvedValue([vest])
  })

  it('shows the log-in page when nobody is logged in', () => {
    render(<App />)
    expect(screen.getByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    expect(api.listItems).not.toHaveBeenCalled()
  })

  it('logs in and shows your closet', async () => {
    api.logIn.mockResolvedValue(ana)
    render(<App />)
    await fillIn('ana@example.com', 'thrift-forever')
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByRole('heading', { name: 'Crochet sweater vest' })).toBeInTheDocument()
    expect(screen.getByText('Ana')).toBeInTheDocument()
  })

  it('shows the error for a wrong password', async () => {
    api.logIn.mockRejectedValue(new ApiError("That email and password don't match", {}, 401))
    render(<App />)
    await fillIn('ana@example.com', 'nope')
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }))
    expect(await screen.findByRole('alert')).toHaveTextContent("That email and password don't match")
  })

  it('after signing up, asks you to check your inbox', async () => {
    api.signUp.mockResolvedValue({ message: "We've sent a link to ana@example.com." })
    render(<App />)
    await userEvent.click(screen.getByRole('button', { name: 'Create an account' }))
    await fillIn('ana@example.com', 'thrift-forever')
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }))

    expect(await screen.findByRole('heading', { name: 'Check your inbox' })).toBeInTheDocument()
    expect(screen.getByText("We've sent a link to ana@example.com.")).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Send the confirmation link again' })).toBeInTheDocument()
  })

  it('shows field errors from the server when signing up', async () => {
    api.signUp.mockRejectedValue(new ApiError('Some fields need fixing', { password: 'Use 8 to 72 characters' }, 400))
    render(<App />)
    await userEvent.click(screen.getByRole('button', { name: 'Create an account' }))
    await fillIn('ana@example.com', 'short')
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }))
    expect(await screen.findByText('Use 8 to 72 characters')).toBeInTheDocument()
  })

  it('offers to resend the link when logging in before confirming', async () => {
    api.logIn.mockRejectedValue(new ApiError('Confirm your email first.', {}, 403, 'EMAIL_NOT_VERIFIED'))
    api.resendVerification.mockResolvedValue({ message: "If that account still needs confirming, we've sent a new link." })
    render(<App />)
    await fillIn('ana@example.com', 'thrift-forever')
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Send the confirmation link again' }))

    expect(api.resendVerification).toHaveBeenCalledWith('ana@example.com')
    expect(await screen.findByRole('heading', { name: 'Check your inbox' })).toBeInTheDocument()
  })

  it('sends a reset link from Forgot password', async () => {
    api.forgotPassword.mockResolvedValue({ message: "If there's an account with that email, we've sent a link." })
    render(<App />)
    await userEvent.click(screen.getByRole('button', { name: 'Forgot password?' }))
    await fillIn('ana@example.com')
    await userEvent.click(screen.getByRole('button', { name: 'Send reset link' }))

    expect(api.forgotPassword).toHaveBeenCalledWith('ana@example.com')
    expect(await screen.findByText("If there's an account with that email, we've sent a link.")).toBeInTheDocument()
  })

  it('confirms your email from the link and opens your closet', async () => {
    openFrom('/?verify=code-from-email')
    api.verifyEmail.mockResolvedValue(ana)
    render(<App />)

    expect(await screen.findByRole('heading', { name: 'Crochet sweater vest' })).toBeInTheDocument()
    expect(api.verifyEmail).toHaveBeenCalledTimes(1)
    expect(api.verifyEmail).toHaveBeenCalledWith('code-from-email')
    expect(window.location.search).toBe('') // the code is removed from the address bar
  })

  it('explains an expired confirmation link', async () => {
    openFrom('/?verify=old-code')
    api.verifyEmail.mockRejectedValue(new ApiError('That link has expired or was already used. Log in to get a new one.', {}, 400))
    render(<App />)
    expect(await screen.findByRole('alert')).toHaveTextContent('That link has expired or was already used')
    expect(screen.getByRole('heading', { name: 'Log in' })).toBeInTheDocument()
  })

  it('sets a new password from a reset link and logs you in', async () => {
    openFrom('/?reset=reset-code')
    api.resetPassword.mockResolvedValue(ana)
    render(<App />)

    await userEvent.type(screen.getByLabelText('New password'), 'brand-new-pass')
    await userEvent.click(screen.getByRole('button', { name: 'Save and log in' }))

    expect(api.resetPassword).toHaveBeenCalledWith('reset-code', 'brand-new-pass')
    expect(await screen.findByRole('heading', { name: 'Crochet sweater vest' })).toBeInTheDocument()
  })

  it('remembers you with a saved token, and logs out', async () => {
    api.hasToken.mockReturnValue(true)
    api.getMe.mockResolvedValue(ana)
    render(<App />)
    expect(await screen.findByRole('heading', { name: 'Crochet sweater vest' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Log out' }))
    expect(api.logOut).toHaveBeenCalled()
    expect(screen.getByRole('heading', { name: 'Log in' })).toBeInTheDocument()
  })

  it('keeps your login and offers a retry when the server is asleep', async () => {
    api.hasToken.mockReturnValue(true)
    api.getMe.mockRejectedValueOnce(new ApiError("Can't reach the server.")).mockResolvedValueOnce(ana)
    render(<App />)
    expect(await screen.findByText(/may be waking up/)).toBeInTheDocument()
    expect(api.logOut).not.toHaveBeenCalled()
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByRole('heading', { name: 'Crochet sweater vest' })).toBeInTheDocument()
  })
})
