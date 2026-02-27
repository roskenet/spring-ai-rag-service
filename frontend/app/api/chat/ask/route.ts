import { NextRequest, NextResponse } from 'next/server';
import { getFabricAuthHeader } from '@/lib/fabric-auth';
import { buildBackendUrl, logBackendConfig } from '@/lib/backend-config';

export async function POST(request: NextRequest) {
  try {
    // Try to get auth header from Fabric Gateway (optional)
    const authHeader = getFabricAuthHeader(request);

    // Log what we received for debugging
    console.log('Chat API - Auth header present:', !!authHeader);
    if (authHeader) {
      console.log('Chat API - Auth header starts with:', authHeader.substring(0, 20) + '...');
    }

    // Get the request body
    const body = await request.json();

    // Build backend URL using centralized configuration
    const backendUrl = buildBackendUrl('/chat/ask');

    // Log config on first request (for debugging)
    if (!process.env._BACKEND_CONFIG_LOGGED) {
      logBackendConfig();
      process.env._BACKEND_CONFIG_LOGGED = 'true';
    }

    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    };

    // Add auth header if available (optional)
    if (authHeader) {
      headers['Authorization'] = authHeader;
      console.log('Chat API - Forwarding auth to backend');
    } else {
      console.log('Chat API - No auth header to forward (proceeding anyway)');
    }

    const backendResponse = await fetch(backendUrl, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
    });

    // Check if the backend response is successful
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

    // Get the response data
    const responseData = await backendResponse.json();

    // Return the backend response
    return NextResponse.json(responseData);

  } catch (error) {
    console.error('Chat API proxy error:', error);
    return NextResponse.json(
      {
        error: 'Internal proxy error',
        details: error instanceof Error ? error.message : 'Unknown error'
      },
      { status: 500 }
    );
  }
}