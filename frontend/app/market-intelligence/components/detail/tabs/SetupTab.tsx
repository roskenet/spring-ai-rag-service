'use client'
import { Brand, Narrative } from '../../../lib/types'

type Props = {
  brand: Brand
  narrative: Narrative | null
  loading: boolean
}

export default function SetupTab({ brand, narrative, loading }: Props) {
  return (
    <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 20, overflow: 'auto', flex: 1 }}>
      <div className="mi-card">
        <div className="mi-row" style={{ justifyContent: 'space-between', marginBottom: 14 }}>
          <div className="mi-card-h" style={{ margin: 0 }}>AI Sales Narrative</div>
          <span style={{ fontSize: 11, color: 'var(--mi-muted)', background: 'var(--mi-surface-2)', padding: '2px 8px', borderRadius: 6 }}>
            Claude · Bedrock
          </span>
        </div>
        {loading && <LoadingSkeleton />}
        {narrative && !loading && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <p style={{ fontSize: 13.5, lineHeight: 1.6, color: 'var(--mi-ink-2)', margin: 0 }}>{narrative.intro}</p>
            <div>
              <div className="mi-card-h" style={{ marginBottom: 10 }}>Product hooks</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {narrative.hooks.map((h, i) => (
                  <div key={i} style={{ background: 'var(--mi-surface-2)', borderRadius: 10, padding: '12px 14px' }}>
                    <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--mi-accent-ink)', marginBottom: 4 }}>{h.product}</div>
                    <div style={{ fontSize: 12, color: 'var(--mi-muted)', marginBottom: 6 }}>Pain: {h.painPoint}</div>
                    <div style={{ fontSize: 13, lineHeight: 1.5 }}>{h.hook}</div>
                  </div>
                ))}
              </div>
            </div>
            {narrative.watchouts.length > 0 && (
              <div>
                <div className="mi-card-h" style={{ marginBottom: 8 }}>Watch-outs</div>
                <ul style={{ margin: 0, paddingLeft: 16, display: 'flex', flexDirection: 'column', gap: 6 }}>
                  {narrative.watchouts.map((w, i) => (
                    <li key={i} style={{ fontSize: 13, color: 'var(--mi-ink-2)' }}>{w}</li>
                  ))}
                </ul>
              </div>
            )}
            <div>
              <div className="mi-card-h" style={{ marginBottom: 8 }}>Conversation openers</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {narrative.openers.map((o, i) => (
                  <div key={i} style={{ fontSize: 13, fontStyle: 'italic', color: 'var(--mi-ink-2)', borderLeft: '3px solid var(--mi-accent)', paddingLeft: 12, lineHeight: 1.5 }}>
                    {o}
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

function LoadingSkeleton() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--mi-muted)', fontSize: 13 }}>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ animation: 'spin 1s linear infinite' }}>
          <path d="M21 12a9 9 0 1 1-3-6.7L21 8"/><path d="M21 3v5h-5"/>
        </svg>
        Generating AI narrative via Bedrock…
      </div>
      {[80, 60, 90, 50].map((w, i) => (
        <div key={i} style={{ height: 12, borderRadius: 6, background: 'var(--mi-surface-2)', width: `${w}%`, opacity: 0.6 }} />
      ))}
      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
    </div>
  )
}
