import { useState } from 'react'
import { resetPassword } from '../api.js'

// Opened from the link in a "Reset your password" email (?reset=...).
export default function ResetPasswordPage({ code, onDone, onAskAgain }) {
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [linkProblem, setLinkProblem] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      onDone(await resetPassword(code, password))
    } catch (err) {
      if (err.fields?.password) setError(err.fields.password)
      else setLinkProblem(err.message)
      setBusy(false)
    }
  }

  return (
    <div className="auth">
      <div className="auth-intro">
        <h1>Almost back in.</h1>
        <p className="auth-tagline">Choose a new password and you&apos;ll go straight to your closet.</p>
      </div>

      <form className="auth-card" onSubmit={handleSubmit} noValidate aria-labelledby="reset-heading">
        <h2 id="reset-heading">Choose a new password</h2>

        {linkProblem ? (
          <>
            <p className="form-message" role="alert">
              {linkProblem}
            </p>
            <button type="button" className="btn btn-primary" onClick={onAskAgain}>
              Send me a new link
            </button>
          </>
        ) : (
          <>
            <div className="field">
              <label htmlFor="reset-password">New password</label>
              <div className="password-row">
                <input
                  id="reset-password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  aria-invalid={!!error}
                  aria-describedby={error ? 'reset-password-error reset-hint' : 'reset-hint'}
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
              <p className="field-hint" id="reset-hint">
                At least 8 characters.
              </p>
              {error && (
                <p className="field-error" id="reset-password-error">
                  {error}
                </p>
              )}
            </div>
            <button type="submit" className="btn btn-primary" disabled={busy}>
              {busy ? 'Saving…' : 'Save and log in'}
            </button>
          </>
        )}
      </form>
    </div>
  )
}
