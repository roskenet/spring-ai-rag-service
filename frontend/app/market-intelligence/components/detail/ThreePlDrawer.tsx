'use client'
import { X } from 'lucide-react'
import { Brand } from '../../lib/types'

const THREEPL_PROFILES: Record<string, {
  positioning: string
  strengths: string[]
  weaknesses: string[]
  zeosCounterPositioning: string
  contractNotes: string
}> = {
  GXO: {
    positioning: 'Global contract logistics leader with strong European fashion presence. Known for automation and large-scale DC operations.',
    strengths: ['Scale and network breadth', 'Strong automation capabilities', 'Established fashion client roster'],
    weaknesses: ['Less flexible for mid-market', 'Long contract lock-ins (3–5 yr)', 'Limited marketplace integration'],
    zeosCounterPositioning: 'ZEOS offers native Zalando marketplace integration that GXO cannot match. For brands where marketplace volume is growing, ZEOS delivers a unified fulfilment + sales channel.',
    contractNotes: 'Typical GXO contracts run 3–5 years with volume commitments. Watch for renewal windows in Q3/Q4.',
  },
  Arvato: {
    positioning: 'Bertelsmann-owned 3PL with strong German fashion and beauty client base. Mid-market focused.',
    strengths: ['Strong DE/AT/CH coverage', 'Fashion and beauty specialisation', 'Flexible contract terms'],
    weaknesses: ['Limited UK/Nordic reach', 'Technology stack can be outdated', 'Smaller scale than GXO/DHL'],
    zeosCounterPositioning: 'ZEOS pan-European coverage and real-time inventory visibility outperforms Arvato for brands scaling beyond DACH.',
    contractNotes: 'Arvato typical contract length 2–3 years. Mid-cycle reviews common.',
  },
  Bleckmann: {
    positioning: 'Fashion-only 3PL. Deep specialisation in apparel, footwear, and accessories across Europe.',
    strengths: ['Fashion-first processes', 'Strong omnichannel capability', 'Good returns management'],
    weaknesses: ['Fashion-only limits diversification', 'Premium pricing', 'Capacity constraints in peak'],
    zeosCounterPositioning: 'ZEOS Returns and ZEOS Content Services can complement or replace Bleckmann value-adds at lower cost with Zalando ecosystem benefits.',
    contractNotes: 'Bleckmann typically targets premium fashion. Check if brand has capacity concerns going into peak.',
  },
  Fiege: {
    positioning: 'Family-owned European 3PL with strong fashion and healthcare portfolio. Known for reliability.',
    strengths: ['Owner-managed, stable relationship', 'Good Central European coverage', 'Reliable SLAs'],
    weaknesses: ['Limited tech investment', 'Smaller international footprint', 'Less competitive on price'],
    zeosCounterPositioning: 'ZEOS technology stack and Zalando consumer data insights are a step-change vs Fiege\'s more traditional offering.',
    contractNotes: 'Fiege relationships tend to be long-term and relationship-driven. Identify the key decision-maker early.',
  },
  'DHL Supply Chain': {
    positioning: 'Global logistics giant. Broad European network with significant fashion and retail practice.',
    strengths: ['Unmatched network scale', 'Strong tech investment (Saloodo etc.)', 'End-to-end logistics capability'],
    weaknesses: ['Expensive for mid-market', 'Bureaucratic sales process', 'Not marketplace-native'],
    zeosCounterPositioning: 'ZEOS agility and Zalando marketplace integration is a genuine differentiator vs DHL\'s broad but non-specialised fashion offering.',
    contractNotes: 'DHL contracts are complex and multi-year. Look for dissatisfaction with service levels or pricing as entry points.',
  },
  'Kuehne+Nagel': {
    positioning: 'Global logistics and contract logistics. Strong in B2B; growing e-commerce practice.',
    strengths: ['Global scale', 'Strong B2B logistics', 'Solid financial stability'],
    weaknesses: ['E-commerce is not core', 'Less consumer-focused', 'Integration complexity'],
    zeosCounterPositioning: 'ZEOS is built for e-commerce from the ground up. For brands where D2C is strategic, ZEOS beats K+N on every e-commerce KPI.',
    contractNotes: 'K+N e-commerce contracts often sit alongside broader freight/forwarding relationships. Map the full relationship before approaching.',
  },
}

type Props = {
  brand: Brand
  onClose: () => void
}

export default function ThreePlDrawer({ brand, onClose }: Props) {
  const profile = brand.threePl ? THREEPL_PROFILES[brand.threePl] : null

  return (
    <>
      <div className="mi-drawer-overlay" onClick={onClose} />
      <div className="mi-drawer">
        <div className="mi-row" style={{ justifyContent: 'space-between', marginBottom: 24 }}>
          <div>
            <div style={{ fontSize: 11, color: 'var(--mi-muted)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 4 }}>
              3PL Competitive Intel
            </div>
            <div style={{ fontSize: 20, fontWeight: 700, letterSpacing: '-0.02em' }}>{brand.threePl}</div>
          </div>
          <button className="mi-btn ghost" onClick={onClose} style={{ padding: 8 }}>
            <X size={16} />
          </button>
        </div>

        {!profile ? (
          <p style={{ color: 'var(--mi-muted)', fontSize: 13 }}>No profile available for {brand.threePl}.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            <div className="mi-card">
              <div className="mi-card-h">Positioning</div>
              <p style={{ margin: 0, fontSize: 13.5, lineHeight: 1.6, color: 'var(--mi-ink-2)' }}>{profile.positioning}</p>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
              <div className="mi-card">
                <div className="mi-card-h">Strengths</div>
                <ul style={{ margin: 0, paddingLeft: 16, display: 'flex', flexDirection: 'column', gap: 6 }}>
                  {profile.strengths.map((s, i) => (
                    <li key={i} style={{ fontSize: 13, color: 'var(--mi-ink-2)' }}>{s}</li>
                  ))}
                </ul>
              </div>
              <div className="mi-card">
                <div className="mi-card-h">Weaknesses</div>
                <ul style={{ margin: 0, paddingLeft: 16, display: 'flex', flexDirection: 'column', gap: 6 }}>
                  {profile.weaknesses.map((w, i) => (
                    <li key={i} style={{ fontSize: 13, color: 'var(--mi-ink-2)' }}>{w}</li>
                  ))}
                </ul>
              </div>
            </div>

            <div className="mi-card" style={{ background: 'var(--mi-accent-soft)', borderColor: 'var(--mi-accent)' }}>
              <div className="mi-card-h">How ZEOS wins against {brand.threePl}</div>
              <p style={{ margin: 0, fontSize: 13.5, lineHeight: 1.6, color: 'var(--mi-accent-ink)' }}>
                {profile.zeosCounterPositioning}
              </p>
            </div>

            <div className="mi-card">
              <div className="mi-card-h">Contract Notes</div>
              <p style={{ margin: 0, fontSize: 13, lineHeight: 1.6, color: 'var(--mi-ink-2)' }}>{profile.contractNotes}</p>
            </div>
          </div>
        )}
      </div>
    </>
  )
}
