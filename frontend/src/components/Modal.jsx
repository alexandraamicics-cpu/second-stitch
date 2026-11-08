import { useEffect, useRef } from 'react'

// Accessible pop-up: focuses itself, closes on Escape, returns focus afterwards.
export default function Modal({ title, onClose, children }) {
  const panelRef = useRef(null)
  // Keep the latest onClose in a ref so the effect below runs only once.
  // (Otherwise focus would jump back to the first field on every keystroke.)
  const onCloseRef = useRef(onClose)
  onCloseRef.current = onClose

  useEffect(() => {
    const previouslyFocused = document.activeElement
    const firstField = panelRef.current?.querySelector('input, select, textarea')
    firstField?.focus()

    const onKey = (e) => {
      if (e.key === 'Escape') onCloseRef.current()
    }
    document.addEventListener('keydown', onKey)
    document.body.classList.add('no-scroll')
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.classList.remove('no-scroll')
      previouslyFocused?.focus?.()
    }
  }, [])

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title" ref={panelRef}>
        <div className="modal-head">
          <h2 id="modal-title">{title}</h2>
          <button type="button" className="btn btn-quiet btn-small" onClick={onClose}>
            Close
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
