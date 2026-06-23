import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useState } from 'react';
import { subscriptionApi } from '../api/endpoints';
import { toApiError } from '../api/client';
import { Page } from '../components/Page';
import { Spinner, EmptyState, Alert, listContainer, listItem } from '../components/ui';

function StatusBadge({ status }: { status: string }) {
  const map: Record<string, string> = { ACTIVE: 'badge-free', CANCELLED: 'badge-role', EXPIRED: 'badge-role' };
  return <span className={`badge ${map[status] ?? 'badge-role'}`}>{status}</span>;
}

export function SubscriptionsPage() {
  const qc = useQueryClient();
  const [feedback, setFeedback] = useState<string | null>(null);
  const subs = useQuery({ queryKey: ['my-subs'], queryFn: () => subscriptionApi.mine() });

  const cancel = useMutation({
    mutationFn: (subId: number) => subscriptionApi.cancel(subId),
    onSuccess: () => { setFeedback('Subscription cancelled.'); qc.invalidateQueries({ queryKey: ['my-subs'] }); },
    onError: (e) => setFeedback(toApiError(e).message),
  });

  return (
    <Page>
      <h1 style={{ marginBottom: '0.3rem' }}>My <span className="gradient-text">subscriptions</span></h1>
      <p className="muted" style={{ marginBottom: '1.6rem' }}>The creators you support.</p>
      {feedback && <div style={{ marginBottom: '1.2rem' }}><Alert kind="success">{feedback}</Alert></div>}

      {subs.isLoading ? <Spinner /> : subs.data && subs.data.length > 0 ? (
        <motion.div className="stack" variants={listContainer} initial="hidden" animate="show">
          {subs.data.map((s) => (
            <motion.div key={s.id} className="card between" variants={listItem}>
              <div>
                <div className="row" style={{ gap: '0.6rem' }}>
                  <span style={{ fontFamily: 'var(--font-display)', fontWeight: 600, color: 'var(--text)' }}>{s.tierName}</span>
                  <StatusBadge status={s.status} />
                </div>
                <div className="muted" style={{ fontSize: '0.85rem', marginTop: '0.2rem' }}>
                  to <Link to={`/creators/${s.creatorId}`}>creator #{s.creatorId}</Link> · since {new Date(s.startDate).toLocaleDateString()}
                </div>
              </div>
              {s.status === 'ACTIVE' && (
                <button className="btn btn-ghost btn-sm" disabled={cancel.isPending} onClick={() => { if (confirm('Cancel this subscription?')) cancel.mutate(s.id); }}>
                  Cancel
                </button>
              )}
            </motion.div>
          ))}
        </motion.div>
      ) : (
        <EmptyState icon="✦" title="No subscriptions yet" hint="Find a creator and unlock their premium drops." action={<Link to="/creators" className="btn btn-sm">Explore creators</Link>} />
      )}
    </Page>
  );
}
