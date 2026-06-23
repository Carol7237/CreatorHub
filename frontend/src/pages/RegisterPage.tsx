import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { toApiError } from '../api/client';
import { Field } from '../components/Field';
import { Alert } from '../components/ui';
import './auth.css';

interface FormValues { username: string; email: string; password: string; displayName: string; }

export function RegisterPage() {
  const { register: registerUser } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<FormValues>();

  const onSubmit = async (v: FormValues) => {
    setServerError(null);
    try {
      await registerUser({ username: v.username, email: v.email, password: v.password, displayName: v.displayName || undefined });
      navigate('/', { replace: true });
    } catch (e) {
      const err = toApiError(e);
      if (err.fieldErrors) {
        // Map backend per-field validation errors onto the form fields.
        Object.entries(err.fieldErrors).forEach(([field, message]) => {
          if (['username', 'email', 'password', 'displayName'].includes(field)) {
            setError(field as keyof FormValues, { message });
          }
        });
      } else {
        setServerError(err.message || 'Registration failed');
      }
    }
  };

  return (
    <div className="auth-screen">
      <motion.div className="auth-card" initial={{ opacity: 0, y: 24, scale: 0.97 }} animate={{ opacity: 1, y: 0, scale: 1 }} transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}>
        <div className="auth-brand">
          <span className="brand-mark">⚡</span>
          <span className="auth-brand-name">Creator<span className="gradient-text">Hub</span></span>
        </div>
        <h1 className="auth-title">Join the night</h1>
        <p className="auth-sub muted">Create your account and start publishing</p>

        {serverError && <div style={{ marginBottom: '1rem' }}><Alert kind="error">{serverError}</Alert></div>}

        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <Field label="Username" htmlFor="r-username" error={errors.username?.message}>
            <input id="r-username" className={`input ${errors.username ? 'invalid' : ''}`} placeholder="nightowl"
              {...register('username', {
                required: 'Username is required',
                minLength: { value: 3, message: 'At least 3 characters' },
                maxLength: { value: 50, message: 'At most 50 characters' },
              })} />
          </Field>
          <Field label="Email" htmlFor="r-email" error={errors.email?.message}>
            <input id="r-email" type="email" className={`input ${errors.email ? 'invalid' : ''}`} placeholder="you@night.owl"
              {...register('email', {
                required: 'Email is required',
                pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: 'Enter a valid email' },
              })} />
          </Field>
          <Field label="Password" htmlFor="r-password" error={errors.password?.message} hint="At least 8 characters">
            <input id="r-password" type="password" className={`input ${errors.password ? 'invalid' : ''}`} placeholder="••••••••"
              {...register('password', {
                required: 'Password is required',
                minLength: { value: 8, message: 'At least 8 characters' },
              })} />
          </Field>
          <Field label="Display name (optional)" htmlFor="r-display" error={errors.displayName?.message}>
            <input id="r-display" className="input" placeholder="Night Owl"
              {...register('displayName', { maxLength: { value: 100, message: 'At most 100 characters' } })} />
          </Field>
          <button className="btn btn-block" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Creating…' : 'Create account ✦'}
          </button>
        </form>

        <div className="auth-glow-line" />
        <p className="auth-foot muted">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </motion.div>
    </div>
  );
}
