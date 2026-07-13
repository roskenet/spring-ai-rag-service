'use client'
import { useEffect, useState } from 'react'
import { Brand } from './lib/types'
import TopBar from './components/ui/TopBar'
import FilterRail from './components/overview/FilterRail'
import BrandTable from './components/overview/BrandTable'

type Filters = { rating: string; logistics: string; priority: string; segment: string }

const DEFAULT_FILTERS: Filters = { rating: 'all', logistics: 'all', priority: 'all', segment: 'All segments' }

export default function MarketIntelligencePage() {
  const [brands, setBrands] = useState<Brand[]>([])
  const [search, setSearch] = useState('')
  const [filters, setFilters] = useState<Filters>(DEFAULT_FILTERS)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetch('/api/market-intelligence/brands')
      .then(r => r.json())
      .then(data => { setBrands(data); setLoading(false) })
      .catch(() => setLoading(false))
  }, [])

  const filtered = brands
    .filter(b => filters.rating === 'all' || b.rating === filters.rating)
    .filter(b => filters.logistics === 'all' || b.logistics === filters.logistics)
    .filter(b => filters.priority === 'all' || b.prioritySegment === filters.priority)
    .filter(b => filters.segment === 'All segments' || b.segment === filters.segment)
    .filter(b => !search || b.name.toLowerCase().includes(search.toLowerCase()) || b.parentCompany.toLowerCase().includes(search.toLowerCase()))

  return (
    <div className="mi-screen">
      <TopBar search={search} onSearch={setSearch} />
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        <FilterRail brands={brands} filters={filters} onChange={setFilters} />
        <div style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          {loading ? (
            <div style={{ flex: 1, display: 'grid', placeItems: 'center', color: 'var(--mi-muted)' }}>
              Loading brands…
            </div>
          ) : (
            <BrandTable brands={filtered} />
          )}
          <div className="mi-status-bar">
            {filtered.length} of {brands.length} brands · Market Intelligence Portal
          </div>
        </div>
      </div>
    </div>
  )
}
