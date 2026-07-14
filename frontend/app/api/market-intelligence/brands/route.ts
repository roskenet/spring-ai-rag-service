import { NextResponse } from 'next/server'
import { readFile } from 'fs/promises'
import { join } from 'path'
import { parseBrandsFromCsv } from '@/app/market-intelligence/lib/parse-csv'

export async function GET() {
  try {
    const csvPath = join(process.cwd(), 'public', 'brands.csv')
    const csv = await readFile(csvPath, 'utf-8')
    const brands = parseBrandsFromCsv(csv)
    return NextResponse.json(brands)
  } catch (err) {
    console.error('Failed to load brands:', err)
    return NextResponse.json({ error: 'Failed to load brands' }, { status: 500 })
  }
}
