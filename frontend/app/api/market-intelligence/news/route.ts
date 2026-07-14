import { NextRequest, NextResponse } from 'next/server'
import { buildBackendUrl } from '@/lib/backend-config'
import { getFabricAuthHeader } from '@/lib/fabric-auth'

export async function POST(request: NextRequest) {
  try {
    const body = await request.json()
    const backendUrl = buildBackendUrl('/market-intelligence/news')

    const headers: HeadersInit = { 'Content-Type': 'application/json' }
    const authHeader = getFabricAuthHeader(request)
    if (authHeader) headers['Authorization'] = authHeader

    const backendResponse = await fetch(backendUrl, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
    })

    if (!backendResponse.ok) {
      const errorText = await backendResponse.text()
      console.error(`Market Intelligence news backend error (${backendResponse.status}):`, errorText)
      return NextResponse.json(
        { error: `Backend error: ${backendResponse.status}`, details: errorText },
        { status: backendResponse.status }
      )
    }

    return NextResponse.json(await backendResponse.json())
  } catch (error) {
    console.error('Market Intelligence news proxy error:', error)
    return NextResponse.json(
      { error: 'Internal proxy error', details: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 }
    )
  }
}
