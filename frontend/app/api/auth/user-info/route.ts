import { NextRequest, NextResponse } from 'next/server';
import { extractUserFromFabricHeaders, extractTokenFromFabricHeaders, isFabricAuthenticated } from '@/lib/fabric-auth';

export async function GET(request: NextRequest) {
  try {
    // Extract all available information (don't require authentication)
    const userInfo = extractUserFromFabricHeaders(request);
    const token = extractTokenFromFabricHeaders(request);
    const isAuthenticated = isFabricAuthenticated(request);

    // Get all headers for debugging
    const allHeaders: Record<string, string> = {};
    request.headers.forEach((value, key) => {
      // Log header presence but mask sensitive values
      if (key.toLowerCase().includes('auth') || key.toLowerCase().includes('token')) {
        allHeaders[key] = value ? `present (${value.length} chars)` : 'missing';
      } else {
        allHeaders[key] = value;
      }
    });

    // Log detailed debug info
    console.log('=== USER INFO DEBUG ===');
    console.log('All headers received:', allHeaders);
    console.log('Extracted user info:', userInfo);
    console.log('Has token:', !!token);
    console.log('Is authenticated:', isAuthenticated);
    console.log('========================');

    return NextResponse.json({
      // Always return info, even if not fully authenticated
      authenticated: isAuthenticated,
      uid: userInfo?.uid || null,
      name: userInfo?.uid || null,
      realm: userInfo?.realm || null,
      hasToken: !!token,
      timestamp: new Date().toISOString(),

      // Debug information (helpful for staging)
      debug: {
        headersReceived: Object.keys(allHeaders).length,
        fabricHeaders: {
          'x-tokeninfo-forward': request.headers.get('x-tokeninfo-forward') ? 'present' : 'missing',
          'x-uid-forward': request.headers.get('x-uid-forward') ? 'present' : 'missing',
          'authorization': request.headers.get('authorization') ? 'present' : 'missing',
          'x-token-forward': request.headers.get('x-token-forward') ? 'present' : 'missing',
        },
        userAgent: request.headers.get('user-agent'),
        host: request.headers.get('host'),
        origin: request.headers.get('origin'),
        referer: request.headers.get('referer'),
      },

      // Show raw header values for debugging (be careful in production)
      rawHeaders: process.env.NODE_ENV === 'development' ? allHeaders : undefined,
    });

  } catch (error) {
    console.error('Error extracting user info:', error);
    return NextResponse.json(
      {
        authenticated: false,
        error: 'Failed to extract user information',
        details: error instanceof Error ? error.message : 'Unknown error',
        debug: {
          error: error instanceof Error ? error.message : 'Unknown error',
          timestamp: new Date().toISOString(),
        }
      },
      { status: 200 } // Don't return 500, so we can see the debug info
    );
  }
}