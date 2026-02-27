import { NextRequest, NextResponse } from 'next/server';
import { getFabricAuthHeader } from '@/lib/fabric-auth';
import { buildBackendUrl } from '@/lib/backend-config';

export async function POST(request: NextRequest) {
  try {
    // Try to get auth header from Fabric Gateway (optional)
    const authHeader = getFabricAuthHeader(request);
    console.log('Document Upload API - Auth header present:', !!authHeader);

    // Get the form data
    const formData = await request.formData();

    // Build backend URL using centralized configuration
    const backendUrl = buildBackendUrl('/documents/upload');

    const headers: HeadersInit = {};

    // Add auth header if available (optional)
    if (authHeader) {
      headers['Authorization'] = authHeader;
      console.log('Document Upload API - Forwarding auth to backend');
    } else {
      console.log('Document Upload API - No auth header to forward (proceeding anyway)');
    }

    const backendResponse = await fetch(backendUrl, {
      method: 'POST',
      headers,
      body: formData,
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
    console.error('Document upload proxy error:', error);
    return NextResponse.json(
      {
        error: 'Internal proxy error',
        details: error instanceof Error ? error.message : 'Unknown error'
      },
      { status: 500 }
    );
  }
}