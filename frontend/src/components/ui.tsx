import { motion } from 'framer-motion';
import { useEffect, useRef, useState, type ReactNode } from 'react';

export function Avatar({ name, size = 'md' }: { name?: string | null; size?: 'md' | 'lg' }) {
  const initials = (name ?? '?').trim().slice(0, 2) || '?';
  return <div className={`avatar ${size === 'lg' ? 'avatar-lg' : ''}`}>{initials}</div>;
}

export function PremiumBadge() {
  return <span className="badge badge-premium">🔒 Premium</span>;
}
export function FreeBadge() {
  return <span className="badge badge-free">● Free</span>;
}
export function RoleBadge({ role }: { role: string }) {
  return <span className="badge badge-role">{role}</span>;
}

export function Spinner() {
  return (
    <div style={{ display: 'grid', placeItems: 'center', padding: '3rem' }}>
      <div className="spinner" aria-label="Loading" />
    </div>
  );
}

export function Skeleton({ height = 16, width = '100%', radius = 8 }: { height?: number; width?: string | number; radius?: number }) {
  return <div className="skeleton" style={{ height, width, borderRadius: radius }} />;
}

export function SkeletonCard() {
  return (
    <div className="card stack">
      <div className="row">
        <Skeleton height={44} width={44} radius={999} />
        <div className="stack" style={{ flex: 1, gap: '0.4rem' }}>
          <Skeleton height={12} width="40%" />
          <Skeleton height={10} width="25%" />
        </div>
      </div>
      <Skeleton height={18} width="70%" />
      <Skeleton height={64} />
    </div>
  );
}

export function EmptyState({ icon = '✦', title, hint, action }: { icon?: string; title: string; hint?: string; action?: ReactNode }) {
  return (
    <motion.div
      className="card center stack"
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      style={{ alignItems: 'center', padding: '3rem 1.5rem' }}
    >
      <div style={{ fontSize: '2.5rem' }} className="gradient-text">{icon}</div>
      <h3>{title}</h3>
      {hint && <p className="muted" style={{ maxWidth: 360 }}>{hint}</p>}
      {action}
    </motion.div>
  );
}

export function Alert({ kind, children }: { kind: 'error' | 'success'; children: ReactNode }) {
  return (
    <motion.div
      className={`alert alert-${kind}`}
      initial={{ opacity: 0, y: -6 }}
      animate={{ opacity: 1, y: 0 }}
      role={kind === 'error' ? 'alert' : 'status'}
    >
      {children}
    </motion.div>
  );
}

/** Count-up animation for stat numbers. */
export function CountUp({ value, duration = 1100, prefix = '' }: { value: number; duration?: number; prefix?: string }) {
  const [display, setDisplay] = useState(0);
  const ref = useRef<number>(0);
  useEffect(() => {
    const start = performance.now();
    const from = ref.current;
    let raf = 0;
    const tick = (now: number) => {
      const t = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - t, 3);
      setDisplay(Math.round(from + (value - from) * eased));
      if (t < 1) raf = requestAnimationFrame(tick);
      else ref.current = value;
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [value, duration]);
  return <>{prefix}{display.toLocaleString()}</>;
}

/** Staggered fade-in container + item for lists. */
export const listContainer = {
  hidden: {},
  show: { transition: { staggerChildren: 0.06 } },
};
export const listItem = {
  hidden: { opacity: 0, y: 16 },
  show: { opacity: 1, y: 0, transition: { duration: 0.4, ease: [0.22, 1, 0.36, 1] as const } },
};
