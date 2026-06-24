import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { authApi, type LoginPayload, type RegisterPayload } from '../api/endpoints';
import { clearToken, getToken, setToken } from './token';
import type { UserResponse } from '../types';

interface AuthState {
  user: UserResponse | null;
  loading: boolean;
  isAdmin: boolean;
  login: (p: LoginPayload) => Promise<void>;
  register: (p: RegisterPayload) => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const refresh = async () => {
    try {
      setUser(await authApi.me());
    } catch {
      setUser(null);
    }
  };

  useEffect(() => {
    // Restore the session on load only if a JWT is stored; /me confirms it (or the
    // 401 interceptor clears an expired token). No token -> stay anonymous.
    if (!getToken()) {
      setUser(null);
      setLoading(false);
      return;
    }
    refresh().finally(() => setLoading(false));
  }, []);

  const login = async (p: LoginPayload) => {
    const res = await authApi.login(p);
    setToken(res.token);
    setUser(res.user);
  };

  const register = async (p: RegisterPayload) => {
    await authApi.register(p);
    await login({ username: p.username, password: p.password });
  };

  const logout = async () => {
    // Stateless JWT: logout is client-side — drop the token and clear the user.
    clearToken();
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{ user, loading, isAdmin: user?.role === 'ADMIN', login, register, logout, refresh }}
    >
      {children}
    </AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
