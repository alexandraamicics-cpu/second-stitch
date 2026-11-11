import { useCallback, useEffect, useState } from 'react'
import { deleteItem, listItems, unwearItem, wearItem } from '../api.js'
import HangTag from '../components/HangTag.jsx'
import ItemForm from '../components/ItemForm.jsx'
import Modal from '../components/Modal.jsx'
import { CATEGORIES, categoryLabel, formatMoney } from '../utils/format.js'

export default function Closet({ notify, canEdit = true }) {
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

  // Totals for the summary tiles (whatever is in view, so they follow the filter)
  const totals = items.reduce(
    (sum, item) => ({
      paid: sum.paid + (Number(item.pricePaid) || 0),
      retail: sum.retail + (Number(item.estimatedRetail) || 0),
      saved: sum.saved + (Number(item.savings) || 0),
      wears: sum.wears + (Number(item.wearCount) || 0),
    }),
    { paid: 0, retail: 0, saved: 0, wears: 0 },
  )

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
        {canEdit && (
          <button type="button" className="btn btn-primary" onClick={() => setEditing({})}>
            Add a piece
          </button>
        )}
      </div>

      {status === 'ready' && items.length > 0 && (
        <dl className="kpis">
          <div className="kpi">
            <dt>You paid</dt>
            <dd>{formatMoney(totals.paid)}</dd>
          </div>
          <div className="kpi">
            <dt>Price new</dt>
            <dd>{formatMoney(totals.retail)}</dd>
          </div>
          <div className="kpi">
            <dt>Saved vs. buying new</dt>
            <dd className="positive">{formatMoney(totals.saved)}</dd>
          </div>
          <div className="kpi">
            <dt>Times worn</dt>
            <dd>{totals.wears}</dd>
          </div>
        </dl>
      )}

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
              : canEdit
                ? 'Your closet is empty. Add the first thing you thrifted.'
                : 'This closet is empty.'}
          </p>
          {canEdit && (
            <button type="button" className="btn btn-primary" onClick={() => setEditing({})}>
              Add a piece
            </button>
          )}
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
              canEdit={canEdit}
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
