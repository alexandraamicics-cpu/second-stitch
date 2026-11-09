import { useCallback, useEffect, useRef, useState } from 'react'
import { clearOwnerPassword, getAuthStatus, hasOwnerPassword, setOwnerPassword } from './api.js'
import OwnerBar from './components/OwnerBar.jsx'
import Closet from './pages/Closet.jsx'
import MixMatch from './pages/MixMatch.jsx'
import Stats from './pages/Stats.jsx'

const TABS = [
  { id: 'closet', label: 'Closet' },
  { id: 'mix', label: 'Mix & match' },
  { id: 'stats', label: 'Savings' },
]

export default function App() {
  const [tab, setTab] = useState('closet')
  const [notice, setNotice] = useState('')
  const timer = useRef(null)

  // Short confirmation messages ("Logged a wear for...") that screen readers also announce.
  const notify = useCallback((message) => {
    setNotice(message)
    clearTimeout(timer.current)
    timer.current = setTimeout(() => setNotice(''), 4000)
  }, [])

  // Does the server need the owner password for changes? (null = still checking)
  const [serverLocked, setServerLocked] = useState(null)
  const [unlocked, setUnlocked] = useState(hasOwnerPassword)
  const canEdit = serverLocked === false || unlocked

  useEffect(() => {
    getAuthStatus()
      .then((status) => setServerLocked(status.locked))
      .catch((err) => {
        // An older backend without /api/auth has no password at all
        if (err.status === 404) setServerLocked(false)
      })
  }, [])

  // api.js fires this if the saved password stops working
  useEffect(() => {
    const onLocked = () => {
      setUnlocked(false)
      notify('Your saved password no longer works. Unlock editing again.')
    }
    window.addEventListener('owner-locked', onLocked)
    return () => window.removeEventListener('owner-locked', onLocked)
  }, [notify])

  function handleUnlock(password) {
    setOwnerPassword(password)
    setUnlocked(true)
    notify('Editing unlocked on this device')
  }

  function handleLock() {
    clearOwnerPassword()
    setUnlocked(false)
    notify('Editing locked')
  }

  return (
    <div className="app">
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      {serverLocked && <OwnerBar unlocked={unlocked} onUnlock={handleUnlock} onLock={handleLock} />}

      <header className="masthead">
        <h1 className="wordmark">
          Second <span>Stitch</span>
        </h1>
        <nav aria-label="Sections">
          <ul className="tabs">
            {TABS.map((t) => (
              <li key={t.id}>
                <button
                  type="button"
                  className="tab"
                  aria-current={tab === t.id ? 'page' : undefined}
                  onClick={() => setTab(t.id)}
                >
                  {t.label}
                </button>
              </li>
            ))}
          </ul>
        </nav>
      </header>

      <main id="main" tabIndex="-1">
        {tab === 'closet' && <Closet notify={notify} canEdit={canEdit} />}
        {tab === 'mix' && <MixMatch notify={notify} canEdit={canEdit} />}
        {tab === 'stats' && <Stats />}
      </main>

      <p className={`notice ${notice ? 'is-visible' : ''}`} role="status" aria-live="polite">
        {notice}
      </p>
    </div>
  )
}
