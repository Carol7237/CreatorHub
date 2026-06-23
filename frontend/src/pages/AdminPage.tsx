import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { useState } from 'react';
import { adminApi } from '../api/endpoints';
import { toApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Page } from '../components/Page';
import { Avatar, RoleBadge, Spinner, Alert, listContainer, listItem } from '../components/ui';

export function AdminPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [feedback, setFeedback] = useState<{ kind: 'error' | 'success'; msg: string } | null>(null);
  const users = useQuery({ queryKey: ['admin-users'], queryFn: () => adminApi.users() });

  const remove = useMutation({
    mutationFn: (id: number) => adminApi.deleteUser(id),
    onSuccess: () => { setFeedback({ kind: 'success', msg: 'User deleted.' }); qc.invalidateQueries({ queryKey: ['admin-users'] }); },
    onError: (e) => setFeedback({ kind: 'error', msg: toApiError(e).message || 'Could not delete user' }),
  });

  return (
    <Page>
      <div className="row" style={{ gap: '0.7rem', marginBottom: '0.3rem' }}>
        <h1>Admin <span className="gradient-text">console</span></h1>
        <RoleBadge role="ADMIN" />
      </div>
      <p className="muted" style={{ marginBottom: '1.6rem' }}>Manage all accounts on the platform.</p>
      {feedback && <div style={{ marginBottom: '1.2rem' }}><Alert kind={feedback.kind}>{feedback.msg}</Alert></div>}

      {users.isLoading ? <Spinner /> : (
        <motion.div className="stack" variants={listContainer} initial="hidden" animate="show">
          {users.data?.map((u) => (
            <motion.div key={u.id} className="card between" variants={listItem}>
              <div className="row" style={{ gap: '0.7rem' }}>
                <Avatar name={u.displayName ?? u.username} />
                <div>
                  <div className="row" style={{ gap: '0.5rem' }}>
                    <span style={{ fontWeight: 600, color: 'var(--text)' }}>{u.displayName ?? u.username}</span>
                    {u.role === 'ADMIN' && <RoleBadge role="ADMIN" />}
                  </div>
                  <div className="muted" style={{ fontSize: '0.82rem' }}>@{u.username} · {u.email}</div>
                </div>
              </div>
              {u.id !== user?.id && (
                <button className="btn btn-danger btn-sm" onClick={() => { if (confirm(`Delete @${u.username}?`)) remove.mutate(u.id); }}>
                  Delete
                </button>
              )}
            </motion.div>
          ))}
        </motion.div>
      )}
    </Page>
  );
}
