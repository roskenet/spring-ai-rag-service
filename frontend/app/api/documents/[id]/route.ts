import { NextRequest, NextResponse } from 'next/server';
import { getFabricAuthHeader } from '@/lib/fabric-auth';
import { buildBackendUrl } from '@/lib/backend-config';

export async function DELETE(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    // Try to get auth header from Fabric Gateway (optional)
    const authHeader = getFabricAuthHeader(request);
    console.log('Document Delete API - Auth header present:', !!authHeader);

    const documentId = params.id;

    // Build backend URL using centralized configuration
    const backendUrl = buildBackendUrl(`/documents/${documentId}`);

    const headers: HeadersInit = {};

    // Add auth header if available (optional)
    if (authHeader) {
      headers['Authorization'] = authHeader;
      console.log('Document Delete API - Forwarding auth to backend');
    } else {
      console.log('Document Delete API - No auth header to forward (proceeding anyway)');
    }

    const backendResponse = await fetch(backendUrl, {
      method: 'DELETE',
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

    // For DELETE requests, backend might return empty response
    const responseText = await backendResponse.text();
    const responseData = responseText ? JSON.parse(responseText) : {};

    return NextResponse.json(responseData);

  } catch (error) {
    console.error('Document delete proxy error:', error);
    return NextResponse.json(
      {
        error: 'Internal proxy error',
        details: error instanceof Error ? error.message : 'Unknown error'
      },
      { status: 500 }
    );
  }
}