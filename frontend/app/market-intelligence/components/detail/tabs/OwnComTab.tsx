import { Brand } from '../../../lib/types'
import { formatCurrency, formatNumber } from '../../../lib/utils'
import StackedBar from '../../overview/StackedBar'

export default function OwnComTab({ brand }: { brand: Brand }) {
  const totalTraffic = brand.euWebTraffic
  const totalGmv = brand.estimatedOwnEcomGmv
  const totalItems = brand.estimatedOwnEcomShippedItems

  return (
    <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 20, overflow: 'auto', flex: 1 }}>

      <div className="mi-card">
        <h3 style={{ margin: '0 0 14px', fontSize: 14, fontWeight: 600 }}>Volume mix · yearly estimate</h3>
        <StackedBar mix={brand.volumeMix} size="lg" />
      </div>

      <div className="mi-card">
        <div className="mi-row" style={{ justifyContent: 'space-between', marginBottom: 16 }}>
          <div>
            <h3 style={{ margin: 0, fontSize: 14, fontWeight: 600 }}>Own.com markets · yearly</h3>
            <div style={{ fontSize: 12, color: 'var(--mi-muted)', marginTop: 2 }}>Estimated direct-to-consumer footprint</div>
          </div>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 24 }}>
          <Stat label="Global web traffic" value={formatNumber(brand.globalWebTraffic, true)} sub="visits / year" />
          <Stat label="EU web traffic" value={formatNumber(totalTraffic, true)} sub="visits / year" />
          <Stat label="Estimated GMV" value={formatCurrency(totalGmv, true)} sub="own.com, all markets" />
          <Stat label="Estimated shipped items" value={formatNumber(totalItems, true)} sub="own.com, all markets" />
        </div>
      </div>

      {brand.markets.length === 0 ? (
        <div style={{ color: 'var(--mi-muted)', fontSize: 13 }}>No per-market data available for {brand.name}.</div>
      ) : (
        <div className="mi-card" style={{ padding: 0, overflow: 'hidden' }}>
          <table className="mi-table">
            <thead>
              <tr>
                <th>Market</th>
                <th>Web Traffic</th>
                <th>Est. GMV</th>
                <th>Shipped Items</th>
                <th>Carriers</th>
                <th>Payment Options</th>
                <th>Delivery Options</th>
              </tr>
            </thead>
            <tbody>
              {brand.markets.map(m => (
                <tr key={m.country} style={{ cursor: 'default' }}>
                  <td>
                    <span style={{
                      display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                      width: 32, height: 22, borderRadius: 4, fontSize: 11, fontWeight: 700,
                      background: 'var(--mi-surface-2)', color: 'var(--mi-ink-2)', letterSpacing: '0.04em',
                    }}>
                      {m.country}
                    </span>
                  </td>
                  <td className="mi-tnum" style={{ fontFamily: 'var(--mi-font-mono)', fontSize: 13 }}>{formatNumber(m.webTraffic, true)}</td>
                  <td className="mi-tnum" style={{ fontFamily: 'var(--mi-font-mono)', fontSize: 13 }}>{formatCurrency(m.estimatedGmv, true)}</td>
                  <td className="mi-tnum" style={{ fontFamily: 'var(--mi-font-mono)', fontSize: 13 }}>{formatNumber(m.estimatedShippedItems, true)}</td>
                  <td><TagList items={m.carriers} /></td>
                  <td><TagList items={m.paymentOptions} /></td>
                  <td><TagList items={m.deliveryOptions} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function Stat({ label, value, sub }: { label: string; value: string; sub: string }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <div style={{ fontSize: 11, color: 'var(--mi-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: 500 }}>{label}</div>
      <div className="mi-tnum" style={{ fontSize: 22, fontWeight: 600, letterSpacing: '-0.02em', fontFamily: 'var(--mi-font-mono)' }}>{value}</div>
      <div style={{ fontSize: 11, color: 'var(--mi-muted)' }}>{sub}</div>
    </div>
  )
}

function TagList({ items }: { items: string[] }) {
  if (items.length === 0) return <span style={{ color: 'var(--mi-muted)' }}>—</span>
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
      {items.map((item, i) => (
        <span key={i} className="mi-cell-pill" style={{ fontSize: 11, padding: '2px 7px' }}>{item}</span>
      ))}
    </div>
  )
}
