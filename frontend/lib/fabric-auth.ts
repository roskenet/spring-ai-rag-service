// Fabric Gateway authentication utilities
import { NextRequest } from 'next/server';

export interface FabricTokenInfo {
  access_token?: string;
  uid?: string;
  scope?: string[];
  expires_in?: number;
  realm?: string;
}

export interface FabricUserInfo {
  uid: string;
  name?: string;
  email?: string;
  realm?: string;
}

/**
 * Extract token information from Fabric Gateway headers
 * Fabric Gateway typically forwards token info in X-TokenInfo-Forward header
 */
export function extractTokenFromFabricHeaders(request: NextRequest): string | null {
  // Check common Fabric Gateway headers
  const tokenInfoHeader = request.headers.get('x-tokeninfo-forward');
  const authHeader = request.headers.get('authorization');
  const tokenHeader = request.headers.get('x-token-forward');

  if (authHeader && authHeader.startsWith('Bearer ')) {
    return authHeader.split(' ')[1];
  }

  if (tokenHeader) {
    return tokenHeader;
  }

  if (tokenInfoHeader) {
    try {
      const tokenInfo: FabricTokenInfo = JSON.parse(tokenInfoHeader);
      return tokenInfo.access_token || null;
    } catch (error) {
      console.error('Failed to parse token info header:', error);
    }
  }

  return null;
}

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const padded = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const json = Buffer.from(padded, 'base64').toString('utf-8');
    return JSON.parse(json);
  } catch {
    return null;
  }
}

/**
 * Extract user information from Fabric Gateway headers
 */
export function extractUserFromFabricHeaders(request: NextRequest): FabricUserInfo | null {
  const tokenInfoHeader = request.headers.get('x-tokeninfo-forward');
  const userIdHeader = request.headers.get('x-uid-forward');

  let userInfo: FabricUserInfo | null = null;

  // Try to extract from x-tokeninfo-forward
  if (tokenInfoHeader) {
    try {
      const tokenInfo: FabricTokenInfo = JSON.parse(tokenInfoHeader);
      if (tokenInfo.uid) {
        userInfo = {
          uid: tokenInfo.uid,
          realm: tokenInfo.realm,
        };
      }
    } catch (error) {
      console.error('Failed to parse token info header:', error);
    }
  }

  // Try to extract from x-uid-forward
  if (!userInfo && userIdHeader) {
    userInfo = { uid: userIdHeader };
  }

  // Fall back: decode the Bearer JWT to get uid/sub/name from claims
  if (!userInfo) {
    const authHeader = request.headers.get('authorization');
    const tokenHeader = request.headers.get('x-token-forward');
    const raw = authHeader?.startsWith('Bearer ') ? authHeader.slice(7) : tokenHeader;
    if (raw) {
      const payload = decodeJwtPayload(raw);
      if (payload) {
        const uid =
          (payload['uid'] as string) ||
          (payload['sub'] as string) ||
          (payload['preferred_username'] as string) ||
          (payload['username'] as string) ||
          (payload['email'] as string) ||
          null;
        const name =
          (payload['name'] as string) ||
          (payload['display_name'] as string) ||
          (payload['given_name'] as string) ||
          null;
        const realm =
          (payload['realm'] as string) ||
          (payload['iss'] as string) ||
          null;
        if (uid) {
          userInfo = { uid, ...(name ? { name } : {}), ...(realm ? { realm } : {}) };
        }
      }
    }
  }

  return userInfo;
}

/**
 * Check if request is authenticated via Fabric Gateway
 */
export function isFabricAuthenticated(request: NextRequest): boolean {
  const token = extractTokenFromFabricHeaders(request);
  const userInfo = extractUserFromFabricHeaders(request);

  return !!(token || userInfo?.uid);
}

/**
 * Get authorization header for backend requests
 */
export function getFabricAuthHeader(request: NextRequest): string | null {
  const token = extractTokenFromFabricHeaders(request);
  return token ? `Bearer ${token}` : null;
}