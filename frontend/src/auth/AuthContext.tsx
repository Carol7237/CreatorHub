import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { authApi, type LoginPayload, type RegisterPayload } from '../api/endpoints';
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
    refresh().finally(() => setLoading(false));
  }, []);

  const login = async (p: LoginPayload) => {
    const u = await authApi.login(p);
    setUser(u);
  };

  const register = async (p: RegisterPayload) => {
    await authApi.register(p);
    await login({ username: p.username, password: p.password });
  };

  const logout = async () => {
    await authApi.logout();
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
