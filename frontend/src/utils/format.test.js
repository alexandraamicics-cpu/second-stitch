import { describe, expect, it } from 'vitest'
import { categoryLabel, formatMoney, wearLabel } from './format.js'

describe('formatMoney', () => {
  it('formats pesos without needless decimals', () => {
    expect(formatMoney(350)).toBe('₱350')
  })

  it('keeps centavos when there are some', () => {
    expect(formatMoney(33.33)).toBe('₱33.33')
  })

  it('always shows two decimals for centavos', () => {
    expect(formatMoney(58.4)).toBe('₱58.40')
  })

  it('adds thousands separators', () => {
    expect(formatMoney(4290)).toBe('₱4,290')
  })

  it('accepts numbers sent as strings', () => {
    expect(formatMoney('180.00')).toBe('₱180')
  })

  it('shows a dash for missing values', () => {
    expect(formatMoney(null)).toBe('—')
    expect(formatMoney(undefined)).toBe('—')
    expect(formatMoney('abc')).toBe('—')
  })
})

describe('wearLabel', () => {
  it.each([
    [0, 'Not worn yet'],
    [1, 'Worn once'],
    [7, 'Worn 7 times'],
  ])('%i wears → %s', (count, label) => {
    expect(wearLabel(count)).toBe(label)
  })
})

describe('categoryLabel', () => {
  it('turns API values into readable labels', () => {
    expect(categoryLabel('OUTERWEAR')).toBe('Outerwear')
    expect(categoryLabel('ACCESSORY')).toBe('Accessories')
  })
})
