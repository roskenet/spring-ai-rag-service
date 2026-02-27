import { NextRequest, NextResponse } from 'next/server';
import { getFabricAuthHeader } from '@/lib/fabric-auth';
import { buildBackendUrl } from '@/lib/backend-config';

export async function GET(request: NextRequest) {
  try {
    // Try to get auth header from Fabric Gateway (optional)
    const authHeader = getFabricAuthHeader(request);

    // Log what we received for debugging
    console.log('Documents API - Auth header present:', !!authHeader);

    // Build backend URL using centralized configuration
    const backendUrl = buildBackendUrl('/documents');

    const headers: HeadersInit = {};

    // Add auth header if available (optional)
    if (authHeader) {
      headers['Authorization'] = authHeader;
      console.log('Documents API - Forwarding auth to backend');
    } else {
      console.log('Documents API - No auth header to forward (proceeding anyway)');
    }

    const backendResponse = await fetch(backendUrl, {
      method: 'GET',
      headers,
    });

    if (!backendResponse.ok) {
      const errorText = await backendResponse.text();
      console.error(`Backend error (${backendResponse.status}):`, errorText);

      return NextResponse.json(
        {
          error: `Backend API error: ${backendResponse.status} ${backendResponse.statusText}`,
          details: errorText
        },
        { status: backendResponse.status }
      );
    }

    const responseData = await backendResponse.json();
    return NextResponse.json(responseData);

  } catch (error) {
    console.error('Documents list proxy error:', error);
    return NextResponse.json(
      {
        error: 'Internal proxy error',
        details: error instanceof Error ? error.message : 'Unknown error'
      },
      { status: 500 }
    );
  }
}