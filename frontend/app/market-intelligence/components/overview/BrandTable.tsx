'use client'
import { useRouter } from 'next/navigation'
import { Brand } from '../../lib/types'
import { brandColor, formatCurrency, initials } from '../../lib/utils'
import { RatingBadge, PriorityBadge, LogisticsBadge } from '../ui/Badges'
import StackedBar from './StackedBar'

type Props = {
  brands: Brand[]
}

export default function BrandTable({ brands }: Props) {
  const router = useRouter()

  return (
    <div style={{ flex: 1, overflow: 'auto' }}>
      <table className="mi-table">
        <thead>
          <tr>
            <th style={{ width: 180, textAlign: 'left' }}>Brand</th>
            <th style={{ width: 100, textAlign: 'center' }}>Parent</th>
            <th style={{ width: 110, textAlign: 'center' }}>Existing vs. Potential</th>
            <th style={{ width: 90, textAlign: 'center' }}>Priority</th>
            <th style={{ width: 80, textAlign: 'center' }}>Segment</th>
            <th style={{ width: 100, textAlign: 'center' }}>Revenue</th>
            <th style={{ textAlign: 'center' }}>Volume Mix (Item share)</th>
            <th style={{ width: 110, textAlign: 'center' }}>Logistics</th>
            <th style={{ width: 80, textAlign: 'center' }}>Incremental Volume Rating</th>
          </tr>
        </thead>
        <tbody>
          {brands.length === 0 && (
            <tr>
              <td colSpan={9} style={{ textAlign: 'center', color: 'var(--mi-muted)', padding: 40 }}>
                No brands match the current filters
              </td>
            </tr>
          )}
          {brands.map(brand => (
            <tr key={brand.id} onClick={() => router.push(`/market-intelligence/brand/${brand.id}`)}>
              <td style={{ textAlign: 'left' }}>
                <div className="mi-row" style={{ gap: 10 }}>
                  <div className="mi-mono" style={{ background: brandColor(brand.name) }}>
                    {initials(brand.name)}
                  </div>
                  <div>
                    <div style={{ fontWeight: 500, fontSize: 13.5 }}>{brand.name}</div>
                    <div style={{ fontSize: 12, color: 'var(--mi-muted)' }}>{brand.hq}</div>
                  </div>
                </div>
              </td>
              <td style={{ color: 'var(--mi-muted)', fontSize: 12, maxWidth: 100, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', textAlign: 'center' }} title={brand.parentCompany}>
                {brand.parentCompany}
              </td>
              <td style={{ fontSize: 12, textAlign: 'center', color: 'var(--mi-muted)' }}>{brand.existingOrPotential}</td>
              <td style={{ textAlign: 'center' }}><PriorityBadge priority={brand.prioritySegment} /></td>
              <td style={{ fontSize: 12, textAlign: 'center' }}>{brand.segment}</td>
              <td style={{ textAlign: 'center' }}>
                <span className="mi-tnum" style={{ fontFamily: 'var(--mi-font-mono)', fontSize: 12 }}>
                  {formatCurrency(brand.onlineRevenue, true)}
                </span>
              </td>
              <td>
                <StackedBar mix={brand.volumeMix} size="lg" />
              </td>
              <td style={{ textAlign: 'center' }}><LogisticsBadge logistics={brand.logistics} threePl={brand.threePl} /></td>
              <td style={{ textAlign: 'center' }}><RatingBadge rating={brand.rating} /></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
