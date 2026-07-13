'use client'
import { Search } from 'lucide-react'
import Image from 'next/image'

type Props = {
  search?: string
  onSearch?: (v: string) => void
}

export default function TopBar({ search = '', onSearch }: Props) {
  return (
    <header className="mi-topbar">
      <div className="mi-row" style={{ gap: 10 }}>
        <Image src="/zeos-logo.png" alt="ZEOS" width={28} height={28} style={{ objectFit: 'contain' }} />
        <span style={{ fontWeight: 700, fontSize: 16, letterSpacing: '-0.02em', color: 'var(--mi-ink)' }}>ZEOS</span>
        <span style={{ color: 'var(--mi-muted)', fontSize: 13, fontWeight: 400, marginLeft: 4 }}>Market Intelligence</span>
      </div>
      <nav className="mi-nav">
        <a className="active">Hunting List</a>
      </nav>
      <div className="mi-search">
        <Search size={14} color="var(--mi-muted)" />
        <input
          placeholder="Search brands…"
          value={search}
          onChange={e => onSearch?.(e.target.value)}
        />
      </div>
      <div className="mi-user">SG</div>
    </header>
  )
}
