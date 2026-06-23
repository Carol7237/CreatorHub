import { motion } from 'framer-motion';
import type { ReactNode } from 'react';

/** Page-entrance transition wrapper. */
export function Page({ children, narrow = false }: { children: ReactNode; narrow?: boolean }) {
  return (
    <motion.div
      className="page"
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -12 }}
      transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
    >
      <div className="container" style={narrow ? { maxWidth: 560 } : undefined}>
        {children}
      </div>
    </motion.div>
  );
}
