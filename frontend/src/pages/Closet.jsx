import { useCallback, useEffect, useState } from 'react'
import { deleteItem, listItems, unwearItem, wearItem } from '../api.js'
import HangTag from '../components/HangTag.jsx'
import ItemForm from '../components/ItemForm.jsx'
import Modal from '../components/Modal.jsx'
import { CATEGORIES, categoryLabel } from '../utils/format.js'

export default function Closet({ notify }) {
  const [items, setItems] = useState([])
  const [category, setCategory] = useState(null)
  const [status, setStatus] = useState('loading') // loading | ready | error
  const [error, setError] = useState('')
  const [editing, setEditing] = useState(null) // null = closed, {} = new item, item = editing

  const load = useCallback(async () => {
    try {
      setItems(await listItems(category))
      setStatus('ready')
    } catch (err) {
      setError(err.message)
      setStatus('error')
    }
  }, [category])

  useEffect(() => {
    load()
  }, [load])

  // Replace one item in the list without refetching everything.
  const replace = (updated) => setItems((list) => list.map((i) => (i.id === updated.id ? updated : i)))

  async function handleWear(item) {
    try {
      const updated = await wearItem(item.id)
      replace(updated)
      notify(`Added a wear to ${item.name} (${updated.wearCount} total)`)
    } catch (err) {
      notify(err.message)
    }
  }

  async function handleUnwear(item) {
    try {
      const updated = await unwearItem(item.id)
      replace(updated)
      notify(`Removed a wear from ${item.name} (${updated.wearCount} total)`)
    } catch (err) {
      notify(err.message)
    }
  }

  async function handleDelete(item) {
    if (!window.confirm(`Delete "${item.name}" from your closet? This can't be undone.`)) return
    try {
      await deleteItem(item.id)
      setItems((list) => list.filter((i) => i.id !== item.id))
      notify(`Deleted ${item.name}`)
    } catch (err) {
      notify(err.message)
    }
  }

  function handleSaved(saved, verb) {
    setEditing(null)
    notify(`${verb} ${saved.name}`)
    load()
  }

  return (
    <section aria-labelledby="closet-heading">
      <div className="page-head">
        <div>
          <h2 id="closet-heading">Your closet</h2>
          {status === 'ready' && (
            <p className="page-sub">
              {items.length} {items.length === 1 ? 'piece' : 'pieces'}
              {category && ` in ${categoryLabel(category).toLowerCase()}`}
            </p>
          )}
        </div>
        <button type="button" className="btn btn-primary" onClick={() => setEditing({})}>
          Add a piece
        </button>
      </div>

      <div className="chips" role="group" aria-label="Filter by category">
        <button type="button" className="chip" aria-pressed={category === null} onClick={() => setCategory(null)}>
          Everything
        </button>
        {CATEGORIES.map((c) => (
          <button key={c} type="button" className="chip" aria-pressed={category === c} onClick={() => setCategory(c)}>
            {categoryLabel(c)}
          </button>
        ))}
      </div>

      {status === 'loading' && <p className="quiet">Loading your closet…</p>}
      {status === 'error' && (
        <div className="empty" role="alert">
          <p>{error}</p>
          <button type="button" className="btn" onClick={load}>
            Try again
          </button>
        </div>
      )}

      {status === 'ready' && items.length === 0 && (
        <div className="empty">
          <p>
            {category
              ? `No ${categoryLabel(category).toLowerCase()} yet.`
              : 'Your closet is empty. Add the first thing you thrifted.'}
          </p>
          <button type="button" className="btn btn-primary" onClick={() => setEditing({})}>
            Add a piece
          </button>
        </div>
      )}

      {status === 'ready' && items.length > 0 && (
        <div className="rack">
          {items.map((item) => (
            <HangTag
              key={item.id}
              item={item}
              onWear={handleWear}
              onUnwear={handleUnwear}
              onEdit={setEditing}
              onDelete={handleDelete}
            />
          ))}
        </div>
      )}

      {editing && (
        <Modal title={editing.id ? `Edit ${editing.name}` : 'Add a piece'} onClose={() => setEditing(null)}>
          <ItemForm item={editing.id ? editing : null} onSaved={handleSaved} onCancel={() => setEditing(null)} />
        </Modal>
      )}
    </section>
  )
}
