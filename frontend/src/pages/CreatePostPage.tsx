import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { postApi, creatorApi, type PostPayload } from '../api/endpoints';
import { toApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Page } from '../components/Page';
import { Field } from '../components/Field';
import { Alert, Spinner } from '../components/ui';

interface FormValues { title: string; body: string; premium: boolean; tierId: string; tags: string; }

export function CreatePostPage() {
  const { user } = useAuth();
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);

  const tiers = useQuery({ queryKey: ['my-tiers', user?.id], queryFn: () => creatorApi.tiers(user!.id), enabled: !!user });
  const existing = useQuery({ queryKey: ['post', Number(id)], queryFn: () => postApi.get(Number(id)), enabled: editing });

  const { register, handleSubmit, watch, reset, setError, formState: { errors, isSubmitting } } = useForm<FormValues>({
    defaultValues: { premium: false, tierId: '', tags: '' },
  });
  const premium = watch('premium');

  useEffect(() => {
    if (existing.data) {
      reset({
        title: existing.data.title,
        body: existing.data.body ?? '',
        premium: existing.data.premium,
        tierId: existing.data.tierId ? String(existing.data.tierId) : '',
        tags: existing.data.tags.join(', '),
      });
    }
  }, [existing.data, reset]);

  const onSubmit = async (v: FormValues) => {
    setServerError(null);
    const payload: PostPayload = {
      title: v.title,
      body: v.body,
      premium: v.premium,
      tierId: v.premium && v.tierId ? Number(v.tierId) : null,
      tags: v.tags.split(',').map((t) => t.trim()).filter(Boolean),
    };
    try {
      const saved = editing ? await postApi.update(Number(id), payload) : await postApi.create(payload);
      navigate(`/posts/${saved.id}`);
    } catch (e) {
      const err = toApiError(e);
      if (err.fieldErrors) {
        Object.entries(err.fieldErrors).forEach(([f, m]) => {
          if (['title', 'body'].includes(f)) setError(f as keyof FormValues, { message: m });
        });
      } else {
        setServerError(err.message || 'Could not save the post');
      }
    }
  };

  if (editing && existing.isLoading) return <Page narrow><Spinner /></Page>;

  return (
    <Page narrow>
      <h1 style={{ marginBottom: '1.4rem' }}>{editing ? 'Edit post' : 'Create a post'}</h1>
      {serverError && <div style={{ marginBottom: '1rem' }}><Alert kind="error">{serverError}</Alert></div>}
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="card">
        <Field label="Title" htmlFor="title" error={errors.title?.message}>
          <input id="title" className={`input ${errors.title ? 'invalid' : ''}`} placeholder="A title that glows in the dark"
            {...register('title', { required: 'Title is required', maxLength: { value: 200, message: 'At most 200 characters' } })} />
        </Field>
        <Field label="Body" htmlFor="body" error={errors.body?.message}>
          <textarea id="body" className={`textarea ${errors.body ? 'invalid' : ''}`} placeholder="Write your drop…"
            {...register('body', { maxLength: { value: 20000, message: 'Too long' } })} />
        </Field>
        <label className="checkbox" style={{ marginBottom: '1rem' }}>
          <input type="checkbox" {...register('premium')} /> 🔒 Premium (subscribers only)
        </label>
        {premium && (
          <Field label="Tier" htmlFor="tierId" error={errors.tierId?.message} hint="Premium posts must be tied to one of your tiers">
            <select id="tierId" className="select" {...register('tierId', { validate: (val) => (premium && !val ? 'Choose a tier for premium posts' : true) })}>
              <option value="">Select a tier…</option>
              {tiers.data?.map((t) => <option key={t.id} value={t.id}>{t.name} (${t.priceMonthly.toFixed(2)}/mo)</option>)}
            </select>
          </Field>
        )}
        {premium && tiers.data && tiers.data.length === 0 && (
          <div style={{ marginBottom: '1rem' }}><Alert kind="error">You have no tiers yet. Create a tier first.</Alert></div>
        )}
        <Field label="Tags" htmlFor="tags" hint="Comma-separated, e.g. art, music, behind-the-scenes">
          <input id="tags" className="input" placeholder="art, music" {...register('tags')} />
        </Field>
        <button className="btn btn-block" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Saving…' : editing ? 'Save changes' : '⚡ Publish'}
        </button>
      </form>
    </Page>
  );
}
