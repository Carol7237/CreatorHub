import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { postApi, creatorApi } from '../api/endpoints';
import { PostCard } from '../components/PostCard';
import { Pagination } from '../components/Pagination';
import { CountUp, EmptyState, SkeletonCard, listContainer } from '../components/ui';
import { useAuth } from '../auth/AuthContext';
import './feed.css';

const SORTS = [
  { value: 'createdAt,desc', label: 'Newest' },
  { value: 'createdAt,asc', label: 'Oldest' },
  { value: 'title,asc', label: 'Title A–Z' },
  { value: 'title,desc', label: 'Title Z–A' },
];

export function FeedPage() {
  const { user } = useAuth();
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState('createdAt,desc');

  const posts = useQuery({ queryKey: ['posts', page, sort], queryFn: () => postApi.list(page, 9, sort) });
  const creators = useQuery({ queryKey: ['creators-count'], queryFn: () => creatorApi.list(0, 1) });

  return (
    <motion.div className="page" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
      <div className="container">
        {/* hero */}
        <section className="hero">
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }}>
            <span className="hero-kicker">✦ Night-owl creator platform</span>
            <h1 className="hero-title">
              Where night owls <span className="gradient-text">create</span>
            </h1>
            <p className="hero-sub">
              Publish free and premium drops. Build paid tiers. Unlock the work of the
              creators you love — all after dark.
            </p>
            <div className="hero-cta">
              {user ? (
                <Link to="/posts/new" className="btn">⚡ Create a post</Link>
              ) : (
                <Link to="/register" className="btn">⚡ Start creating</Link>
              )}
              <Link to="/creators" className="btn btn-ghost">Explore creators</Link>
            </div>
          </motion.div>

          <div className="stats">
            <motion.div className="stat-card" initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} transition={{ delay: 0.15 }}>
              <div className="stat-num gradient-text"><CountUp value={posts.data?.totalElements ?? 0} /></div>
              <div className="stat-label">Posts</div>
            </motion.div>
            <motion.div className="stat-card" initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} transition={{ delay: 0.25 }}>
              <div className="stat-num gradient-text"><CountUp value={creators.data?.totalElements ?? 0} /></div>
              <div className="stat-label">Creators</div>
            </motion.div>
          </div>
        </section>

        {/* feed controls */}
        <div className="between feed-bar">
          <h2>The feed</h2>
          <label className="row" style={{ gap: '0.5rem' }}>
            <span className="muted" style={{ fontSize: '0.85rem' }}>Sort</span>
            <select className="select" style={{ width: 'auto' }} value={sort} onChange={(e) => { setSort(e.target.value); setPage(0); }}>
              {SORTS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
            </select>
          </label>
        </div>

        {/* feed grid */}
        {posts.isLoading ? (
          <div className="post-grid">{Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} />)}</div>
        ) : posts.isError ? (
          <EmptyState icon="⚠" title="Couldn’t load the feed" hint="Please try again in a moment." />
        ) : posts.data && posts.data.content.length > 0 ? (
          <>
            <motion.div className="post-grid" variants={listContainer} initial="hidden" animate="show">
              {posts.data.content.map((p) => <PostCard key={p.id} post={p} />)}
            </motion.div>
            <Pagination page={posts.data.page} totalPages={posts.data.totalPages} onChange={setPage} />
          </>
        ) : (
          <EmptyState
            icon="🌙"
            title="The night is quiet"
            hint="No posts yet. Be the first to publish something."
            action={user ? <Link to="/posts/new" className="btn btn-sm">Create a post</Link> : <Link to="/register" className="btn btn-sm">Join to post</Link>}
          />
        )}
      </div>
    </motion.div>
  );
}
