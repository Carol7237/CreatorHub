import axios, { AxiosError } from 'axios';
import type { ApiErrorResponse } from '../types';
import { clearToken, getToken } from '../auth/token';

/**
 * Axios instance. In dev, /api is proxied to the API gateway (:8085 — see
 * vite.config.ts). Authentication is stateless JWT: the access token travels in the
 * Authorization header, so there are no cookies and no CSRF (withCredentials is not
 * needed).
 */
export const api = axios.create({
  baseURL: '/',
});

// Attach the JWT as a Bearer token on every request, when one is stored.
api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

// On a 401 WITH a stored token, the token is invalid/expired (there is no refresh
// token): clear it and send the user back to login to re-authenticate. A 401 WITHOUT
// a token (e.g. a wrong-password login attempt) is left for the caller to handle.
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401 && getToken()) {
      clearToken();
      if (window.location.pathname !== '/login') {
        window.location.assign('/login');
      }
    }
    return Promise.reject(error);
  },
);

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
