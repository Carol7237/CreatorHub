import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { postApi, commentApi } from '../api/endpoints';
import { toApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Page } from '../components/Page';
import { Avatar, FreeBadge, PremiumBadge, Spinner, EmptyState, Alert, listContainer, listItem } from '../components/ui';
import './postdetail.css';

export function PostDetailPage() {
  const { id } = useParams();
  const postId = Number(id);
  const { user, isAdmin } = useAuth();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [commentText, setCommentText] = useState('');
  const [commentError, setCommentError] = useState<string | null>(null);

  const post = useQuery({ queryKey: ['post', postId], queryFn: () => postApi.get(postId) });
  const comments = useQuery({ queryKey: ['comments', postId], queryFn: () => postApi.comments(postId) });

  const addComment = useMutation({
    mutationFn: () => commentApi.create(postId, commentText),
    onSuccess: () => { setCommentText(''); setCommentError(null); qc.invalidateQueries({ queryKey: ['comments', postId] }); },
    onError: (e) => setCommentError(toApiError(e).message || 'Could not post comment'),
  });

  const removePost = useMutation({
    mutationFn: () => postApi.remove(postId),
    onSuccess: () => navigate('/'),
  });

  if (post.isLoading) return <Page><Spinner /></Page>;
  if (post.isError || !post.data) {
    return <Page><EmptyState icon="🔍" title="Post not found" hint="This post may have been removed." action={<Link to="/" className="btn btn-sm">Back to feed</Link>} /></Page>;
  }

  const p = post.data;
  const locked = p.premium && p.locked;
  const isOwner = user?.id === p.creatorId;
  const canComment = user && !locked;

  return (
    <Page>
      <article className="card post-detail">
        <header className="between">
          <Link to={`/creators/${p.creatorId}`} className="row" style={{ gap: '0.7rem' }}>
            <Avatar name={p.creatorUsername ?? `creator-${p.creatorId}`} />
            <div>
              <div style={{ fontWeight: 600, color: 'var(--text)' }}>@{p.creatorUsername ?? `creator-${p.creatorId}`}</div>
              <div className="muted" style={{ fontSize: '0.8rem' }}>{new Date(p.createdAt).toLocaleString()}</div>
            </div>
          </Link>
          {p.premium ? <PremiumBadge /> : <FreeBadge />}
        </header>

        <h1 className="detail-title">{p.title}</h1>

        {locked ? (
          <motion.div className="detail-lock" initial={{ opacity: 0, scale: 0.96 }} animate={{ opacity: 1, scale: 1 }}>
            <div className="lock-orb">🔒</div>
            <h3>Exclusive content</h3>
            <p className="muted">
              {p.tierName ? `This drop is for “${p.tierName}” subscribers.` : 'Subscribe to this creator to unlock.'}
            </p>
            <button className="btn" onClick={() => navigate(`/creators/${p.creatorId}`)}>✦ Unlock with a subscription</button>
          </motion.div>
        ) : (
          <p className="detail-body">{p.body}</p>
        )}

        <div className="tags" style={{ marginTop: '1rem' }}>
          {p.tags.map((t) => <span key={t} className="tag">#{t}</span>)}
        </div>

        {(isOwner || isAdmin) && (
          <div className="row" style={{ marginTop: '1.3rem', gap: '0.6rem' }}>
            {isOwner && <Link to={`/posts/${p.id}/edit`} className="btn btn-ghost btn-sm">Edit</Link>}
            <button className="btn btn-danger btn-sm" onClick={() => { if (confirm('Delete this post?')) removePost.mutate(); }}>
              Delete
            </button>
          </div>
        )}
      </article>

      {/* comments */}
      <section className="comments">
        <h2 className="comments-title">Comments {comments.data ? `(${comments.data.length})` : ''}</h2>

        {canComment ? (
          <div className="comment-form card">
            {commentError && <div style={{ marginBottom: '0.7rem' }}><Alert kind="error">{commentError}</Alert></div>}
            <textarea
              className="textarea" placeholder="Add a comment…" value={commentText} maxLength={1000}
              onChange={(e) => setCommentText(e.target.value)} style={{ minHeight: 80 }}
            />
            <div className="between" style={{ marginTop: '0.6rem' }}>
              <span className="muted" style={{ fontSize: '0.78rem' }}>{commentText.length}/1000</span>
              <button className="btn btn-sm" disabled={!commentText.trim() || addComment.isPending} onClick={() => addComment.mutate()}>
                {addComment.isPending ? 'Posting…' : 'Post comment'}
              </button>
            </div>
          </div>
        ) : (
          <div className="card center muted" style={{ padding: '1.2rem' }}>
            {!user ? <>You need to <Link to="/login">sign in</Link> to comment.</>
              : locked ? 'Only active subscribers can comment on this premium post.'
              : 'Comments are closed.'}
          </div>
        )}

        {comments.isLoading ? <Spinner /> : comments.data && comments.data.length > 0 ? (
          <motion.div className="comment-list" variants={listContainer} initial="hidden" animate="show">
            {comments.data.map((c) => (
              <motion.div key={c.id} className="comment card" variants={listItem}>
                <div className="row" style={{ gap: '0.6rem' }}>
                  <Avatar name={c.authorUsername ?? `user-${c.authorId}`} />
                  <div style={{ flex: 1 }}>
                    <div className="between">
                      <span style={{ fontWeight: 600, color: 'var(--text)' }}>@{c.authorUsername ?? `user-${c.authorId}`}</span>
                      <span className="muted" style={{ fontSize: '0.75rem' }}>{new Date(c.createdAt).toLocaleDateString()}</span>
                    </div>
                    <p style={{ marginTop: '0.2rem' }}>{c.text}</p>
                  </div>
                </div>
              </motion.div>
            ))}
          </motion.div>
        ) : (
          <p className="muted center" style={{ padding: '1.5rem' }}>No comments yet. Start the conversation.</p>
        )}
      </section>
    </Page>
  );
}
