// JWT access-token storage.
//
// Stored in localStorage: simple and persists across page refreshes (so the user
// stays logged in), which suits this project. NOTE: localStorage is readable by any
// JavaScript on the page, so it is exposed to XSS — acceptable here, but a hardened
// production app would prefer an httpOnly cookie or in-memory storage.
const TOKEN_KEY = 'creatorhub.jwt';

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}
