import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from '../auth/AuthContext';
import { Spinner, EmptyState } from './ui';

export function ProtectedRoute({ children, requireAdmin = false }: { children: ReactNode; requireAdmin?: boolean }) {
  const { user, loading, isAdmin } = useAuth();
  const location = useLocation();

  if (loading) return <Spinner />;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (requireAdmin && !isAdmin) {
    return (
      <div className="container page">
        <EmptyState icon="⛔" title="No access" hint="This area is for administrators only." />
      </div>
    );
  }
  return <>{children}</>;
}
