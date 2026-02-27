import { NextRequest, NextResponse } from 'next/server';
import { getFabricAuthHeader } from '@/lib/fabric-auth';
import { buildBackendUrl } from '@/lib/backend-config';


export async function GET(request: NextRequest) {
  try {
    // Get the authorization token from request headers
    const authToken = request.headers.get('x-auth-token');
    const authHeader = request.headers.get('authorization');

    if (!authToken && !authHeader) {
      return NextResponse.json(
        { error: 'Missing authentication token' },
        { status: 401 }
      );
    }

    // Get query parameters and forward them
    const { searchParams } = new URL(request.url);
    const queryString = searchParams.toString();

    // Build backend URL using centralized configuration
    const backendUrl = buildBackendUrl('/analytics/documents', queryString);

    const backendResponse = await fetch(backendUrl, {
      method: 'GET',
      headers: {
        'Authorization': authHeader || `Bearer ${authToken}`,
      },
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
    console.error('Document analytics proxy error:', error);
    return NextResponse.json(
      {
        error: 'Internal proxy error',
        details: error instanceof Error ? error.message : 'Unknown error'
      },
      { status: 500 }
    );
  }
}