// Small, pure helpers. Each one has tests in format.test.js.

// Whole pesos show as ₱350; anything with centavos always gets two decimals (₱58.40).
const wholePesos = new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP', maximumFractionDigits: 0 })
const withCentavos = new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP', minimumFractionDigits: 2 })

export function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '—'
  const n = Number(value)
  if (!Number.isFinite(n)) return '—'
  return Number.isInteger(n) ? wholePesos.format(n) : withCentavos.format(n)
}

export const CATEGORIES = ['TOP', 'BOTTOM', 'DRESS', 'OUTERWEAR', 'SHOES', 'ACCESSORY']

const CATEGORY_LABELS = {
  TOP: 'Tops',
  BOTTOM: 'Bottoms',
  DRESS: 'Dresses',
  OUTERWEAR: 'Outerwear',
  SHOES: 'Shoes',
  ACCESSORY: 'Accessories',
}

const CATEGORY_SINGULAR = {
  TOP: 'Top',
  BOTTOM: 'Bottom',
  DRESS: 'Dress',
  OUTERWEAR: 'Outerwear',
  SHOES: 'Shoes',
  ACCESSORY: 'Accessory',
}

export const categoryLabel = (c) => CATEGORY_LABELS[c] ?? c
export const categorySingular = (c) => CATEGORY_SINGULAR[c] ?? c

export function wearLabel(count) {
  if (!count) return 'Not worn yet'
  if (count === 1) return 'Worn once'
  return `Worn ${count} times`
}
