export type Market = {
  country: string
  webTraffic: number
  estimatedGmv: number
  estimatedShippedItems: number
  carriers: string[]
  paymentOptions: string[]
  deliveryOptions: string[]
}

export type VolumeMix = {
  ppb: number
  marketplaces: number
  ownCom: number
}

export type Brand = {
  id: string
  name: string
  parentCompany: string
  prioritySegment: string
  segment: string
  onlineRevenue: number
  existingOrPotential: string
  volumeMix: VolumeMix
  logistics: string
  threePl: string | null
  rating: string
  warehouseLocation: string
  hq: string
  globalWebTraffic: number
  euWebTraffic: number
  estimatedOwnEcomGmv: number
  estimatedOwnEcomShippedItems: number
  markets: Market[]
}

export type Narrative = {
  intro: string
  hooks: { product: string; painPoint: string; hook: string }[]
  watchouts: string[]
  leadWith: string[]
  openers: string[]
}

export type NewsItem = {
  text: string
  date: string
  source: string
}

export type NewsCategories = {
  pressReleases: NewsItem[]
  mandA: NewsItem[]
  ecommerceLogistics: NewsItem[]
  financialReports: NewsItem[]
  ecommerceNews: NewsItem[]
  managementChanges: NewsItem[]
}
