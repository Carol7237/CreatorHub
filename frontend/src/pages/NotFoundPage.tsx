import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Page } from '../components/Page';

export function NotFoundPage() {
  return (
    <Page>
      <motion.div
        className="center stack"
        style={{ alignItems: 'center', padding: '4rem 1rem', gap: '0.6rem' }}
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
      >
        <motion.div
          className="gradient-text"
          style={{ fontFamily: 'var(--font-display)', fontSize: 'clamp(4rem, 14vw, 8rem)', fontWeight: 700, lineHeight: 1 }}
          animate={{ y: [0, -10, 0] }}
          transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut' }}
        >
          404
        </motion.div>
        <h2>Lost in the dark</h2>
        <p className="muted" style={{ maxWidth: 380 }}>
          The page you’re looking for has slipped into the night. Let’s get you back.
        </p>
        <Link to="/" className="btn" style={{ marginTop: '0.6rem' }}>← Back to the feed</Link>
      </motion.div>
    </Page>
  );
}
