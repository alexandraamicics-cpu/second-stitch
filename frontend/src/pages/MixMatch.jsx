import { useEffect, useMemo, useState } from 'react'
import { createOutfit, deleteOutfit, listItems, listOutfits, unwearOutfit, wearOutfit } from '../api.js'
import ItemPhoto from '../components/ItemPhoto.jsx'
import WearStepper from '../components/WearStepper.jsx'
import { categoryLabel, categorySingular, formatMoney } from '../utils/format.js'
import {
  LAYER_ORDER,
  addToLook,
  fillGaps,
  groupByCategory,
  isInLook,
  lookFromOutfit,
  lookTotals,
  removeFromLook,
} from '../utils/mixer.js'

const pieces = (n) => `${n} ${n === 1 ? 'piece' : 'pieces'}`

export default function MixMatch({ notify }) {
  const [items, setItems] = useState(null)
  const [outfits, setOutfits] = useState([])
  const [look, setLook] = useState([])
  const [filter, setFilter] = useState(null) // null = every category
  const [name, setName] = useState('')
  const [occasion, setOccasion] = useState('')
  const [errors, setErrors] = useState({})
  const [loadError, setLoadError] = useState('')

  const racks = useMemo(() => groupByCategory(items ?? []), [items])
  const categories = LAYER_ORDER.filter((c) => racks[c].length > 0)
  const totals = lookTotals(look)

  useEffect(() => {
    Promise.all([listItems(), listOutfits()])
      .then(([loadedItems, loadedOutfits]) => {
        setItems(loadedItems)
        setOutfits(loadedOutfits)
      })
      .catch((err) => setLoadError(err.message))
  }, [])

  // Click a piece on the rack: add it, or take it off if it's already on.
  function toggle(item) {
    if (isInLook(look, item)) {
      setLook(removeFromLook(look, item))
      notify(`Took off ${item.name}`)
      return
    }
    const { look: next, removed } = addToLook(look, item)
    setLook(next)
    notify(
      removed.length > 0
        ? `Swapped ${removed.map((piece) => piece.name).join(' and ')} for ${item.name}`
        : `Added ${item.name}`,
    )
  }

  function handleFill() {
    const next = fillGaps(look, racks)
    const added = next.filter((piece) => !isInLook(look, piece))
    if (added.length === 0) {
      notify('Nothing to add: your look already has a base and shoes')
      return
    }
    setLook(next)
    notify(`Added ${added.map((piece) => piece.name).join(', ')}`)
  }

  function mixAgain(outfit) {
    setLook(lookFromOutfit(outfit, items))
    window.scrollTo?.({ top: 0 })
    notify(`Loaded ${outfit.name} into your look`)
  }

  async function handleSave(e) {
    e.preventDefault()
    setErrors({})
    try {
      const saved = await createOutfit({ name, occasion, itemIds: look.map((piece) => piece.id) })
      setOutfits((list) => [saved, ...list])
      setName('')
      setOccasion('')
      notify(`Saved outfit ${saved.name}`)
    } catch (err) {
      setErrors({ ...err.fields, form: err.message })
    }
  }

  // Adds (+1) or removes (-1) a wear for the outfit and every piece in it.
  async function changeWear(outfit, dir) {
    try {
      const updated = dir > 0 ? await wearOutfit(outfit.id) : await unwearOutfit(outfit.id)
      setOutfits((list) => list.map((o) => (o.id === updated.id ? updated : o)))
      // Every piece's wear count changed, so reload them (and the look's copies)
      const fresh = await listItems()
      setItems(fresh)
      setLook((current) => lookFromOutfit({ items: current }, fresh))
      notify(
        dir > 0
          ? `Added a wear to ${outfit.name} and every piece in it`
          : `Removed a wear from ${outfit.name} and every piece in it`,
      )
    } catch (err) {
      notify(err.message)
    }
  }

  async function handleDelete(outfit) {
    if (!window.confirm(`Delete the outfit "${outfit.name}"? The pieces stay in your closet.`)) return
    try {
      await deleteOutfit(outfit.id)
      setOutfits((list) => list.filter((o) => o.id !== outfit.id))
      notify(`Deleted ${outfit.name}`)
    } catch (err) {
      notify(err.message)
    }
  }

  if (loadError) {
    return (
      <p className="empty" role="alert">
        {loadError}
      </p>
    )
  }
  if (!items) return <p className="quiet">Opening the fitting room…</p>

  if (items.length === 0) {
    return (
      <div className="empty">
        <p>Add a few pieces on the Closet tab, then come back to mix and match them.</p>
      </div>
    )
  }

  return (
    <section aria-labelledby="mix-heading">
      <div className="page-head">
        <div>
          <h2 id="mix-heading">Mix &amp; match</h2>
          <p className="page-sub">Click pieces on the rack to try them on. Click again to take them off.</p>
        </div>
        <button type="button" className="btn" onClick={handleFill}>
          Fill the gaps for me
        </button>
      </div>

      <div className="mixer">
        <div className="rack-panel">
          <div className="chips" role="group" aria-label="Show on the rack">
            <button type="button" className="chip" aria-pressed={filter === null} onClick={() => setFilter(null)}>
              Everything
            </button>
            {categories.map((c) => (
              <button key={c} type="button" className="chip" aria-pressed={filter === c} onClick={() => setFilter(c)}>
                {categoryLabel(c)}
              </button>
            ))}
          </div>

          {categories
            .filter((c) => filter === null || filter === c)
            .map((c) => (
              <section key={c} className="rack-group" aria-labelledby={`rack-${c}`}>
                <h3 id={`rack-${c}`}>{categoryLabel(c)}</h3>
                <div className="pick-grid">
                  {racks[c].map((item) => {
                    const on = isInLook(look, item)
                    return (
                      <button
                        key={item.id}
                        type="button"
                        className="pick"
                        aria-pressed={on}
                        onClick={() => toggle(item)}
                      >
                        <ItemPhoto item={item} className="item-photo-small" />
                        <span className="pick-name">{item.name}</span>
                        <span className="pick-price">{formatMoney(item.pricePaid)}</span>
                        <span className="pick-state" aria-hidden="true">
                          {on ? 'In your look' : 'Add'}
                        </span>
                      </button>
                    )
                  })}
                </div>
              </section>
            ))}
        </div>

        <form id="look" className="look-receipt" onSubmit={handleSave} aria-labelledby="look-heading">
          <div className="look-head">
            <h3 id="look-heading">This look</h3>
            {look.length > 0 && (
              <button type="button" className="btn btn-small btn-quiet" onClick={() => setLook([])}>
                Clear
              </button>
            )}
          </div>

          {look.length === 0 ? (
            <p className="quiet look-empty">
              Nothing on yet. Click pieces on the rack, or use Fill the gaps for me.
            </p>
          ) : (
            <ol className="look-stack">
              {look.map((item) => (
                <li key={item.id}>
                  <ItemPhoto item={item} className="item-photo-small" />
                  <div className="look-piece">
                    <span className="look-cat">{categorySingular(item.category)}</span>
                    <span className="look-name">{item.name}</span>
                    <span className="look-meta">
                      {item.estimatedRetail ? `${formatMoney(item.estimatedRetail)} new, ` : ''}
                      {formatMoney(item.costPerWear)} per wear
                    </span>
                  </div>
                  <span className="look-price">{formatMoney(item.pricePaid)}</span>
                  <button
                    type="button"
                    className="remove"
                    aria-label={`Take off ${item.name}`}
                    onClick={() => toggle(item)}
                  >
                    ×
                  </button>
                </li>
              ))}
            </ol>
          )}

          <dl className="look-totals">
            <div>
              <dt>You paid</dt>
              <dd>{formatMoney(totals.paid)}</dd>
            </div>
            <div>
              <dt>Price new</dt>
              <dd>{totals.retail ? formatMoney(totals.retail) : '—'}</dd>
            </div>
            <div>
              <dt>Cost per wear so far</dt>
              <dd>{formatMoney(totals.costPerWear)}</dd>
            </div>
            <div className="look-saved">
              <dt>Saved by thrifting</dt>
              <dd>{formatMoney(totals.saved)}</dd>
            </div>
          </dl>

          {errors.itemIds && <p className="field-error">{errors.itemIds}</p>}

          <div className="field">
            <label htmlFor="o-name">Outfit name</label>
            <input
              id="o-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Rainy commute"
              aria-invalid={!!errors.name}
            />
            {errors.name && <p className="field-error">{errors.name}</p>}
          </div>
          <div className="field">
            <label htmlFor="o-occasion">Occasion (optional)</label>
            <input
              id="o-occasion"
              value={occasion}
              onChange={(e) => setOccasion(e.target.value)}
              placeholder="soft fabrics day"
            />
          </div>
          {errors.form && !errors.name && !errors.itemIds && (
            <p className="form-message" role="alert">
              {errors.form}
            </p>
          )}
          <button type="submit" className="btn btn-primary">
            Save outfit
          </button>
        </form>
      </div>

      {/* Phones only: the look sits below the rack, so keep a running total in view */}
      {look.length > 0 && (
        <a className="look-bar" href="#look">
          <span>
            {pieces(look.length)}, {formatMoney(totals.paid)}
          </span>
          <span className="look-bar-link">See look</span>
        </a>
      )}

      <h3 className="section-title">Saved outfits</h3>
      {outfits.length === 0 && <p className="quiet">No saved outfits yet.</p>}
      <div className="outfit-list">
        {outfits.map((outfit) => (
          <article key={outfit.id} className="outfit">
            <div className="outfit-strip">
              {outfit.items.map((item) => (
                <ItemPhoto key={item.id} item={item} className="item-photo-small" />
              ))}
            </div>
            <div className="outfit-body">
              <h4>{outfit.name}</h4>
              <p className="quiet">
                {[outfit.occasion, pieces(outfit.items.length)].filter(Boolean).join(', ')}
              </p>
              <p className="outfit-price">
                {formatMoney(outfit.totalPaid)} paid
                {Number(outfit.totalSaved) > 0 && <span>, saved {formatMoney(outfit.totalSaved)}</span>}
              </p>
              <WearStepper
                count={outfit.wearCount}
                name={outfit.name}
                onAdd={() => changeWear(outfit, 1)}
                onRemove={() => changeWear(outfit, -1)}
              />
              <div className="tag-actions">
                <button type="button" className="btn btn-small" onClick={() => mixAgain(outfit)}>
                  Mix again
                </button>
                <button
                  type="button"
                  className="btn btn-small btn-quiet"
                  onClick={() => handleDelete(outfit)}
                  aria-label={`Delete outfit ${outfit.name}`}
                >
                  Delete
                </button>
              </div>
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}
