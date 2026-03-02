import { NextRequest } from 'next/server';
import { getFabricAuthHeader } from './fabric-auth';

const BACKEND_BASE_URL = process.env.BACKEND_API_URL || 'http://localhost:8080';

export interface ProxyOptions {
  endpoint: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  body?: any;
  isFormData?: boolean;
}

/**
 * Create headers for backend request with optional auth
 */
export function createBackendHeaders(
  request: NextRequest,
  apiName: string,
  options: ProxyOptions
): HeadersInit {
  const headers: HeadersInit = {};

  // Try to get auth header from Fabric Gateway (optional)
  const authHeader = getFabricAuthHeader(request);

  if (authHeader) {
    headers['Authorization'] = authHeader;
    console.log(`${apiName} - Auth header present, forwarding to backend`);
  } else {
    console.log(`${apiName} - No auth header found (proceeding without auth)`);
  }

  // Set content type for non-FormData requests
  if (!options.isFormData && (options.method === 'POST' || options.method === 'PUT')) {
    headers['Content-Type'] = 'application/json';
  }

  // Log all received headers for debugging
  console.log(`${apiName} - Received headers:`, {
    'authorization': request.headers.get('authorization') ? 'present' : 'missing',
    'x-tokeninfo-forward': request.headers.get('x-tokeninfo-forward') ? 'present' : 'missing',
    'x-token-forward': request.headers.get('x-token-forward') ? 'present' : 'missing',
    'x-uid-forward': request.headers.get('x-uid-forward') ? 'present' : 'missing',
  });

  return headers;
}

/**
 * Create backend URL
 */
export function createBackendUrl(path: string, searchParams?: URLSearchParams): string {
  const baseUrl = `${BACKEND_BASE_URL}/api${path}`;
  return searchParams?.toString() ? `${baseUrl}?${searchParams.toString()}` : baseUrl;
}