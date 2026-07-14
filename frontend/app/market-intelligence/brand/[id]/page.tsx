'use client'
import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { Brand, Narrative, NewsCategories } from '../../lib/types'
import TopBar from '../../components/ui/TopBar'
import BrandSidebar from '../../components/detail/BrandSidebar'
import SetupTab from '../../components/detail/tabs/SetupTab'
import OwnComTab from '../../components/detail/tabs/OwnComTab'
import NewsTab from '../../components/detail/tabs/NewsTab'
import ThreePlDrawer from '../../components/detail/ThreePlDrawer'
import { ArrowLeft } from 'lucide-react'

const TABS = ['Own.com Deep Dive', 'Sales Narrative', 'Activity & News']

export default function BrandPage() {
  const { id } = useParams<{ id: string }>()
  const router = useRouter()
  const [brand, setBrand] = useState<Brand | null>(null)
  const [activeTab, setActiveTab] = useState(0)
  const [showDrawer, setShowDrawer] = useState(false)

  const [narrative, setNarrative] = useState<Narrative | null>(null)
  const [narrativeLoading, setNarrativeLoading] = useState(false)
  const [news, setNews] = useState<NewsCategories | null>(null)
  const [newsLoading, setNewsLoading] = useState(false)

  useEffect(() => {
    fetch('/api/market-intelligence/brands')
      .then(r => r.json())
      .then((brands: Brand[]) => {
        const found = brands.find(b => b.id === id)
        if (found) setBrand(found)
      })
  }, [id])

  useEffect(() => {
    if (!brand) return

    setNarrativeLoading(true)
    fetch('/api/market-intelligence/narrative', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(brand),
    })
      .then(async r => {
        const data = await r.json()
        if (!r.ok) throw new Error(data?.error ?? `HTTP ${r.status}`)
        return data as Narrative
      })
      .then(data => setNarrative(data))
      .catch(err => {
        console.error('Failed to load narrative:', err)
        setNarrative(null)
      })
      .finally(() => setNarrativeLoading(false))

    setNewsLoading(true)
    fetch('/api/market-intelligence/news', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(brand),
    })
      .then(async r => {
        const data = await r.json()
        if (!r.ok) throw new Error(data?.error ?? `HTTP ${r.status}`)
        return data as NewsCategories
      })
      .then(data => setNews(data))
      .catch(err => {
        console.error('Failed to load news:', err)
        setNews(null)
      })
      .finally(() => setNewsLoading(false))
  }, [brand?.id])

  if (!brand) {
    return (
      <div className="mi-screen" style={{ alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ color: 'var(--mi-muted)' }}>Loading…</div>
      </div>
    )
  }

  return (
    <div className="mi-screen">
      <TopBar />
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        <BrandSidebar brand={brand} onOpenDrawer={() => setShowDrawer(true)} />

        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 24px', background: 'var(--mi-surface)', borderBottom: '1px solid var(--mi-line)', flexShrink: 0 }}>
            <button className="mi-btn ghost" onClick={() => router.push('/market-intelligence')} style={{ padding: '6px 10px', gap: 6 }}>
              <ArrowLeft size={14} />
              Back
            </button>
          </div>

          <div className="mi-tab-bar">
            {TABS.map((tab, i) => (
              <div
                key={tab}
                className={`mi-tab-item ${activeTab === i ? 'active' : ''}`}
                onClick={() => setActiveTab(i)}
              >
                {tab}
              </div>
            ))}
          </div>

          <div style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column', position: 'relative' }}>
            <div style={{ display: activeTab === 0 ? 'flex' : 'none', flexDirection: 'column', flex: 1, overflow: 'hidden' }}>
              <OwnComTab brand={brand} />
            </div>
            <div style={{ display: activeTab === 1 ? 'flex' : 'none', flexDirection: 'column', flex: 1, overflow: 'hidden' }}>
              <SetupTab brand={brand} narrative={narrative} loading={narrativeLoading} />
            </div>
            <div style={{ display: activeTab === 2 ? 'flex' : 'none', flexDirection: 'column', flex: 1, overflow: 'hidden' }}>
              <NewsTab brand={brand} news={news} loading={newsLoading} />
            </div>
          </div>
        </div>
      </div>

      {showDrawer && <ThreePlDrawer brand={brand} onClose={() => setShowDrawer(false)} />}
    </div>
  )
}
