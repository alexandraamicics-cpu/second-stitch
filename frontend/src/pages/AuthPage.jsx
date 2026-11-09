import { useState } from 'react'
import { forgotPassword, logIn, resendVerification, signUp } from '../api.js'

// Everything before you're logged in, on one kraft price tag:
//   login  → log in (or "Forgot password?" / "Create an account")
//   signup → create an account, then "check your inbox"
//   forgot → ask for a reset link, then "check your inbox"
//   sent   → "check your inbox", with a button to send the confirmation link again
const HEADINGS = {
  login: 'Log in',
  signup: 'Create your closet',
  forgot: 'Forgot your password?',
  sent: 'Check your inbox',
}

export default function AuthPage({ onAuthed, initialMode = 'login', initialMessage = '' }) {
  const [mode, setMode] = useState(initialMode)
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [showPassword, setShowPassword] = useState(false)
  const [errors, setErrors] = useState({})
  const [message, setMessage] = useState(initialMessage) // problems, shown as an alert
  const [info, setInfo] = useState('') // "we've sent you a link" text on the inbox screen
  const [canResend, setCanResend] = useState(false) // offer "send the link again"
  const [busy, setBusy] = useState(false)

  const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }))

  function goTo(next) {
    setMode(next)
    setErrors({})
    setMessage('')
    setCanResend(false)
  }

  async function run(action) {
    setBusy(true)
    setErrors({})
    setMessage('')
    try {
      await action()
    } catch (err) {
      setErrors(err.fields ?? {})
      setMessage(err.message)
      setCanResend(err.code === 'EMAIL_NOT_VERIFIED')
    } finally {
      setBusy(false)
    }
  }

  function showInbox(text, offerResend) {
    setInfo(text)
    setMode('sent')
    setCanResend(offerResend)
  }

  function handleSubmit(e) {
    e.preventDefault()
    if (mode === 'login') {
      run(async () => onAuthed(await logIn({ email: form.email, password: form.password }), false))
    } else if (mode === 'signup') {
      run(async () => {
        const result = await signUp({ email: form.email, password: form.password, name: form.name })
        showInbox(result.message, true)
      })
    } else if (mode === 'forgot') {
      run(async () => showInbox((await forgotPassword(form.email)).message, false))
    }
  }

  function handleResend() {
    run(async () => showInbox((await resendVerification(form.email)).message, true))
  }

  const errorFor = (field) =>
    errors[field] && (
      <p className="field-error" id={`auth-${field}-error`}>
        {errors[field]}
      </p>
    )
  const describedBy = (field, hint) =>
    [errors[field] && `auth-${field}-error`, hint].filter(Boolean).join(' ') || undefined

  const submitLabel = {
    login: busy ? 'Logging in…' : 'Log in',
    signup: busy ? 'Creating…' : 'Create account',
    forgot: busy ? 'Sending…' : 'Send reset link',
  }[mode]

  return (
    <div className="auth">
      <div className="auth-intro">
        <h1>Track what you thrift.</h1>
        <p className="auth-tagline">
          Log your finds, mix outfits from what you own, and see what every rewear saves you.
        </p>
      </div>

      <form className="auth-card" onSubmit={handleSubmit} noValidate aria-labelledby="auth-heading">
        <h2 id="auth-heading">{HEADINGS[mode]}</h2>

        {mode === 'sent' && <p className="auth-info">{info}</p>}
        {mode === 'forgot' && (
          <p className="auth-info">Enter your account&apos;s email and we&apos;ll send you a link to choose a new password.</p>
        )}

        {mode === 'signup' && (
          <div className="field">
            <label htmlFor="auth-name">Name (optional)</label>
            <input id="auth-name" autoComplete="given-name" value={form.name} onChange={set('name')} />
            {errorFor('name')}
          </div>
        )}

        {mode !== 'sent' && (
          <div className="field">
            <label htmlFor="auth-email">Email</label>
            <input
              id="auth-email"
              type="email"
              autoComplete="email"
              value={form.email}
              onChange={set('email')}
              aria-invalid={!!errors.email}
              aria-describedby={describedBy('email')}
            />
            {errorFor('email')}
          </div>
        )}

        {(mode === 'login' || mode === 'signup') && (
          <div className="field">
            <label htmlFor="auth-password">Password</label>
            <div className="password-row">
              <input
                id="auth-password"
                type={showPassword ? 'text' : 'password'}
                autoComplete={mode === 'signup' ? 'new-password' : 'current-password'}
                value={form.password}
                onChange={set('password')}
                aria-invalid={!!errors.password}
                aria-describedby={describedBy('password', mode === 'signup' && 'auth-password-hint')}
              />
              <button
                type="button"
                className="btn btn-small btn-quiet"
                onClick={() => setShowPassword((v) => !v)}
                aria-pressed={showPassword}
              >
                {showPassword ? 'Hide' : 'Show'}
              </button>
            </div>
            {mode === 'signup' && (
              <p className="field-hint" id="auth-password-hint">
                At least 8 characters.
              </p>
            )}
            {errorFor('password')}
            {mode === 'login' && (
              <button type="button" className="link-button forgot-link" onClick={() => goTo('forgot')}>
                Forgot password?
              </button>
            )}
          </div>
        )}

        {message && Object.keys(errors).length === 0 && (
          <p className="form-message" role="alert">
            {message}
          </p>
        )}

        {mode !== 'sent' && (
          <button type="submit" className="btn btn-primary" disabled={busy}>
            {submitLabel}
          </button>
        )}

        {canResend && (
          <button type="button" className="btn btn-small resend" onClick={handleResend} disabled={busy}>
            Send the confirmation link again
          </button>
        )}

        <p className="auth-switch">
          {mode === 'login' && (
            <>
              New here?{' '}
              <button type="button" className="link-button" onClick={() => goTo('signup')}>
                Create an account
              </button>
            </>
          )}
          {mode === 'signup' && (
            <>
              Already have an account?{' '}
              <button type="button" className="link-button" onClick={() => goTo('login')}>
                Log in
              </button>
            </>
          )}
          {(mode === 'forgot' || mode === 'sent') && (
            <button type="button" className="link-button" onClick={() => goTo('login')}>
              Back to log in
            </button>
          )}
        </p>
      </form>
    </div>
  )
}
