// A − count + control for logging wears. A mis-click is one tap to undo.
export default function WearStepper({ count, name, onAdd, onRemove, label = 'Times worn' }) {
  return (
    <div className="wear-stepper" role="group" aria-label={`${label}: ${name}`}>
      <span className="wear-label">{label}</span>
      <button
        type="button"
        className="step-btn"
        aria-label={`Remove a wear from ${name}`}
        disabled={count === 0}
        onClick={onRemove}
      >
        −
      </button>
      <output className="wear-count" aria-live="polite">
        {count}
      </output>
      <button type="button" className="step-btn" aria-label={`Add a wear to ${name}`} onClick={onAdd}>
        +
      </button>
    </div>
  )
}
