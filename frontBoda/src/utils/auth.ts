// Gestión centralizada de la sesión del invitado (token JWT en localStorage)
const API_URL = import.meta.env.PUBLIC_USERS_API_URL || 'http://localhost:8082';
const IMAGES_API_URL = import.meta.env.PUBLIC_IMAGES_API_URL || 'http://localhost:8084';

const TOKEN_KEY = 'token';
const USER_KEY = 'user';

export interface Session {
  token: string;
  email: string;
  family: any;
}

export function isBrowser(): boolean {
  return typeof window !== 'undefined' && typeof window.localStorage === 'object';
}

export function getApiUrl(): string {
  return API_URL;
}

export function getImagesApiUrl(): string {
  return IMAGES_API_URL;
}

function safeGetItem(key: string): string | null {
  if (!isBrowser()) return null;
  try { return localStorage.getItem(key); } catch { return null; }
}

function safeSetItem(key: string, value: string): void {
  if (!isBrowser()) return;
  try { localStorage.setItem(key, value); } catch {}
}

function safeRemoveItem(key: string): void {
  if (!isBrowser()) return;
  try { localStorage.removeItem(key); } catch {}
}

export function getToken(): string | null {
  return safeGetItem(TOKEN_KEY);
}

export function getStoredFamily(): any | null {
  const raw = safeGetItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function saveSession(token: string, family: unknown): void {
  safeSetItem(TOKEN_KEY, token);
  safeSetItem(USER_KEY, JSON.stringify(family));
}

export function clearSession(): void {
  safeRemoveItem(TOKEN_KEY);
  safeRemoveItem(USER_KEY);
}

export function logout(redirectTo = '/login'): void {
  clearSession();
  if (isBrowser()) window.location.href = redirectTo;
}

function decodePayload(token: string): { exp?: number; sub?: string } | null {
  try {
    const part = token.split('.')[1];
    if (!part) return null;
    const base64 = part.replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64));
  } catch {
    return null;
  }
}

export function isTokenExpired(token: string): boolean {
  const payload = decodePayload(token);
  if (!payload?.exp) return true;
  return payload.exp * 1000 <= Date.now();
}

export function getSessionEmail(token: string): string | null {
  return decodePayload(token)?.sub ?? null;
}

// Hay sesión local válida (existe token y no ha expirado)
export function hasActiveSession(): boolean {
  const token = getToken();
  return !!token && !isTokenExpired(token);
}

// Valida el token contra el backend y refresca los datos de la familia.
// Devuelve la sesión o null si el token no es válido.
export async function restoreSession(): Promise<Session | null> {
  const token = getToken();
  if (!token || isTokenExpired(token)) {
    clearSession();
    return null;
  }

  const email = getSessionEmail(token) || '';

  try {
    const res = await fetch(`${API_URL}/api/auth/me`, {
      headers: { Authorization: `Bearer ${token}` },
    });

    if (!res.ok) {
      clearSession();
      return null;
    }

    const data = (await res.json()) as { email?: string; family?: unknown };
    saveSession(token, data.family);
    return { token, email: data.email || email, family: data.family };
  } catch {
    // Error de red (backend caído): mantenemos la sesión local si ya existe una familia guardada.
    const family = getStoredFamily();
    return family ? { token, email, family } : null;
  }
}

export function initializeSession(token: string, family: unknown): Session {
  saveSession(token, family);
  return {
    token,
    email: getSessionEmail(token) || '',
    family,
  };
}

// Para páginas protegidas: restaura la sesión o redirige al login
export async function requireAuth(redirectTo = '/login'): Promise<Session | null> {
  if (!isBrowser()) return null;
  const session = await restoreSession();
  if (!session) {
    window.location.href = redirectTo;
    return null;
  }
  return session;
}

// Busca la persona logueada dentro de la familia a partir del email del token
export function findLoggedPerson(family: any, email: string): any | null {
  if (!family?.people || !email) return null;
  const normalized = email.trim().toLowerCase();
  return (
    family.people.find(
      (p: any) => p.email && p.email.trim().toLowerCase() === normalized
    ) ?? null
  );
}

const profileUrlCache = new Map<string, string>();

export async function resolveProfileImageUrl(key?: string | null): Promise<string | null> {
  if (!key) return null;
  const normalized = key.trim();
  if (!normalized) return null;

  const cached = profileUrlCache.get(normalized);
  if (cached) return cached;

  try {
    const res = await fetch(`${IMAGES_API_URL}/api/images/url?key=${encodeURIComponent(normalized)}`);
    if (!res.ok) return null;
    const data = await res.json() as { url?: string };
    if (!data.url) return null;
    profileUrlCache.set(normalized, data.url);
    return data.url;
  } catch {
    return null;
  }
}
