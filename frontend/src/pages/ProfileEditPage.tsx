import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { profileApi } from '../api/endpoints';
import { toApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Page } from '../components/Page';
import { Field } from '../components/Field';
import { Alert, Spinner } from '../components/ui';

interface FormValues { displayName: string; bio: string; avatarUrl: string; }

export function ProfileEditPage() {
  const { user, refresh } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);
  const profile = useQuery({ queryKey: ['profile-user', user!.id], queryFn: () => profileApi.byUser(user!.id) });
  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm<FormValues>();

  useEffect(() => {
    if (profile.data) reset({ displayName: profile.data.displayName, bio: profile.data.bio ?? '', avatarUrl: profile.data.avatarUrl ?? '' });
  }, [profile.data, reset]);

  const onSubmit = async (v: FormValues) => {
    setServerError(null);
    try {
      await profileApi.update(profile.data!.id, { displayName: v.displayName, bio: v.bio, avatarUrl: v.avatarUrl });
      await refresh();
      navigate(`/creators/${user!.id}`);
    } catch (e) {
      const err = toApiError(e);
      if (err.fieldErrors) Object.entries(err.fieldErrors).forEach(([f, m]) => {
        if (['displayName', 'bio', 'avatarUrl'].includes(f)) setError(f as keyof FormValues, { message: m });
      });
      else setServerError(err.message || 'Could not save profile');
    }
  };

  if (profile.isLoading) return <Page narrow><Spinner /></Page>;

  return (
    <Page narrow>
      <h1 style={{ marginBottom: '1.4rem' }}>Edit profile</h1>
      {serverError && <div style={{ marginBottom: '1rem' }}><Alert kind="error">{serverError}</Alert></div>}
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="card">
        <Field label="Display name" htmlFor="displayName" error={errors.displayName?.message}>
          <input id="displayName" className={`input ${errors.displayName ? 'invalid' : ''}`} placeholder="Night Owl"
            {...register('displayName', { maxLength: { value: 100, message: 'At most 100 characters' } })} />
        </Field>
        <Field label="Bio" htmlFor="bio" error={errors.bio?.message}>
          <textarea id="bio" className="textarea" placeholder="Tell the world who you are after dark…"
            {...register('bio', { maxLength: { value: 1000, message: 'At most 1000 characters' } })} />
        </Field>
        <Field label="Avatar URL" htmlFor="avatarUrl" error={errors.avatarUrl?.message} hint="Optional link to an image">
          <input id="avatarUrl" className="input" placeholder="https://…"
            {...register('avatarUrl', { maxLength: { value: 500, message: 'Too long' } })} />
        </Field>
        <button className="btn btn-block" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Saving…' : 'Save profile'}
        </button>
      </form>
    </Page>
  );
}
