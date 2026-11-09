import { describe, expect, it } from 'vitest'
import { fitWithin, shrinkImage } from './image.js'

describe('fitWithin', () => {
  it('shrinks a landscape photo so its long side is the max', () => {
    expect(fitWithin(4000, 3000, 1200)).toEqual({ width: 1200, height: 900 })
  })

  it('shrinks a portrait photo the same way', () => {
    expect(fitWithin(3024, 4032, 1200)).toEqual({ width: 900, height: 1200 })
  })

  it('never makes a small photo bigger', () => {
    expect(fitWithin(800, 600, 1200)).toEqual({ width: 800, height: 600 })
  })
})

describe('shrinkImage', () => {
  it('falls back to the original file if the browser cannot read it', async () => {
    const file = new File(['not really an image'], 'notes.png', { type: 'image/png' })
    // The test environment has no createImageBitmap, so this exercises the fallback
    expect(await shrinkImage(file)).toBe(file)
  })
})
