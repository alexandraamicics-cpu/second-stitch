import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import HangTag from './HangTag.jsx'

const jeans = {
  id: 7,
  name: 'Straight-leg 501s',
  category: 'BOTTOM',
  brand: "Levi's",
  size: '28',
  color: 'faded indigo',
  colorHex: '#5B7493',
  pricePaid: 350,
  savings: 3940,
  costPerWear: 25,
  wearCount: 14,
  photoUrl: null,
}

const renderTag = (item = jeans) => {
  const handlers = { onWear: vi.fn(), onUnwear: vi.fn(), onEdit: vi.fn(), onDelete: vi.fn() }
  render(<HangTag item={item} {...handlers} />)
  return handlers
}

describe('HangTag', () => {
  it('shows the name, price and cost per wear', () => {
    renderTag()
    expect(screen.getByRole('heading', { name: 'Straight-leg 501s' })).toBeInTheDocument()
    expect(screen.getByLabelText('Paid ₱350')).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('14')
    expect(screen.getByText('₱25')).toBeInTheDocument()
  })

  it('shows a savings sticker when the piece was cheaper than new', () => {
    renderTag()
    expect(screen.getByText('₱3,940', { exact: false })).toBeInTheDocument()
  })

  it('hides the sticker when there are no savings', () => {
    renderTag({ ...jeans, savings: 0 })
    expect(screen.queryByText('Saved')).not.toBeInTheDocument()
  })

  it('adds a wear with the + button', async () => {
    const { onWear } = renderTag()
    await userEvent.click(screen.getByRole('button', { name: 'Add a wear to Straight-leg 501s' }))
    expect(onWear).toHaveBeenCalledWith(jeans)
  })

  it('removes a wear with the − button, to undo a mis-click', async () => {
    const { onUnwear } = renderTag()
    await userEvent.click(screen.getByRole('button', { name: 'Remove a wear from Straight-leg 501s' }))
    expect(onUnwear).toHaveBeenCalledWith(jeans)
  })

  it('disables − when the piece has never been worn', () => {
    renderTag({ ...jeans, wearCount: 0 })
    expect(screen.getByRole('button', { name: 'Remove a wear from Straight-leg 501s' })).toBeDisabled()
  })

  it('shows the wear count but no buttons when editing is locked', () => {
    render(<HangTag item={jeans} onWear={vi.fn()} onUnwear={vi.fn()} onEdit={vi.fn()} onDelete={vi.fn()} canEdit={false} />)
    expect(screen.getByText('14')).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('gives the delete button a clear accessible name', () => {
    renderTag()
    expect(screen.getByRole('button', { name: 'Delete Straight-leg 501s' })).toBeInTheDocument()
  })

  it('uses the photo with alt text when there is one', () => {
    renderTag({ ...jeans, photoUrl: '/api/items/7/photo' })
    expect(screen.getByRole('img', { name: 'Straight-leg 501s' })).toHaveAttribute('src', '/api/items/7/photo')
  })
})
