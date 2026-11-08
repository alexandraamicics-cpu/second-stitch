import ItemPhoto from './ItemPhoto.jsx'
import WearStepper from './WearStepper.jsx'
import { categorySingular, formatMoney } from '../utils/format.js'

// One closet piece: the photo, with a thrift-store price tag hanging off it.
export default function HangTag({ item, onWear, onUnwear, onEdit, onDelete }) {
  const details = [item.brand, item.size && `Size ${item.size}`, item.color].filter(Boolean).join(', ')
  const saved = Number(item.savings) > 0

  return (
    <article className="piece" aria-labelledby={`item-${item.id}-name`}>
      <ItemPhoto item={item} />

      {saved && (
        <p className="sticker">
          <span>Saved</span> {formatMoney(item.savings)}
        </p>
      )}

      <div className="tag">
        <span className="tag-hole" aria-hidden="true" />
        <p className="tag-category">{categorySingular(item.category)}</p>
        <h3 id={`item-${item.id}-name`}>{item.name}</h3>
        {details && <p className="tag-details">{details}</p>}

        <p className="tag-price" aria-label={`Paid ${formatMoney(item.pricePaid)}`}>
          {formatMoney(item.pricePaid)}
        </p>

        <div className="tag-wears">
          <WearStepper
            count={item.wearCount}
            name={item.name}
            onAdd={() => onWear(item)}
            onRemove={() => onUnwear(item)}
          />
          <p>
            <strong>{formatMoney(item.costPerWear)}</strong> per wear
          </p>
        </div>

        <div className="tag-actions">
          <button type="button" className="btn btn-small" onClick={() => onEdit(item)}>
            Edit
          </button>
          <button
            type="button"
            className="btn btn-small btn-quiet"
            onClick={() => onDelete(item)}
            aria-label={`Delete ${item.name}`}
          >
            Delete
          </button>
        </div>
      </div>
    </article>
  )
}
