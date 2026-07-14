import { ratingClass } from '../../lib/utils'

export function RatingBadge({ rating }: { rating: string }) {
  const cls = ratingClass(rating)
  return (
    <span className={`mi-pill ${cls}`}>
      <span className="mi-dot" />
      {rating}
    </span>
  )
}

export function PriorityBadge({ priority }: { priority: string }) {
  const color =
    priority.toLowerCase().includes('must') ? 'var(--mi-bad)' :
    priority === 'Prio 1' ? 'var(--mi-warn)' : 'var(--mi-muted-2)'
  return (
    <span className="mi-cell-pill">
      <span className="mi-dot" style={{ background: color }} />
      {priority}
    </span>
  )
}

export function LogisticsBadge({ logistics, threePl }: { logistics: string; threePl?: string | null }) {
  const label = logistics === '3PL' && threePl ? `3PL · ${threePl}` : logistics
  return <span className="mi-cell-pill">{label}</span>
}
