import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.jsx'
import * as api from './api.js'

vi.mock('./api.js', () => ({
  apiUrl: (path) => path,
  getAuthStatus: vi.fn(),
  checkOwnerPassword: vi.fn(),
  hasOwnerPassword: vi.fn(() => false),
  setOwnerPassword: vi.fn(),
  clearOwnerPassword: vi.fn(),
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

describe('owner password in the app', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.hasOwnerPassword.mockReturnValue(false)
    api.listItems.mockResolvedValue([vest])
  })

  it('shows a view-only closet to visitors on a locked site', async () => {
    api.getAuthStatus.mockResolvedValue({ locked: true })
    render(<App />)

    expect(await screen.findByText("You're viewing this closet. Changes are turned off.")).toBeInTheDocument()
    await screen.findByRole('heading', { name: 'Crochet sweater vest' })
    expect(screen.queryByRole('button', { name: 'Add a piece' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Add a wear to Crochet sweater vest' })).not.toBeInTheDocument()
  })

  it('unlocks editing with the right password', async () => {
    api.getAuthStatus.mockResolvedValue({ locked: true })
    api.checkOwnerPassword.mockResolvedValue({ ok: true })
    render(<App />)

    await userEvent.click(await screen.findByRole('button', { name: 'Unlock editing' }))
    await userEvent.type(screen.getByLabelText('Owner password'), 'my-secret')
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Unlock editing' }))

    expect(api.checkOwnerPassword).toHaveBeenCalledWith('my-secret')
    expect(api.setOwnerPassword).toHaveBeenCalledWith('my-secret')
    expect(await screen.findByText('Editing unlocked on this device.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Add a piece' })).toBeInTheDocument()
  })

  it('shows the error for a wrong password and stays locked', async () => {
    api.getAuthStatus.mockResolvedValue({ locked: true })
    api.checkOwnerPassword.mockRejectedValue(new Error("That password isn't right"))
    render(<App />)

    await userEvent.click(await screen.findByRole('button', { name: 'Unlock editing' }))
    await userEvent.type(screen.getByLabelText('Owner password'), 'guess')
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Unlock editing' }))

    expect(await screen.findByText("That password isn't right")).toBeInTheDocument()
    expect(api.setOwnerPassword).not.toHaveBeenCalled()
  })

  it('has no bar and full editing when the server has no password (your laptop)', async () => {
    api.getAuthStatus.mockResolvedValue({ locked: false })
    render(<App />)

    expect(await screen.findByRole('button', { name: 'Add a piece' })).toBeInTheDocument()
    expect(screen.queryByText(/Changes are turned off/)).not.toBeInTheDocument()
  })
})
