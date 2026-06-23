import axios, { AxiosError } from 'axios';
import type { ApiErrorResponse } from '../types';

/**
 * Axios instance. Same-origin in dev via the Vite proxy (/api -> :8081), so
 * session + XSRF-TOKEN cookies flow and JS can read the CSRF cookie.
 */
export const api = axios.create({
  baseURL: '/',
  withCredentials: true,
});

function readCookie(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((c) => c.startsWith(name + '='))
    ?.split('=')[1];
}

let csrfPrimed = false;

/** Ensure the XSRF-TOKEN cookie exists (any GET through the chain sets it). */
async function ensureCsrf(): Promise<void> {
  if (csrfPrimed || readCookie('XSRF-TOKEN')) {
    csrfPrimed = true;
    return;
  }
  try {
    await api.get('/api/auth/csrf');
  } catch {
    /* ignore — the cookie filter still sets it */
  }
  csrfPrimed = true;
}

// Attach the CSRF token to state-changing requests.
api.interceptors.request.use(async (config) => {
  const method = (config.method ?? 'get').toLowerCase();
  if (['post', 'put', 'patch', 'delete'].includes(method)) {
    await ensureCsrf();
    const token = readCookie('XSRF-TOKEN');
    if (token) {
      config.headers.set('X-XSRF-TOKEN', decodeURIComponent(token));
    }
  }
  return config;
});

/** Normalizes an axios error to the backend ApiErrorResponse shape. */
export function toApiError(err: unknown): ApiErrorResponse {
  const axiosErr = err as AxiosError<ApiErrorResponse>;
  if (axiosErr.response?.data && typeof axiosErr.response.data === 'object') {
    return axiosErr.response.data;
  }
  return {
    status: axiosErr.response?.status ?? 0,
    error: 'Network error',
    message: axiosErr.message ?? 'Something went wrong',
  };
}

export type FieldErrors = Record<string, string>;
