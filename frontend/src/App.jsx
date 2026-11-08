import { useCallback, useRef, useState } from 'react'
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

  return (
    <div className="app">
      <a className="skip-link" href="#main">
        Skip to content
      </a>
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
        {tab === 'closet' && <Closet notify={notify} />}
        {tab === 'mix' && <MixMatch notify={notify} />}
        {tab === 'stats' && <Stats />}
      </main>

      <p className={`notice ${notice ? 'is-visible' : ''}`} role="status" aria-live="polite">
        {notice}
      </p>
    </div>
  )
}
