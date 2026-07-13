import Papa from 'papaparse'
import { Brand, Market } from './types'
import { slugify } from './utils'

function parseNumber(val: string): number {
  if (!val || val === 'Use Placeholder') return 0
  return Number(val.replace(/[€,\s]/g, '')) || 0
}

function parseList(val: string): string[] {
  if (!val || val === 'Use Placeholder') return []
  return val.split(',').map(s => s.trim()).filter(Boolean)
}

type RawRow = Record<string, string>

function buildMarkets(row: RawRow): Market[] {
  const markets: Market[] = []
  const countries = ['DE', 'UK']

  for (const c of countries) {
    const traffic = parseNumber(row[`${c} Web traffic`])
    const gmv = parseNumber(row[`${c} Estimated GMV`])
    const items = parseNumber(row[`${c} Estimated shipped items`])
    if (traffic || gmv || items) {
      markets.push({
        country: c,
        webTraffic: traffic,
        estimatedGmv: gmv,
        estimatedShippedItems: items,
        carriers: parseList(row[`${c} Carriers`]),
        paymentOptions: parseList(row[`${c} Payment options`]),
        deliveryOptions: parseList(row[`${c} Delivery options`]),
      })
    }
  }
  return markets
}

export function parseBrandsFromCsv(csvText: string): Brand[] {
  const { data } = Papa.parse<RawRow>(csvText, {
    header: true,
    skipEmptyLines: true,
  })

  return data.map((row): Brand => ({
    id: slugify(row['Brand'] || ''),
    name: row['Brand'] || '',
    parentCompany: row['Parent company'] || '',
    prioritySegment: row['Priority Segment'] || '',
    segment: row['Segment'] || '',
    onlineRevenue: parseNumber(row['Online Revenue']),
    existingOrPotential: row['Existing vs. Potential'] || '',
    volumeMix: {
      ppb: parseNumber(row['Volume distribution  PPB']),
      marketplaces: parseNumber(row['Volume distribution  Marketplaces']),
      ownCom: parseNumber(row['Volume distribution  Own.com']),
    },
    logistics: row['Logistics setup'] || '',
    threePl: row['3PL company'] || null,
    rating: row['Rating'] || '',
    warehouseLocation: row['Warehouse location'] || '',
    hq: row['HQ'] || '',
    globalWebTraffic: parseNumber(row['Global Web Traffic']),
    euWebTraffic: parseNumber(row['Eu Web Traffic']),
    estimatedOwnEcomGmv: parseNumber(row['Estimated own ecom GMV']),
    estimatedOwnEcomShippedItems: parseNumber(row['Estimated own ecom shipped items']),
    markets: buildMarkets(row),
  })).filter(b => b.name)
}
