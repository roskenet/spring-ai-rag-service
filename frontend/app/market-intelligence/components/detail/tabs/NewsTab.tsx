'use client'
import { Brand, NewsCategories, NewsItem } from '../../../lib/types'

type Props = {
  brand: Brand
  news: NewsCategories | null
  loading: boolean
}

const CATEGORIES: { key: keyof NewsCategories; label: string }[] = [
  { key: 'pressReleases', label: 'Press Releases' },
  { key: 'mandA', label: 'M&A' },
  { key: 'ecommerceLogistics', label: 'Ecommerce Logistics' },
  { key: 'financialReports', label: 'Financial Reports' },
  { key: 'ecommerceNews', label: 'Ecommerce News' },
  { key: 'managementChanges', label: 'Management Changes' },
]

export default function NewsTab({ brand, news, loading }: Props) {
  if (loading) {
    return (
      <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--mi-muted)', fontSize: 13 }}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ animation: 'spin 1s linear infinite' }}>
            <path d="M21 12a9 9 0 1 1-3-6.7L21 8"/><path d="M21 3v5h-5"/>
          </svg>
          Generating market intelligence via Bedrock…
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginTop: 8 }}>
          {[...Array(6)].map((_, i) => (
            <div key={i} className="mi-card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              <div style={{ height: 10, borderRadius: 5, background: 'var(--mi-surface-2)', width: '40%' }} />
              <div style={{ height: 10, borderRadius: 5, background: 'var(--mi-surface-2)', width: '90%', opacity: 0.6 }} />
              <div style={{ height: 10, borderRadius: 5, background: 'var(--mi-surface-2)', width: '70%', opacity: 0.4 }} />
            </div>
          ))}
        </div>
        <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
      </div>
    )
  }

  if (!news) return null

  return (
    <div style={{ padding: 24, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, overflow: 'auto', flex: 1, alignContent: 'start' }}>
      {CATEGORIES.map(({ key, label }) => (
        <div key={key} className="mi-card">
          <div className="mi-row" style={{ justifyContent: 'space-between', marginBottom: 12 }}>
            <div className="mi-card-h" style={{ margin: 0 }}>{label}</div>
            <span style={{ fontSize: 11, color: 'var(--mi-muted)', background: 'var(--mi-surface-2)', padding: '2px 8px', borderRadius: 6 }}>
              Claude · Bedrock
            </span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {(news[key] as NewsItem[]).map((item, i) => (
              <NewsItemCard key={i} item={item} />
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}

function NewsItemCard({ item }: { item: NewsItem }) {
  const hasSource = item.source && item.source.trim() !== '' && item.source !== 'placeholder'
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
      <p style={{ margin: 0, fontSize: 13, lineHeight: 1.5, color: 'var(--mi-ink-2)' }}>{item.text}</p>
      <div className="mi-row" style={{ gap: 8 }}>
        <span style={{ fontSize: 11, color: 'var(--mi-muted)', fontFamily: 'var(--mi-font-mono)' }}>{item.date || 'Date unknown'}</span>
        {hasSource && <span style={{ fontSize: 11, color: 'var(--mi-accent-ink)' }}>{item.source}</span>}
      </div>
    </div>
  )
}
