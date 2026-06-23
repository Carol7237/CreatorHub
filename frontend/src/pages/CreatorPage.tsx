import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { creatorApi, profileApi, subscriptionApi } from '../api/endpoints';
import { toApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Page } from '../components/Page';
import { PostCard } from '../components/PostCard';
import { Pagination } from '../components/Pagination';
import { Avatar, RoleBadge, Spinner, EmptyState, Alert, listContainer, listItem, SkeletonCard } from '../components/ui';
import './creator.css';

function CreatorsList() {
  const [page, setPage] = useState(0);
  const creators = useQuery({ queryKey: ['creators', page], queryFn: () => creatorApi.list(page, 12) });
  return (
    <Page>
      <h1 style={{ marginBottom: '0.4rem' }}>Discover <span className="gradient-text">creators</span></h1>
      <p className="muted" style={{ marginBottom: '1.6rem' }}>Find your next favourite night-owl.</p>
      {creators.isLoading ? (
        <div className="creator-grid">{Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} />)}</div>
      ) : creators.data && creators.data.content.length > 0 ? (
        <>
          <motion.div className="creator-grid" variants={listContainer} initial="hidden" animate="show">
            {creators.data.content.map((c) => (
              <motion.div key={c.id} variants={listItem}>
                <Link to={`/creators/${c.id}`} className="creator-tile card card-hover">
                  <Avatar name={c.displayName ?? c.username} size="lg" />
                  <div className="creator-tile-name">{c.displayName ?? c.username}</div>
                  <div className="muted">@{c.username}</div>
                  {c.role === 'ADMIN' && <RoleBadge role="ADMIN" />}
                </Link>
              </motion.div>
            ))}
          </motion.div>
          <Pagination page={creators.data.page} totalPages={creators.data.totalPages} onChange={setPage} />
        </>
      ) : (
        <EmptyState icon="🌙" title="No creators yet" />
      )}
    </Page>
  );
}

export function CreatorPage({ list = false }: { list?: boolean }) {
  if (list) return <CreatorsList />;
  return <CreatorProfile />;
}

function CreatorProfile() {
  const { id } = useParams();
  const creatorId = Number(id);
  const { user } = useAuth();
  const qc = useQueryClient();
  const [feedback, setFeedback] = useState<{ kind: 'error' | 'success'; msg: string } | null>(null);
  const [postsPage, setPostsPage] = useState(0);

  const creator = useQuery({ queryKey: ['creator', creatorId], queryFn: () => creatorApi.get(creatorId) });
  const profile = useQuery({ queryKey: ['profile-user', creatorId], queryFn: () => profileApi.byUser(creatorId) });
  const tiers = useQuery({ queryKey: ['creator-tiers', creatorId], queryFn: () => creatorApi.tiers(creatorId) });
  const posts = useQuery({ queryKey: ['creator-posts', creatorId, postsPage], queryFn: () => creatorApi.posts(creatorId, postsPage, 9) });

  const subscribe = useMutation({
    mutationFn: (tierId: number) => subscriptionApi.subscribe(tierId),
    onSuccess: () => {
      setFeedback({ kind: 'success', msg: 'Subscribed! Premium content is now unlocked.' });
      qc.invalidateQueries({ queryKey: ['creator-posts', creatorId] });
    },
    onError: (e) => setFeedback({ kind: 'error', msg: toApiError(e).message || 'Could not subscribe' }),
  });

  if (creator.isLoading) return <Page><Spinner /></Page>;
  if (creator.isError || !creator.data) {
    return <Page><EmptyState icon="🔍" title="Creator not found" action={<Link to="/creators" className="btn btn-sm">Browse creators</Link>} /></Page>;
  }

  const c = creator.data;
  const isOwner = user?.id === c.id;

  return (
    <Page>
      <motion.section className="creator-hero card" initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}>
        <Avatar name={profile.data?.displayName ?? c.username} size="lg" />
        <div className="creator-hero-info">
          <div className="row" style={{ gap: '0.6rem' }}>
            <h1>{profile.data?.displayName ?? c.displayName ?? c.username}</h1>
            {c.role === 'ADMIN' && <RoleBadge role="ADMIN" />}
          </div>
          <div className="muted">@{c.username}</div>
          {profile.data?.bio && <p className="creator-bio">{profile.data.bio}</p>}
          {isOwner && (
            <div className="row" style={{ gap: '0.6rem', marginTop: '0.7rem' }}>
              <Link to="/profile/edit" className="btn btn-ghost btn-sm">Edit profile</Link>
              <Link to="/tiers" className="btn btn-ghost btn-sm">Manage tiers</Link>
            </div>
          )}
        </div>
      </motion.section>

      {feedback && <div style={{ margin: '1.2rem 0' }}><Alert kind={feedback.kind}>{feedback.msg}</Alert></div>}

      {/* tiers */}
      <h2 className="section-title">Subscription tiers</h2>
      {tiers.data && tiers.data.length > 0 ? (
        <motion.div className="tier-grid" variants={listContainer} initial="hidden" animate="show">
          {tiers.data.map((t) => (
            <motion.div key={t.id} className="tier-card card card-hover" variants={listItem}>
              <div className="tier-name">{t.name}</div>
              <div className="tier-price"><span className="gradient-text">${t.priceMonthly.toFixed(2)}</span><span className="muted">/mo</span></div>
              {t.perks && <p className="muted tier-perks">{t.perks}</p>}
              {!isOwner && user && (
                <button className="btn btn-sm btn-block" disabled={subscribe.isPending} onClick={() => subscribe.mutate(t.id)}>
                  ✦ Subscribe
                </button>
              )}
              {!user && <Link to="/login" className="btn btn-ghost btn-sm btn-block">Sign in to subscribe</Link>}
            </motion.div>
          ))}
        </motion.div>
      ) : (
        <p className="muted">This creator hasn’t set up any tiers yet.</p>
      )}

      {/* posts */}
      <h2 className="section-title" style={{ marginTop: '2.2rem' }}>Posts</h2>
      {posts.isLoading ? (
        <div className="post-grid">{Array.from({ length: 3 }).map((_, i) => <SkeletonCard key={i} />)}</div>
      ) : posts.data && posts.data.content.length > 0 ? (
        <>
          <motion.div className="post-grid" variants={listContainer} initial="hidden" animate="show">
            {posts.data.content.map((p) => <PostCard key={p.id} post={p} />)}
          </motion.div>
          <Pagination page={posts.data.page} totalPages={posts.data.totalPages} onChange={setPostsPage} />
        </>
      ) : (
        <p className="muted">No posts yet.</p>
      )}
    </Page>
  );
}
