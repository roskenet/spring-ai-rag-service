/**
 * Session-scoped storage for the Zalando bearer token.
 * Uses sessionStorage so the token is scoped to the browser tab and cleared on close.
 * Never written to localStorage or sent anywhere except the Authorization header.
 */

const KEY = 'zeos_bearer_token';

export function setToken(token: string): void {
  if (typeof window === 'undefined') return;
  sessionStorage.setItem(KEY, token.trim());
}

export function getToken(): string | null {
  if (typeof window === 'undefined') return null;
  return sessionStorage.getItem(KEY);
}

export function clearToken(): void {
  if (typeof window === 'undefined') return;
  sessionStorage.removeItem(KEY);
}

export function hasToken(): boolean {
  return getToken() !== null;
}

/** Returns token expiry as Date, or null if not parseable. */
export function getTokenExpiry(token: string): Date | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')));
    if (!payload.exp) return null;
    return new Date(payload.exp * 1000);
  } catch {
    return null;
  }
}

export function isTokenExpired(token: string): boolean {
  const expiry = getTokenExpiry(token);
  if (!expiry) return false; // can't tell, assume valid
  return expiry < new Date();
}
