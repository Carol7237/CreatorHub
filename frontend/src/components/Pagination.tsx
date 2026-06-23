export function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <div className="row" style={{ justifyContent: 'center', gap: '0.5rem', marginTop: '2rem' }}>
      <button className="btn btn-ghost btn-sm" disabled={page === 0} onClick={() => onChange(page - 1)}>
        ← Prev
      </button>
      <span className="muted" style={{ fontFamily: 'var(--font-display)', padding: '0 0.5rem' }}>
        Page {page + 1} of {totalPages}
      </span>
      <button className="btn btn-ghost btn-sm" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
        Next →
      </button>
    </div>
  );
}
