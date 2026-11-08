import { describe, expect, it } from 'vitest'
import { addToLook, fillGaps, groupByCategory, lookFromOutfit, lookTotals, removeFromLook } from './mixer.js'

const top1 = { id: 1, category: 'TOP', pricePaid: 180, estimatedRetail: 1490, savings: 1310, costPerWear: 20 }
const top2 = { id: 2, category: 'TOP', pricePaid: 120, estimatedRetail: null, savings: 0, costPerWear: 40 }
const jeans = { id: 3, category: 'BOTTOM', pricePaid: 350, estimatedRetail: 4290, savings: 3940, costPerWear: 25 }
const dress = { id: 4, category: 'DRESS', pricePaid: 280, estimatedRetail: 2600, savings: 2320, costPerWear: 280 }
const jacket = { id: 5, category: 'OUTERWEAR', pricePaid: 650, estimatedRetail: 5500, savings: 4850, costPerWear: 108.33 }
const loafers = { id: 6, category: 'SHOES', pricePaid: 450, estimatedRetail: 3800, savings: 3350, costPerWear: 56.25 }
const scarf = { id: 7, category: 'ACCESSORY', pricePaid: 60, estimatedRetail: 900, savings: 840, costPerWear: 30 }
const bag = { id: 8, category: 'ACCESSORY', pricePaid: 90, estimatedRetail: null, savings: 0, costPerWear: 90 }
const closet = [top1, top2, jeans, dress, jacket, loafers, scarf, bag]
const ids = (look) => look.map((piece) => piece.id)

describe('addToLook', () => {
  it('keeps the look in head-to-toe order', () => {
    let look = addToLook([], loafers).look
    look = addToLook(look, jeans).look
    look = addToLook(look, jacket).look
    look = addToLook(look, top1).look
    expect(ids(look)).toEqual([5, 1, 3, 6])
  })

  it('swaps a top for the new top', () => {
    const { look, removed } = addToLook([top1, jeans], top2)
    expect(ids(look)).toEqual([2, 3])
    expect(removed).toEqual([top1])
  })

  it('a dress takes off the top and bottom', () => {
    const { look, removed } = addToLook([top1, jeans, loafers], dress)
    expect(ids(look)).toEqual([4, 6])
    expect(ids(removed)).toEqual([1, 3])
  })

  it('a top takes off the dress', () => {
    expect(ids(addToLook([dress, loafers], top1).look)).toEqual([1, 6])
  })

  it('accessories stack', () => {
    const { look, removed } = addToLook([top1, scarf], bag)
    expect(ids(look)).toEqual([1, 7, 8])
    expect(removed).toEqual([])
  })
})

describe('removeFromLook', () => {
  it('takes one piece off', () => {
    expect(ids(removeFromLook([top1, jeans, loafers], jeans))).toEqual([1, 6])
  })
})

describe('fillGaps', () => {
  const racks = groupByCategory(closet)

  it('keeps what you picked and adds the missing bottom and shoes', () => {
    expect(ids(fillGaps([top2], racks, () => 0.9))).toEqual([2, 3, 6])
  })

  it('adds a dress and shoes to an empty look when random says so', () => {
    expect(ids(fillGaps([], racks, () => 0.1))).toEqual([4, 6])
  })

  it('leaves a complete look alone', () => {
    expect(ids(fillGaps([dress, loafers], racks))).toEqual([4, 6])
  })

  it('does nothing when the closet has no pieces to add', () => {
    expect(fillGaps([], groupByCategory([]))).toEqual([])
  })
})

describe('lookTotals', () => {
  it('adds up the price details for the look', () => {
    expect(lookTotals([top1, jeans, jacket])).toEqual({
      paid: 1180,
      retail: 11280,
      saved: 10100,
      costPerWear: 153.33,
    })
  })

  it('treats an unknown price new as zero', () => {
    expect(lookTotals([top2]).retail).toBe(0)
  })
})

describe('lookFromOutfit', () => {
  it('loads a saved outfit using the closet copy of each piece', () => {
    const worn = { ...loafers, wearCount: 9 }
    const look = lookFromOutfit({ items: [loafers, dress] }, [top1, dress, worn])
    expect(look).toEqual([dress, worn])
  })
})
