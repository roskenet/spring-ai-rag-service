/**
 * Backend configuration for different environments
 *
 * This centralizes all backend URL configuration to ensure consistency
 * across development, staging, and production environments.
 */

/**
 * Get the backend base URL based on environment
 *
 * Environment Variable: BACKEND_API_URL
 *
 * Examples:
 * - Local: http://localhost:8080
 * - Staging K8s: https://zeos-rag-backend-staging.zfs-test.zalan.do
 * - Production K8s: https://zeos-rag-backend.zalan.do
 *
 * NOTE: Do NOT include /api suffix - it's added per-endpoint
 */
export function getBackendBaseUrl(): string {
  const backendUrl = process.env.BACKEND_API_URL;

  if (!backendUrl) {
    // Default for local development
    return 'http://localhost:8080';
  }

  // Remove trailing slash if present
  return backendUrl.replace(/\/$/, '');
}

/**
 * Build a complete backend API URL
 *
 * @param endpoint - The API endpoint path (e.g., '/chat/ask', '/documents')
 * @param queryParams - Optional query parameters
 * @returns Complete backend URL
 *
 * Examples:
 * - buildBackendUrl('/chat/ask') → 'http://localhost:8080/api/chat/ask'
 * - buildBackendUrl('/chat/ask', 'timeout=5000') → 'http://localhost:8080/api/chat/ask?timeout=5000'
 * - buildBackendUrl('/documents/123') → 'http://localhost:8080/api/documents/123'
 */
export function buildBackendUrl(endpoint: string, queryParams?: string): string {
  const baseUrl = getBackendBaseUrl();

  // Ensure endpoint starts with /
  const path = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;

  // Build URL with /api prefix
  let url = `${baseUrl}/api${path}`;

  // Add query parameters if provided
  if (queryParams) {
    url += `?${queryParams}`;
  }

  return url;
}

/**
 * Log backend configuration (useful for debugging)
 */
export function logBackendConfig(): void {
  const backendUrl = getBackendBaseUrl();
  console.log('Backend Configuration:', {
    BACKEND_API_URL: process.env.BACKEND_API_URL || 'not set',
    resolvedBaseUrl: backendUrl,
    environment: process.env.NODE_ENV || 'development',
    exampleUrl: buildBackendUrl('/chat/ask'),
  });
}