import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MixMatch from './MixMatch.jsx'
import * as api from '../api.js'

vi.mock('../api.js', () => ({
  apiUrl: (path) => path,
  listItems: vi.fn(),
  listOutfits: vi.fn(),
  createOutfit: vi.fn(),
  wearOutfit: vi.fn(),
  unwearOutfit: vi.fn(),
  deleteOutfit: vi.fn(),
}))

const piece = (id, name, category, pricePaid, estimatedRetail) => ({
  id,
  name,
  category,
  pricePaid,
  estimatedRetail,
  savings: estimatedRetail ? estimatedRetail - pricePaid : 0,
  costPerWear: pricePaid,
  wearCount: 0,
  colorHex: '#9CAF88',
  photoUrl: null,
})

const linen = piece(1, 'Linen button-up', 'TOP', 180, 1490)
const vest = piece(2, 'Crochet vest', 'TOP', 120, null)
const jeans = piece(3, 'Straight-leg 501s', 'BOTTOM', 350, 4290)
const loafers = piece(4, 'Penny loafers', 'SHOES', 450, 3800)
const slip = piece(5, 'Slip dress', 'DRESS', 280, 2600)

const look = () => screen.getByRole('form', { name: 'This look' })
const lookNames = () =>
  within(look())
    .queryAllByRole('listitem')
    .map((li) => li.querySelector('.look-name').textContent)
const rackButton = (name) => screen.getByRole('button', { name: new RegExp(`^${name}`) })

async function renderMixer(notify = vi.fn()) {
  render(<MixMatch notify={notify} />)
  await screen.findByRole('heading', { name: 'Mix & match' })
  return notify
}

describe('Mix & match page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.listItems.mockResolvedValue([linen, vest, jeans, loafers, slip])
    api.listOutfits.mockResolvedValue([])
  })

  it('shows every piece on the rack, grouped by category, with an empty look', async () => {
    await renderMixer()
    expect(screen.getByRole('heading', { name: 'Tops' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Dresses' })).toBeInTheDocument()
    expect(rackButton('Crochet vest')).toHaveAttribute('aria-pressed', 'false')
    expect(lookNames()).toEqual([])
  })

  it('adds clicked pieces to the look, head to toe, and adds up the price', async () => {
    await renderMixer()
    await userEvent.click(rackButton('Penny loafers'))
    await userEvent.click(rackButton('Linen button-up'))
    await userEvent.click(rackButton('Straight-leg 501s'))

    expect(lookNames()).toEqual(['Linen button-up', 'Straight-leg 501s', 'Penny loafers'])
    expect(rackButton('Linen button-up')).toHaveAttribute('aria-pressed', 'true')
    expect(within(look()).getByText('₱980')).toBeInTheDocument() // you paid
    expect(within(look()).getByText('₱8,600')).toBeInTheDocument() // saved: 1,310 + 3,940 + 3,350
  })

  it('swaps the top when you pick another one', async () => {
    const notify = await renderMixer()
    await userEvent.click(rackButton('Linen button-up'))
    await userEvent.click(rackButton('Crochet vest'))
    expect(lookNames()).toEqual(['Crochet vest'])
    expect(notify).toHaveBeenLastCalledWith('Swapped Linen button-up for Crochet vest')
  })

  it('a dress replaces the top and bottom', async () => {
    await renderMixer()
    await userEvent.click(rackButton('Linen button-up'))
    await userEvent.click(rackButton('Straight-leg 501s'))
    await userEvent.click(rackButton('Slip dress'))
    expect(lookNames()).toEqual(['Slip dress'])
  })

  it('takes a piece off from the rack or with the × button', async () => {
    await renderMixer()
    await userEvent.click(rackButton('Linen button-up'))
    await userEvent.click(rackButton('Penny loafers'))

    await userEvent.click(rackButton('Linen button-up'))
    expect(lookNames()).toEqual(['Penny loafers'])

    await userEvent.click(screen.getByRole('button', { name: 'Take off Penny loafers' }))
    expect(lookNames()).toEqual([])
  })

  it('fills the gaps without changing what you picked', async () => {
    await renderMixer()
    await userEvent.click(rackButton('Crochet vest'))
    await userEvent.click(screen.getByRole('button', { name: 'Fill the gaps for me' }))
    expect(lookNames()).toEqual(['Crochet vest', 'Straight-leg 501s', 'Penny loafers'])
  })

  it('saves the look as an outfit', async () => {
    api.createOutfit.mockResolvedValue({
      id: 9,
      name: 'Errand day',
      occasion: null,
      wearCount: 0,
      items: [linen, jeans],
      totalPaid: 530,
      totalSaved: 5250,
    })
    await renderMixer()
    await userEvent.click(rackButton('Straight-leg 501s'))
    await userEvent.click(rackButton('Linen button-up'))
    await userEvent.type(screen.getByLabelText('Outfit name'), 'Errand day')
    await userEvent.click(screen.getByRole('button', { name: 'Save outfit' }))

    expect(api.createOutfit).toHaveBeenCalledWith({ name: 'Errand day', occasion: '', itemIds: [1, 3] })
    expect(await screen.findByRole('heading', { name: 'Errand day' })).toBeInTheDocument()
  })

  it('asks for pieces when the closet is empty', async () => {
    api.listItems.mockResolvedValue([])
    render(<MixMatch notify={vi.fn()} />)
    expect(await screen.findByText(/Add a few pieces on the Closet tab/)).toBeInTheDocument()
  })
})
