import { useForm } from 'react-hook-form';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { motion } from 'framer-motion';
import { creatorApi, tierApi, type TierPayload } from '../api/endpoints';
import { toApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Page } from '../components/Page';
import { Field } from '../components/Field';
import { Alert, Spinner, EmptyState, listContainer, listItem } from '../components/ui';

interface FormValues { name: string; priceMonthly: string; perks: string; }

export function TiersPage() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [editId, setEditId] = useState<number | null>(null);
  const [feedback, setFeedback] = useState<{ kind: 'error' | 'success'; msg: string } | null>(null);

  const tiers = useQuery({ queryKey: ['my-tiers', user!.id], queryFn: () => creatorApi.tiers(user!.id) });
  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm<FormValues>({
    defaultValues: { name: '', priceMonthly: '', perks: '' },
  });

  const save = useMutation({
    mutationFn: (p: TierPayload) => (editId ? tierApi.update(editId, p) : tierApi.create(p)),
    onSuccess: () => {
      setFeedback({ kind: 'success', msg: editId ? 'Tier updated.' : 'Tier created.' });
      setEditId(null); reset({ name: '', priceMonthly: '', perks: '' });
      qc.invalidateQueries({ queryKey: ['my-tiers', user!.id] });
    },
    onError: (e) => {
      const err = toApiError(e);
      if (err.fieldErrors) Object.entries(err.fieldErrors).forEach(([f, m]) => {
        if (['name', 'priceMonthly'].includes(f)) setError(f as keyof FormValues, { message: m });
      });
      else setFeedback({ kind: 'error', msg: err.message || 'Could not save tier' });
    },
  });

  const remove = useMutation({
    mutationFn: (id: number) => tierApi.remove(id),
    onSuccess: () => { setFeedback({ kind: 'success', msg: 'Tier deleted.' }); qc.invalidateQueries({ queryKey: ['my-tiers', user!.id] }); },
    onError: (e) => setFeedback({ kind: 'error', msg: toApiError(e).message || 'Could not delete tier' }),
  });

  const onSubmit = (v: FormValues) => save.mutate({ name: v.name, priceMonthly: Number(v.priceMonthly), perks: v.perks || undefined });

  return (
    <Page>
      <h1 style={{ marginBottom: '0.3rem' }}>Your <span className="gradient-text">tiers</span></h1>
      <p className="muted" style={{ marginBottom: '1.6rem' }}>Set up the paid tiers fans can subscribe to.</p>
      {feedback && <div style={{ marginBottom: '1.2rem' }}><Alert kind={feedback.kind}>{feedback.msg}</Alert></div>}

      <div className="tiers-layout">
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="card" style={{ alignSelf: 'start' }}>
          <h3 style={{ marginBottom: '1rem' }}>{editId ? 'Edit tier' : 'New tier'}</h3>
          <Field label="Name" htmlFor="name" error={errors.name?.message}>
            <input id="name" className={`input ${errors.name ? 'invalid' : ''}`} placeholder="VIP"
              {...register('name', { required: 'Name is required', maxLength: { value: 100, message: 'At most 100 characters' } })} />
          </Field>
          <Field label="Monthly price (USD)" htmlFor="price" error={errors.priceMonthly?.message}>
            <input id="price" type="number" step="0.01" min="0.01" className={`input ${errors.priceMonthly ? 'invalid' : ''}`} placeholder="9.99"
              {...register('priceMonthly', {
                required: 'Price is required',
                validate: (val) => Number(val) >= 0.01 || 'Price must be greater than 0',
              })} />
          </Field>
          <Field label="Perks (optional)" htmlFor="perks">
            <textarea id="perks" className="textarea" placeholder="What do subscribers get?" {...register('perks')} style={{ minHeight: 80 }} />
          </Field>
          <div className="row" style={{ gap: '0.6rem' }}>
            <button className="btn btn-block" type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Saving…' : editId ? 'Save' : '✦ Create tier'}
            </button>
            {editId && <button type="button" className="btn btn-ghost" onClick={() => { setEditId(null); reset({ name: '', priceMonthly: '', perks: '' }); }}>Cancel</button>}
          </div>
        </form>

        <div>
          {tiers.isLoading ? <Spinner /> : tiers.data && tiers.data.length > 0 ? (
            <motion.div className="stack" variants={listContainer} initial="hidden" animate="show">
              {tiers.data.map((t) => (
                <motion.div key={t.id} className="card between" variants={listItem}>
                  <div>
                    <div style={{ fontFamily: 'var(--font-display)', fontWeight: 600, color: 'var(--text)' }}>{t.name}</div>
                    <div className="gradient-text" style={{ fontFamily: 'var(--font-display)', fontSize: '1.2rem' }}>${t.priceMonthly.toFixed(2)}/mo</div>
                    {t.perks && <p className="muted" style={{ fontSize: '0.85rem', marginTop: '0.3rem' }}>{t.perks}</p>}
                  </div>
                  <div className="row" style={{ gap: '0.5rem' }}>
                    <button className="btn btn-ghost btn-sm" onClick={() => { setEditId(t.id); reset({ name: t.name, priceMonthly: String(t.priceMonthly), perks: t.perks ?? '' }); }}>Edit</button>
                    <button className="btn btn-danger btn-sm" onClick={() => { if (confirm(`Delete tier “${t.name}”?`)) remove.mutate(t.id); }}>Delete</button>
                  </div>
                </motion.div>
              ))}
            </motion.div>
          ) : (
            <EmptyState icon="✦" title="No tiers yet" hint="Create your first tier to start earning." />
          )}
        </div>
      </div>
    </Page>
  );
}
