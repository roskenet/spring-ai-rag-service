export function slugify(str: string): string {
  return str.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '')
}

export function formatCurrency(n: number | undefined | null, compact = false): string {
  if (n == null || isNaN(n)) return '—'
  if (compact && n >= 1_000_000) return `€${(n / 1_000_000).toFixed(1)}M`
  if (compact && n >= 1_000) return `€${(n / 1_000).toFixed(0)}K`
  return `€${n.toLocaleString('de-DE')}`
}

export function formatNumber(n: number | undefined | null, compact = false): string {
  if (n == null || isNaN(n)) return '—'
  if (compact && n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`
  if (compact && n >= 1_000) return `${(n / 1_000).toFixed(0)}K`
  return n.toLocaleString('de-DE')
}

export function ratingClass(rating: string): 'high' | 'med' | 'low' {
  const r = rating.toLowerCase()
  if (r === 'high') return 'high'
  if (r === 'medium' || r === 'med') return 'med'
  return 'low'
}

const BRAND_COLORS = [
  '#6B4FBF', '#3D7EAA', '#B85C3A', '#4A9B6F', '#A0522D',
  '#5F7A8A', '#8B6914', '#6A5ACD', '#2E8B57', '#CD5C5C',
]

export function brandColor(name: string): string {
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
  return BRAND_COLORS[Math.abs(hash) % BRAND_COLORS.length]
}

export function initials(name: string): string {
  return name.split(/\s+/).slice(0, 2).map(w => w[0]).join('').toUpperCase()
}
