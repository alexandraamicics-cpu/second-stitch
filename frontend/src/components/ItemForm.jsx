import { useEffect, useState } from 'react'
import { createItem, updateItem, uploadPhoto } from '../api.js'
import { CATEGORIES, categorySingular } from '../utils/format.js'
import { shrinkImage } from '../utils/image.js'

const EMPTY = {
  name: '',
  category: 'TOP',
  brand: '',
  size: '',
  color: '',
  colorHex: '#c9cbc0',
  pricePaid: '',
  estimatedRetail: '',
  store: '',
  dateFound: '',
  notes: '',
}

// Turns an item from the API into form values (null → '' so inputs stay controlled).
function toForm(item) {
  if (!item) return EMPTY
  const form = { ...EMPTY }
  for (const key of Object.keys(EMPTY)) {
    if (item[key] !== null && item[key] !== undefined) form[key] = String(item[key])
  }
  form.colorHex = form.colorHex.toLowerCase() // colour inputs only accept lowercase hex
  return form
}

// Turns form values into what the API expects ('' → null, numbers as numbers).
function toRequest(form) {
  const blank = (v) => (v.trim() === '' ? null : v.trim())
  return {
    ...form,
    brand: blank(form.brand),
    size: blank(form.size),
    color: blank(form.color),
    store: blank(form.store),
    notes: blank(form.notes),
    dateFound: form.dateFound || null,
    pricePaid: form.pricePaid === '' ? null : Number(form.pricePaid),
    estimatedRetail: form.estimatedRetail === '' ? null : Number(form.estimatedRetail),
  }
}

export default function ItemForm({ item, onSaved, onCancel }) {
  const [form, setForm] = useState(() => toForm(item))
  const [photo, setPhoto] = useState(null)
  const [preview, setPreview] = useState(item?.photoUrl ?? null)
  const [errors, setErrors] = useState({})
  const [message, setMessage] = useState('')
  const [saving, setSaving] = useState(false)

  // Free the temporary preview URL when it changes or the form closes.
  useEffect(() => {
    if (!photo) return
    const url = URL.createObjectURL(photo)
    setPreview(url)
    return () => URL.revokeObjectURL(url)
  }, [photo])

  const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }))

  async function handleSubmit(e) {
    e.preventDefault()
    setSaving(true)
    setErrors({})
    setMessage('')
    try {
      let saved = item ? await updateItem(item.id, toRequest(form)) : await createItem(toRequest(form))
      // Shrink big phone photos before uploading (see utils/image.js)
      if (photo) saved = await uploadPhoto(saved.id, await shrinkImage(photo))
      onSaved(saved, item ? 'Saved changes' : 'Added')
    } catch (err) {
      setErrors(err.fields ?? {})
      setMessage(err.message)
    } finally {
      setSaving(false)
    }
  }

  const field = (name, label, input) => (
    <div className={`field field-${name}`}>
      <label htmlFor={`f-${name}`}>{label}</label>
      {input}
      {errors[name] && (
        <p className="field-error" id={`f-${name}-error`}>
          {errors[name]}
        </p>
      )}
    </div>
  )

  const describedBy = (name) => (errors[name] ? `f-${name}-error` : undefined)

  return (
    <form className="item-form" onSubmit={handleSubmit} noValidate>
      {field(
        'name',
        'Name',
        <input
          id="f-name"
          value={form.name}
          onChange={set('name')}
          placeholder="Corduroy chore jacket"
          aria-invalid={!!errors.name}
          aria-describedby={describedBy('name')}
          required
        />,
      )}

      <div className="field-row">
        {field(
          'category',
          'Category',
          <select id="f-category" value={form.category} onChange={set('category')}>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {categorySingular(c)}
              </option>
            ))}
          </select>,
        )}
        {field('size', 'Size', <input id="f-size" value={form.size} onChange={set('size')} placeholder="M" />)}
      </div>

      <div className="field-row">
        {field('brand', 'Brand', <input id="f-brand" value={form.brand} onChange={set('brand')} />)}
        {field(
          'store',
          'Where you found it',
          <input id="f-store" value={form.store} onChange={set('store')} placeholder="Ukay-ukay, Cubao" />,
        )}
      </div>

      <div className="field-row">
        {field(
          'pricePaid',
          'Price paid (₱)',
          <input
            id="f-pricePaid"
            type="number"
            min="0"
            step="1"
            inputMode="decimal"
            value={form.pricePaid}
            onChange={set('pricePaid')}
            aria-invalid={!!errors.pricePaid}
            aria-describedby={describedBy('pricePaid')}
            required
          />,
        )}
        {field(
          'estimatedRetail',
          'Price new, roughly (₱)',
          <input
            id="f-estimatedRetail"
            type="number"
            min="0"
            step="1"
            inputMode="decimal"
            value={form.estimatedRetail}
            onChange={set('estimatedRetail')}
          />,
        )}
      </div>

      <div className="field-row">
        {field(
          'color',
          'Colour',
          <input id="f-color" value={form.color} onChange={set('color')} placeholder="sage" />,
        )}
        {field(
          'colorHex',
          'Swatch',
          <input id="f-colorHex" type="color" value={form.colorHex} onChange={set('colorHex')} />,
        )}
        {field(
          'dateFound',
          'Date found',
          <input id="f-dateFound" type="date" value={form.dateFound} onChange={set('dateFound')} />,
        )}
      </div>

      {field(
        'notes',
        'Notes',
        <textarea
          id="f-notes"
          rows="2"
          value={form.notes}
          onChange={set('notes')}
          placeholder="Fit, fabric, how it feels to wear..."
        />,
      )}

      <div className="field field-photo">
        <label htmlFor="f-photo">Photo</label>
        <div className="photo-picker">
          {preview && <img src={preview} alt="" className="photo-preview" />}
          <input
            id="f-photo"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            onChange={(e) => setPhoto(e.target.files?.[0] ?? null)}
          />
        </div>
        <p className="field-hint">Square, flat-lay photos on a plain background look best. Big photos are shrunk automatically.</p>
      </div>

      {message && (
        <p className="form-message" role="alert">
          {message}
        </p>
      )}

      <div className="form-actions">
        <button type="button" className="btn" onClick={onCancel}>
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={saving}>
          {saving ? 'Saving…' : item ? 'Save changes' : 'Add to closet'}
        </button>
      </div>
    </form>
  )
}
