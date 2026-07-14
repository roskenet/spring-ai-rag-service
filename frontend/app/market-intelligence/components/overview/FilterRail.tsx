'use client'
import { Brand } from '../../lib/types'

type Filters = {
  rating: string
  logistics: string
  priority: string
  segment: string
}

type Props = {
  brands: Brand[]
  filters: Filters
  onChange: (f: Filters) => void
}

export default function FilterRail({ brands, filters, onChange }: Props) {
  const set = (key: keyof Filters, val: string) =>
    onChange({ ...filters, [key]: val })

  const ratings = ['all', 'High', 'Medium', 'Low']
  const logistics = ['all', 'In-house', '3PL', 'Hybrid']
  const priorities = ['all', 'Must win', 'Prio 1', 'Prio 2', 'Prio 3']
  const segments = ['All segments', ...Array.from(new Set(brands.map(b => b.segment))).filter(Boolean)]

  const countFor = (key: keyof Filters, val: string) => {
    if (val === 'all' || val === 'All segments') return brands.length
    return brands.filter(b => {
      if (key === 'rating') return b.rating === val
      if (key === 'logistics') return b.logistics === val
      if (key === 'priority') return b.prioritySegment === val
      if (key === 'segment') return b.segment === val
      return true
    }).length
  }

  return (
    <aside style={{
      background: 'var(--mi-surface)',
      borderRight: '1px solid var(--mi-line)',
      padding: '20px 0',
      overflow: 'auto',
      flexShrink: 0,
      width: 240,
    }}>
      <div style={{ padding: '20px 20px 0' }}>
        <div className="mi-filter-section-title">Rating</div>
        <div className="mi-col" style={{ gap: 2 }}>
          {ratings.map(r => (
            <div
              key={r}
              className={`mi-filter-option ${filters.rating === r ? 'active' : ''}`}
              onClick={() => set('rating', r)}
            >
              <span>{r === 'all' ? 'All ratings' : r}</span>
              <span className="mi-filter-count">{countFor('rating', r)}</span>
            </div>
          ))}
        </div>
      </div>

      <div style={{ padding: '20px 20px 0' }}>
        <div className="mi-filter-section-title">Logistics</div>
        <div className="mi-col" style={{ gap: 2 }}>
          {logistics.map(l => (
            <div
              key={l}
              className={`mi-filter-option ${filters.logistics === l ? 'active' : ''}`}
              onClick={() => set('logistics', l)}
            >
              <span>{l === 'all' ? 'All logistics' : l}</span>
              <span className="mi-filter-count">{countFor('logistics', l)}</span>
            </div>
          ))}
        </div>
      </div>

      <div style={{ padding: '20px 20px 0' }}>
        <div className="mi-filter-section-title">Priority</div>
        <div className="mi-col" style={{ gap: 2 }}>
          {priorities.map(p => (
            <div
              key={p}
              className={`mi-filter-option ${filters.priority === p ? 'active' : ''}`}
              onClick={() => set('priority', p)}
            >
              <span>{p === 'all' ? 'All priorities' : p}</span>
              <span className="mi-filter-count">{countFor('priority', p)}</span>
            </div>
          ))}
        </div>
      </div>

      <div style={{ padding: '20px 20px 0' }}>
        <div className="mi-filter-section-title">Segment</div>
        <div className="mi-col" style={{ gap: 2 }}>
          {segments.map(s => (
            <div
              key={s}
              className={`mi-filter-option ${filters.segment === s ? 'active' : ''}`}
              onClick={() => set('segment', s)}
            >
              <span>{s}</span>
              <span className="mi-filter-count">{countFor('segment', s)}</span>
            </div>
          ))}
        </div>
      </div>
    </aside>
  )
}
