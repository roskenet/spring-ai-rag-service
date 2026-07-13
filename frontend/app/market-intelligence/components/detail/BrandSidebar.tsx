import { Brand } from '../../lib/types'
import { brandColor, formatCurrency, formatNumber, initials } from '../../lib/utils'
import { RatingBadge, PriorityBadge } from '../ui/Badges'

type Props = { brand: Brand; onOpenDrawer: () => void }

export default function BrandSidebar({ brand, onOpenDrawer }: Props) {
  const color = brandColor(brand.name)

  return (
    <aside className="mi-brand-sidebar">
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <div
          className="mi-mono xl"
          style={{ background: color, width: 64, height: 64, borderRadius: 16, fontSize: 24 }}
        >
          {initials(brand.name)}
        </div>
        <div>
          <div style={{ fontSize: 20, fontWeight: 700, letterSpacing: '-0.02em', lineHeight: 1.2 }}>
            {brand.name}
          </div>
          <div style={{ fontSize: 13, color: 'var(--mi-muted)', marginTop: 4 }}>{brand.parentCompany}</div>
        </div>
        <div className="mi-row" style={{ gap: 6, flexWrap: 'wrap' }}>
          <RatingBadge rating={brand.rating} />
          <PriorityBadge priority={brand.prioritySegment} />
        </div>
      </div>

      <hr className="mi-hr" />

      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <KpiRow label="Global Web Traffic" value={formatNumber(brand.globalWebTraffic, true)} />
        <KpiRow label="Online Revenue" value={formatCurrency(brand.onlineRevenue, true)} />
        <KpiRow label="EU Web Traffic" value={formatNumber(brand.euWebTraffic, true)} />
        <KpiRow label="Own.com GMV" value={formatCurrency(brand.estimatedOwnEcomGmv, true)} />
        <KpiRow label="Own.com Shipped Items" value={formatNumber(brand.estimatedOwnEcomShippedItems, true)} />
      </div>

      <hr className="mi-hr" />

      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <FactRow label="HQ" value={brand.hq} />
        <FactRow label="Warehouse" value={brand.warehouseLocation || '—'} />
        <FactRow label="Logistics" value={brand.logistics + (brand.threePl ? ` · ${brand.threePl}` : '')} />
        {brand.threePl && (
          <button className="mi-btn primary" onClick={onOpenDrawer} style={{ marginTop: 4, width: '100%', justifyContent: 'center' }}>
            Deep dive · {brand.threePl}
          </button>
        )}
        <FactRow label="Segment" value={brand.segment} />
        <FactRow label="Status" value={brand.existingOrPotential} />
      </div>
    </aside>
  )
}

function KpiRow({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div style={{ fontSize: 11, color: 'var(--mi-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 2 }}>{label}</div>
      <div className="mi-tnum" style={{ fontFamily: 'var(--mi-font-mono)', fontSize: 15, fontWeight: 500 }}>{value}</div>
    </div>
  )
}

function FactRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="mi-row" style={{ justifyContent: 'space-between', fontSize: 13 }}>
      <span style={{ color: 'var(--mi-muted)' }}>{label}</span>
      <span style={{ fontWeight: 500, textAlign: 'right', maxWidth: 160 }}>{value}</span>
    </div>
  )
}
