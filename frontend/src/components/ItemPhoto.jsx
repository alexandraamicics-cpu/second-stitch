import { useState } from 'react'
import { apiUrl } from '../api.js'

// Shows the item's photo, or a simple garment outline on its colour swatch
// when there is no photo yet (or the photo can't be loaded).

const SHAPES = {
  TOP: 'M30 22 L44 13 Q50 20 56 13 L70 22 L82 38 L71 44 L68 37 L68 86 L32 86 L32 37 L29 44 L18 38 Z',
  BOTTOM: 'M31 12 H69 L73 88 H56 L50 42 L44 88 H27 Z',
  DRESS: 'M41 10 H59 L60 32 L77 88 H23 L40 32 Z',
  OUTERWEAR:
    'M30 18 L44 12 L50 32 L56 12 L70 18 L83 72 L72 74 L68 42 L68 88 H32 L32 42 L28 74 L17 72 Z',
  SHOES: 'M16 64 Q18 48 34 50 L48 56 Q60 61 75 60 Q86 62 86 73 L86 78 H16 Z',
  ACCESSORY: 'M27 42 H73 L77 86 H23 Z',
}

export function Silhouette({ category }) {
  return (
    <svg className="silhouette" viewBox="0 0 100 100" aria-hidden="true">
      <path d={SHAPES[category] ?? SHAPES.TOP} />
      {category === 'ACCESSORY' && <path className="strap" d="M39 42 Q39 22 50 22 Q61 22 61 42" />}
    </svg>
  )
}

export default function ItemPhoto({ item, className = '' }) {
  const swatch = item.colorHex || '#C9CBC0'
  // If a photo fails to load, remember which one, and show the outline instead of a broken image
  const [failedUrl, setFailedUrl] = useState(null)
  const showPhoto = item.photoUrl && failedUrl !== item.photoUrl

  return (
    <div className={`item-photo ${className}`} style={{ '--swatch': swatch }}>
      {showPhoto ? (
        <img
          src={apiUrl(item.photoUrl)}
          alt={item.name}
          loading="lazy"
          onError={() => setFailedUrl(item.photoUrl)}
        />
      ) : (
        <Silhouette category={item.category} />
      )}
    </div>
  )
}
