import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useState } from 'react';
import type { PostResponse } from '../types';
import { Avatar, FreeBadge, PremiumBadge } from './ui';
import { listItem } from './ui';
import './postcard.css';

function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const m = Math.floor(diff / 60000);
  if (m < 1) return 'just now';
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  return `${Math.floor(h / 24)}d ago`;
}

export function PostCard({ post }: { post: PostResponse }) {
  const navigate = useNavigate();
  const [liked, setLiked] = useState(false);
  const locked = post.premium && post.locked;

  return (
    <motion.article
      className="post-card card card-hover"
      variants={listItem}
      onClick={() => navigate(`/posts/${post.id}`)}
      role="link"
      tabIndex={0}
      onKeyDown={(e) => e.key === 'Enter' && navigate(`/posts/${post.id}`)}
    >
      <header className="post-head" onClick={(e) => e.stopPropagation()}>
        <Link to={`/creators/${post.creatorId}`} className="post-author">
          <Avatar name={post.creatorUsername} />
          <div>
            <div className="post-author-name">@{post.creatorUsername}</div>
            <div className="muted post-time">{timeAgo(post.createdAt)}</div>
          </div>
        </Link>
        {post.premium ? <PremiumBadge /> : <FreeBadge />}
      </header>

      <h3 className="post-title">{post.title}</h3>

      {locked ? (
        <div className="locked-zone">
          <div className="locked-blur" aria-hidden>
            ▓▓▓▓▓ ▓▓▓▓ ▓▓▓▓▓▓ ▓▓▓ ▓▓▓▓▓▓▓ ▓▓ ▓▓▓▓ ▓▓▓▓▓ ▓▓▓▓▓▓
          </div>
          <div className="lock-overlay">
            <motion.div
              className="lock-orb"
              whileHover={{ scale: 1.1, rotate: -6 }}
              transition={{ type: 'spring', stiffness: 300 }}
            >
              🔒
            </motion.div>
            <div className="lock-title">Exclusive content</div>
            <div className="muted lock-sub">
              {post.tierName ? `Unlock with the “${post.tierName}” tier` : 'Subscribe to unlock'}
            </div>
            <button
              className="btn btn-sm unlock-btn"
              onClick={(e) => { e.stopPropagation(); navigate(`/creators/${post.creatorId}`); }}
            >
              ✦ Unlock
            </button>
          </div>
        </div>
      ) : (
        <p className="post-body">{post.body}</p>
      )}

      <footer className="post-foot" onClick={(e) => e.stopPropagation()}>
        <div className="tags">
          {post.tags.slice(0, 3).map((t) => (
            <span key={t} className="tag">#{t}</span>
          ))}
        </div>
        <button
          className={`like-btn ${liked ? 'liked' : ''}`}
          onClick={() => setLiked((l) => !l)}
          aria-pressed={liked}
          aria-label="Like"
        >
          <motion.span
            key={liked ? 'on' : 'off'}
            initial={{ scale: 0.6 }}
            animate={{ scale: liked ? [1, 1.5, 1] : 1 }}
            transition={{ duration: 0.35 }}
          >
            {liked ? '♥' : '♡'}
          </motion.span>
        </button>
      </footer>
    </motion.article>
  );
}
