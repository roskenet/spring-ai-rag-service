import { VolumeMix } from '../../lib/types'

type Props = {
  mix: VolumeMix
  size?: 'sm' | 'lg'
  showLegend?: boolean
}

export default function StackedBar({ mix, size = 'sm', showLegend = false }: Props) {
  const total = mix.ppb + mix.marketplaces + mix.ownCom
  if (total === 0) return <span className="mi-muted" style={{ fontSize: 12 }}>—</span>

  const ppbPct = (mix.ppb / total) * 100
  const mktPct = (mix.marketplaces / total) * 100
  const ownPct = (mix.ownCom / total) * 100

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 5, minWidth: 140 }}>
      <div className={`mi-sbar ${size === 'lg' ? 'lg' : ''}`}>
        <div className="mi-seg" style={{ width: `${ppbPct}%`, background: 'var(--mi-seg-ppb)' }} title={`Zalando: ${ppbPct.toFixed(0)}%`} />
        <div className="mi-seg" style={{ width: `${mktPct}%`, background: 'var(--mi-seg-mkt)' }} title={`Other Marketplaces: ${mktPct.toFixed(0)}%`} />
        <div className="mi-seg" style={{ width: `${ownPct}%`, background: 'var(--mi-seg-own)' }} title={`Own e-com: ${ownPct.toFixed(0)}%`} />
      </div>
      {size === 'lg' ? (
        <div style={{ display: 'flex', width: '100%', fontSize: 11, color: 'var(--mi-muted)', fontFamily: 'var(--mi-font-mono)' }}>
          <div style={{ width: `${ppbPct}%`, minWidth: 0, overflow: 'hidden', textAlign: 'center' }}>
            <div style={{ whiteSpace: 'nowrap' }}>Zalando</div>
            <div style={{ whiteSpace: 'nowrap', fontWeight: 500, color: 'var(--mi-ink-2)' }}>{ppbPct.toFixed(0)}%</div>
          </div>
          <div style={{ width: `${mktPct}%`, minWidth: 0, overflow: 'hidden', textAlign: 'center' }}>
            <div style={{ whiteSpace: 'nowrap' }}>Other Mkt</div>
            <div style={{ whiteSpace: 'nowrap', fontWeight: 500, color: 'var(--mi-ink-2)' }}>{mktPct.toFixed(0)}%</div>
          </div>
          <div style={{ width: `${ownPct}%`, minWidth: 0, overflow: 'hidden', textAlign: 'center' }}>
            <div style={{ whiteSpace: 'nowrap' }}>Own e-com</div>
            <div style={{ whiteSpace: 'nowrap', fontWeight: 500, color: 'var(--mi-ink-2)' }}>{ownPct.toFixed(0)}%</div>
          </div>
        </div>
      ) : (
        <div style={{ display: 'flex', gap: 6, fontSize: 10, color: 'var(--mi-muted)', fontFamily: 'var(--mi-font-mono)' }}>
          <span style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <span style={{ width: 6, height: 6, borderRadius: 1, background: 'var(--mi-seg-ppb)', flexShrink: 0, display: 'inline-block' }} />
            Zalando {ppbPct.toFixed(0)}%
          </span>
          <span style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <span style={{ width: 6, height: 6, borderRadius: 1, background: 'var(--mi-seg-mkt)', flexShrink: 0, display: 'inline-block' }} />
            Other Mkt {mktPct.toFixed(0)}%
          </span>
          <span style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <span style={{ width: 6, height: 6, borderRadius: 1, background: 'var(--mi-seg-own)', flexShrink: 0, display: 'inline-block' }} />
            Own e-com {ownPct.toFixed(0)}%
          </span>
        </div>
      )}
      {showLegend && (
        <div className="mi-sbar-legend">
          <span className="item"><span className="swatch" style={{ background: 'var(--mi-seg-ppb)' }} />Zalando {ppbPct.toFixed(0)}%</span>
          <span className="item"><span className="swatch" style={{ background: 'var(--mi-seg-mkt)' }} />Other Mkt {mktPct.toFixed(0)}%</span>
          <span className="item"><span className="swatch" style={{ background: 'var(--mi-seg-own)' }} />Own e-com {ownPct.toFixed(0)}%</span>
        </div>
      )}
    </div>
  )
}
