import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Closet from './Closet.jsx'
import * as api from '../api.js'

// Replace the real API with fakes, so these tests don't need the backend running.
vi.mock('../api.js', () => ({
  apiUrl: (path) => path,
  listItems: vi.fn(),
  wearItem: vi.fn(),
  unwearItem: vi.fn(),
  deleteItem: vi.fn(),
  createItem: vi.fn(),
  updateItem: vi.fn(),
  uploadPhoto: vi.fn(),
}))

const vest = {
  id: 1,
  name: 'Crochet sweater vest',
  category: 'TOP',
  pricePaid: 120,
  savings: 0,
  costPerWear: 40,
  wearCount: 3,
  colorHex: '#D8CBB0',
}

describe('Closet page', () => {
  beforeEach(() => vi.clearAllMocks())

  it('lists the pieces from the API', async () => {
    api.listItems.mockResolvedValue([vest])
    render(<Closet notify={vi.fn()} />)
    expect(await screen.findByRole('heading', { name: 'Crochet sweater vest' })).toBeInTheDocument()
    expect(screen.getByText('1 piece')).toBeInTheDocument()
  })

  it('shows a helpful empty state', async () => {
    api.listItems.mockResolvedValue([])
    render(<Closet notify={vi.fn()} />)
    expect(await screen.findByText(/Your closet is empty/)).toBeInTheDocument()
  })

  it('filters by category', async () => {
    api.listItems.mockResolvedValue([vest])
    render(<Closet notify={vi.fn()} />)
    await screen.findByText('1 piece')
    await userEvent.click(screen.getByRole('button', { name: 'Tops' }))
    expect(api.listItems).toHaveBeenLastCalledWith('TOP')
  })

  it('adds a wear with + and tells the user', async () => {
    api.listItems.mockResolvedValue([vest])
    api.wearItem.mockResolvedValue({ ...vest, wearCount: 4, costPerWear: 30 })
    const notify = vi.fn()
    render(<Closet notify={notify} />)

    await userEvent.click(await screen.findByRole('button', { name: 'Add a wear to Crochet sweater vest' }))

    expect(api.wearItem).toHaveBeenCalledWith(1)
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('4'))
    expect(notify).toHaveBeenCalledWith('Added a wear to Crochet sweater vest (4 total)')
  })

  it('removes a wear with − to undo a mis-click', async () => {
    api.listItems.mockResolvedValue([vest])
    api.unwearItem.mockResolvedValue({ ...vest, wearCount: 2, costPerWear: 60 })
    const notify = vi.fn()
    render(<Closet notify={notify} />)

    await userEvent.click(await screen.findByRole('button', { name: 'Remove a wear from Crochet sweater vest' }))

    expect(api.unwearItem).toHaveBeenCalledWith(1)
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('2'))
    expect(notify).toHaveBeenCalledWith('Removed a wear from Crochet sweater vest (2 total)')
  })

  it('shows the error and a retry button when the API is down', async () => {
    api.listItems.mockRejectedValue(new Error("Can't reach the server."))
    render(<Closet notify={vi.fn()} />)
    expect(await screen.findByRole('alert')).toHaveTextContent("Can't reach the server.")
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
  })
})
