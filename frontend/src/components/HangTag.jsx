import ItemPhoto from './ItemPhoto.jsx'
import WearStepper from './WearStepper.jsx'
import { categorySingular, formatMoney } from '../utils/format.js'

// One closet piece as a card: photo (with a "saved" badge), name and price, wears, actions.
export default function HangTag({ item, onWear, onUnwear, onEdit, onDelete, canEdit = true }) {
  const details = [categorySingular(item.category), item.brand, item.size && `Size ${item.size}`, item.color]
    .filter(Boolean)
    .join(' · ')
  const saved = Number(item.savings) > 0

  return (
    <article className="piece" aria-labelledby={`item-${item.id}-name`}>
      <div className="piece-photo">
        <ItemPhoto item={item} />
        {saved && (
          <p className="saved-badge">
            <span>Saved</span> {formatMoney(item.savings)}
          </p>
        )}
      </div>

      <div className="piece-body">
        <div className="piece-row">
          <h3 id={`item-${item.id}-name`}>{item.name}</h3>
          <p className="price" aria-label={`Paid ${formatMoney(item.pricePaid)}`}>
            {formatMoney(item.pricePaid)}
          </p>
        </div>
        <p className="piece-meta">{details}</p>

        <div className="piece-wears">
          <WearStepper
            count={item.wearCount}
            name={item.name}
            onAdd={() => onWear(item)}
            onRemove={() => onUnwear(item)}
            readOnly={!canEdit}
          />
          <p className="piece-cpw">
            <strong>{formatMoney(item.costPerWear)}</strong> per wear
          </p>
        </div>
      </div>

      {canEdit && (
        <div className="piece-actions">
          <button type="button" className="btn btn-small btn-ghost" onClick={() => onEdit(item)}>
            Edit
          </button>
          <button
            type="button"
            className="btn btn-small btn-ghost btn-danger"
            onClick={() => onDelete(item)}
            aria-label={`Delete ${item.name}`}
          >
            Delete
          </button>
        </div>
      )}
    </article>
  )
}
