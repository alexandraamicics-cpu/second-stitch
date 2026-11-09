import { useState } from 'react'
import { checkOwnerPassword } from '../api.js'
import Modal from './Modal.jsx'

// The strip at the top of the live site: "viewing only" for visitors,
// "editing unlocked" for the owner.
export default function OwnerBar({ unlocked, onUnlock, onLock }) {
  const [asking, setAsking] = useState(false)

  return (
    <>
      <div className={`owner-bar ${unlocked ? 'is-unlocked' : ''}`}>
        <p>
          {unlocked
            ? 'Editing unlocked on this device.'
            : "You're viewing this closet. Changes are turned off."}
        </p>
        <button type="button" className="btn btn-small" onClick={unlocked ? onLock : () => setAsking(true)}>
          {unlocked ? 'Lock editing' : 'Unlock editing'}
        </button>
      </div>

      {asking && (
        <Modal title="Unlock editing" onClose={() => setAsking(false)}>
          <UnlockForm
            onUnlocked={(password) => {
              setAsking(false)
              onUnlock(password)
            }}
            onCancel={() => setAsking(false)}
          />
        </Modal>
      )}
    </>
  )
}

function UnlockForm({ onUnlocked, onCancel }) {
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [checking, setChecking] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    if (!password) {
      setError('Enter the owner password')
      return
    }
    setChecking(true)
    setError('')
    try {
      await checkOwnerPassword(password)
      onUnlocked(password)
    } catch (err) {
      setError(err.message)
      setChecking(false)
    }
  }

  return (
    <form className="item-form" onSubmit={handleSubmit} noValidate>
      <p className="quiet">Only the closet's owner can add, edit or delete pieces.</p>
      <div className="field">
        <label htmlFor="owner-password">Owner password</label>
        <input
          id="owner-password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          aria-invalid={!!error}
          aria-describedby={error ? 'owner-password-error' : undefined}
        />
        {error && (
          <p className="field-error" id="owner-password-error" role="alert">
            {error}
          </p>
        )}
      </div>
      <div className="form-actions">
        <button type="button" className="btn" onClick={onCancel}>
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={checking}>
          {checking ? 'Checking…' : 'Unlock editing'}
        </button>
      </div>
    </form>
  )
}
