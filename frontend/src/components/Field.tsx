import type { ReactNode } from 'react';

/** Labeled form field with an optional per-field error message. */
export function Field({
  label,
  error,
  htmlFor,
  children,
  hint,
}: {
  label: string;
  error?: string;
  htmlFor?: string;
  children: ReactNode;
  hint?: string;
}) {
  return (
    <div className="field">
      <label htmlFor={htmlFor}>{label}</label>
      {children}
      {hint && !error && <span className="muted" style={{ fontSize: '0.78rem' }}>{hint}</span>}
      {error && <span className="field-error">⚠ {error}</span>}
    </div>
  );
}
