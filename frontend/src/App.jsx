import { useCallback, useEffect, useRef, useState } from 'react'
import { getMe, hasToken, logOut, verifyEmail } from './api.js'
import Logo from './components/Logo.jsx'
import AuthPage from './pages/AuthPage.jsx'
import ResetPasswordPage from './pages/ResetPasswordPage.jsx'
import { takeEmailLink } from './utils/emailLink.js'
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

  // Did they arrive from an email link (?verify=... or ?reset=...)?
  const [emailLink] = useState(takeEmailLink)
  const [resetCode, setResetCode] = useState(emailLink?.kind === 'reset' ? emailLink.code : null)
  const verifyStarted = useRef(false)

  // Who is logged in. 'checking' while we ask the server about a saved token.
  const [user, setUser] = useState(null)
  const [status, setStatus] = useState(() => {
    if (emailLink?.kind === 'verify') return 'verifying'
    return hasToken() ? 'checking' : 'ready'
  })

  // Which log-in screen to show, and any message on it (n makes it start fresh)
  const [authView, setAuthView] = useState({ mode: 'login', message: '', n: 0 })
  const showAuth = (mode, message = '') => setAuthView((v) => ({ mode, message, n: v.n + 1 }))

  // Short confirmation messages ("Added a wear to...") that screen readers also announce.
  const notify = useCallback((message) => {
    setNotice(message)
    clearTimeout(timer.current)
    timer.current = setTimeout(() => setNotice(''), 4000)
  }, [])

  const checkSavedLogin = useCallback(() => {
    setStatus('checking')
    getMe()
      .then((me) => {
        setUser(me)
        setStatus('ready')
      })
      .catch((err) => {
        if (err.status) {
          logOut() // the server answered: the token is no good
          setStatus('ready')
        } else {
          setStatus('offline') // the server didn't answer (e.g. waking up): keep the token and retry
        }
      })
  }, [])

  useEffect(() => {
    if (emailLink?.kind === 'verify') {
      if (verifyStarted.current) return // the link only works once, so never send it twice
      verifyStarted.current = true
      verifyEmail(emailLink.code)
        .then((me) => {
          setUser(me)
          setStatus('ready')
          notify('Email confirmed. Welcome to your closet!')
        })
        .catch((err) => {
          if (hasToken()) {
            // Already logged in (e.g. clicked an old link again): stay logged in, just mention it
            notify("That link was already used, but you're still logged in.")
            checkSavedLogin()
          } else {
            showAuth('login', err.message)
            setStatus('ready')
          }
        })
      return
    }
    if (hasToken()) checkSavedLogin()
  }, [emailLink, checkSavedLogin, notify])

  // api.js fires this when a request comes back "log in to continue"
  useEffect(() => {
    const onExpired = () => {
      setUser(null)
      notify('You were logged out. Log in again to continue.')
    }
    window.addEventListener('session-expired', onExpired)
    return () => window.removeEventListener('session-expired', onExpired)
  }, [notify])

  function handleAuthed(me) {
    setUser(me)
    setTab('closet')
    notify(`Welcome back${me.name ? `, ${me.name}` : ''}`)
  }

  function handleLogOut() {
    logOut()
    setUser(null)
    showAuth('login')
    notify('Logged out')
  }

  const noticeEl = (
    <p className={`notice ${notice ? 'is-visible' : ''}`} role="status" aria-live="polite">
      {notice}
    </p>
  )

  // The top bar on every screen (a thin plaid strip runs along its top edge).
  // Sections and the account menu only show once you're logged in.
  const topbar = (signedIn) => {
    const shownName = user ? user.name || user.email : ''
    return (
      <header className="topbar">
        <div className="shell topbar-inner">
          <Logo />
          {signedIn && (
            <>
              <nav aria-label="Sections" className="nav">
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
              <div className="account">
                <span className="avatar" aria-hidden="true">
                  {shownName.charAt(0).toUpperCase()}
                </span>
                <span className="account-name">{shownName}</span>
                <button type="button" className="btn btn-small btn-ghost" onClick={handleLogOut}>
                  Log out
                </button>
              </div>
            </>
          )}
        </div>
      </header>
    )
  }

  if (status === 'checking' || status === 'verifying') {
    return (
      <>
        {topbar(false)}
        <p className="quiet app-message">
          {status === 'verifying' ? 'Confirming your email…' : 'Opening your closet…'}
        </p>
      </>
    )
  }

  if (resetCode) {
    return (
      <>
        {topbar(false)}
        <main id="main" className="shell">
          <ResetPasswordPage
            code={resetCode}
            onDone={(me) => {
              setResetCode(null)
              setUser(me)
              setTab('closet')
              notify("Password changed. You're logged in.")
            }}
            onAskAgain={() => {
              setResetCode(null)
              showAuth('forgot')
            }}
          />
        </main>
        {noticeEl}
      </>
    )
  }

  if (status === 'offline') {
    return (
      <>
        {topbar(false)}
        <div className="app-message empty" role="alert">
          <p>Can&apos;t reach the server. It may be waking up, which takes about a minute.</p>
          <button type="button" className="btn" onClick={checkSavedLogin}>
            Try again
          </button>
        </div>
      </>
    )
  }

  if (!user) {
    return (
      <>
        {topbar(false)}
        <main id="main" className="shell">
          <AuthPage
            key={authView.n}
            initialMode={authView.mode}
            initialMessage={authView.message}
            onAuthed={handleAuthed}
          />
        </main>
        {noticeEl}
      </>
    )
  }

  return (
    <>
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      {topbar(true)}

      {/* key: switching accounts starts every page fresh */}
      <main id="main" className="shell app" tabIndex="-1" key={user.id}>
        {tab === 'closet' && <Closet notify={notify} />}
        {tab === 'mix' && <MixMatch notify={notify} />}
        {tab === 'stats' && <Stats />}
      </main>

      {noticeEl}
    </>
  )
}
