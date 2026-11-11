import { useEffect, useState } from 'react'
import { getStats } from '../api.js'
import ItemPhoto from '../components/ItemPhoto.jsx'
import { categoryLabel, formatMoney, wearLabel } from '../utils/format.js'

export default function Stats() {
  const [stats, setStats] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getStats()
      .then(setStats)
      .catch((err) => setError(err.message))
  }, [])

  if (error) {
    return (
      <p className="empty" role="alert">
        {error}
      </p>
    )
  }
  if (!stats) return <p className="quiet">Adding it all up…</p>

  const maxCount = Math.max(1, ...stats.byCategory.map((c) => c.count))

  return (
    <section aria-labelledby="stats-heading">
      <div className="page-head">
        <div>
          <h2 id="stats-heading">Savings</h2>
          <p className="page-sub">What thrifting has saved you, and how hard your closet works.</p>
        </div>
      </div>

      <div className="stats">
        <div className="receipt">
          <h3>Closet receipt</h3>
          <dl>
            <div>
              <dt>Pieces</dt>
              <dd>{stats.totalItems}</dd>
            </div>
            <div>
              <dt>You paid</dt>
              <dd>{formatMoney(stats.totalPaid)}</dd>
            </div>
            <div>
              <dt>Price new</dt>
              <dd>{formatMoney(stats.totalRetailValue)}</dd>
            </div>
            <div>
              <dt>Times worn</dt>
              <dd>{stats.totalWears}</dd>
            </div>
            <div>
              <dt>Average cost per wear</dt>
              <dd>{formatMoney(stats.averageCostPerWear)}</dd>
            </div>
            <div className="receipt-total">
              <dt>Saved by thrifting</dt>
              <dd>{formatMoney(stats.totalSaved)}</dd>
            </div>
          </dl>
          <p className="receipt-foot">Every rewear keeps a piece out of landfill.</p>
        </div>

        <div className="stats-side">
          {stats.mostWorn && (
            <div className="panel">
              <h3>Most worn</h3>
              <div className="mini">
                <ItemPhoto item={stats.mostWorn} className="item-photo-small" />
                <div>
                  <p className="mini-name">{stats.mostWorn.name}</p>
                  <p className="quiet">
                    {wearLabel(stats.mostWorn.wearCount)}, now {formatMoney(stats.mostWorn.costPerWear)} per wear
                  </p>
                </div>
              </div>
            </div>
          )}

          {stats.waitingToBeWorn.length > 0 && (
            <div className="panel">
              <h3>Waiting for their first wear</h3>
              <ul className="mini-list">
                {stats.waitingToBeWorn.map((item) => (
                  <li key={item.id} className="mini">
                    <ItemPhoto item={item} className="item-photo-small" />
                    <p className="mini-name">{item.name}</p>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {stats.byCategory.length > 0 && (
            <div className="panel">
              <h3>What's in the closet</h3>
              <ul className="bars">
                {stats.byCategory.map((c) => (
                  <li key={c.category}>
                    <span className="bar-label">{categoryLabel(c.category)}</span>
                    <span className="bar-track">
                      <span className="bar-fill" style={{ width: `${(c.count / maxCount) * 100}%` }} />
                    </span>
                    <span className="bar-value">{c.count}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {stats.topStores.length > 0 && (
            <div className="panel">
              <h3>Best hunting grounds</h3>
              <ol className="stores">
                {stats.topStores.map((s) => (
                  <li key={s.store}>
                    {s.store} <span className="quiet">({s.count} finds)</span>
                  </li>
                ))}
              </ol>
            </div>
          )}
        </div>
      </div>
    </section>
  )
}
