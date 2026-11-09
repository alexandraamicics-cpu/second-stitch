// The logic behind the Mix & match page, kept free of React so it's easy to test.
// A "look" is just an array of pieces, always kept in head-to-toe order.

/** Head-to-toe order the look is shown in. */
export const LAYER_ORDER = ['OUTERWEAR', 'TOP', 'DRESS', 'BOTTOM', 'SHOES', 'ACCESSORY']

/** Pieces that can't be worn together: adding one takes the others off. */
const REPLACES = {
  TOP: ['TOP', 'DRESS'],
  BOTTOM: ['BOTTOM', 'DRESS'],
  DRESS: ['DRESS', 'TOP', 'BOTTOM'],
  OUTERWEAR: ['OUTERWEAR'],
  SHOES: ['SHOES'],
  ACCESSORY: [], // accessories stack: wear as many as you like
}

const byLayer = (a, b) => LAYER_ORDER.indexOf(a.category) - LAYER_ORDER.indexOf(b.category)

/** { TOP: [...], BOTTOM: [...], ... } with every category present. */
export function groupByCategory(items) {
  const racks = {}
  for (const category of LAYER_ORDER) racks[category] = []
  for (const item of items) racks[item.category]?.push(item)
  return racks
}

export const isInLook = (look, item) => look.some((piece) => piece.id === item.id)

export function removeFromLook(look, item) {
  return look.filter((piece) => piece.id !== item.id)
}

/**
 * Adds a piece to the look, taking off anything it can't be worn with.
 * Returns the new look and the pieces that were taken off (for the message).
 */
export function addToLook(look, item) {
  const clashes = REPLACES[item.category] ?? []
  const removed = look.filter((piece) => clashes.includes(piece.category))
  const kept = look.filter((piece) => !clashes.includes(piece.category))
  return { look: [...kept, item].sort(byLayer), removed }
}

const pick = (list, random) => list[Math.floor(random() * list.length)]

/**
 * Completes the look without touching what you already picked:
 * adds a missing top or bottom (or a dress if you have nothing yet), and shoes.
 * `random` is passed in so tests can make the result predictable.
 */
export function fillGaps(look, racks, random = Math.random) {
  let next = [...look]
  const has = (category) => next.some((piece) => piece.category === category)

  if (!has('DRESS')) {
    const hasSeparate = has('TOP') || has('BOTTOM')
    const canDoSeparates = racks.TOP.length > 0 && racks.BOTTOM.length > 0
    const useDress = !hasSeparate && racks.DRESS.length > 0 && (!canDoSeparates || random() < 0.35)

    if (useDress) {
      next.push(pick(racks.DRESS, random))
    } else {
      if (!has('TOP') && racks.TOP.length > 0) next.push(pick(racks.TOP, random))
      if (!has('BOTTOM') && racks.BOTTOM.length > 0) next.push(pick(racks.BOTTOM, random))
    }
  }
  if (!has('SHOES') && racks.SHOES.length > 0) next.push(pick(racks.SHOES, random))

  return next.sort(byLayer)
}

const round = (n) => Math.round(n * 100) / 100
const total = (items, field) => round(items.reduce((sum, item) => sum + (Number(item[field]) || 0), 0))

/** Price details for a whole look. */
export function lookTotals(items) {
  return {
    paid: total(items, 'pricePaid'),
    // Only pieces where you know the price new count towards this
    retail: total(items, 'estimatedRetail'),
    saved: total(items, 'savings'),
    // What wearing this whole look has cost you per wear so far
    costPerWear: total(items, 'costPerWear'),
  }
}

/** Loads a saved outfit back into the look, using the closet's latest copy of each piece. */
export function lookFromOutfit(outfit, items) {
  const ids = new Set(outfit.items.map((piece) => piece.id))
  return items.filter((item) => ids.has(item.id)).sort(byLayer)
}

/** A short label for the look: what's still missing, or "Complete". */
export function lookStatus(items) {
  const has = (category) => items.some((piece) => piece.category === category)
  if (items.length === 0) return { label: 'Empty', complete: false }
  if (!(has('DRESS') || (has('TOP') && has('BOTTOM')))) {
    if (has('TOP')) return { label: 'Needs a bottom', complete: false }
    if (has('BOTTOM')) return { label: 'Needs a top', complete: false }
    return { label: 'Needs a top and bottom, or a dress', complete: false }
  }
  if (!has('SHOES')) return { label: 'Needs shoes', complete: false }
  return { label: 'Complete', complete: true }
}
