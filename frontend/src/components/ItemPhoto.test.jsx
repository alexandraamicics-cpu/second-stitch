import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import ItemPhoto from './ItemPhoto.jsx'

const scarf = { id: 7, name: 'Printed silk scarf', category: 'ACCESSORY', colorHex: '#C9A13B' }

describe('ItemPhoto', () => {
  it('shows the photo when there is one', () => {
    render(<ItemPhoto item={{ ...scarf, photoUrl: '/api/items/7/photo?v=ab12' }} />)
    expect(screen.getByRole('img', { name: 'Printed silk scarf' })).toBeInTheDocument()
  })

  it('shows the outline instead of a broken image if the photo fails to load', () => {
    const { container } = render(<ItemPhoto item={{ ...scarf, photoUrl: '/api/items/7/photo?v=ab12' }} />)
    fireEvent.error(screen.getByRole('img', { name: 'Printed silk scarf' }))
    expect(screen.queryByRole('img', { name: 'Printed silk scarf' })).not.toBeInTheDocument()
    expect(container.querySelector('.silhouette')).toBeInTheDocument()
  })

  it('shows the outline when there is no photo', () => {
    const { container } = render(<ItemPhoto item={{ ...scarf, photoUrl: null }} />)
    expect(container.querySelector('.silhouette')).toBeInTheDocument()
  })
})
