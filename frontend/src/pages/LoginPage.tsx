import { useForm } from 'react-hook-form';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { toApiError } from '../api/client';
import { Field } from '../components/Field';
import { Alert } from '../components/ui';
import './auth.css';

interface FormValues { username: string; password: string; }

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? '/';
  const [serverError, setServerError] = useState<string | null>(null);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>();

  const onSubmit = async (v: FormValues) => {
    setServerError(null);
    try {
      await login(v);
      navigate(from, { replace: true });
    } catch (e) {
      setServerError(toApiError(e).message || 'Login failed');
    }
  };

  return (
    <div className="auth-screen">
      <motion.div className="auth-card" initial={{ opacity: 0, y: 24, scale: 0.97 }} animate={{ opacity: 1, y: 0, scale: 1 }} transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}>
        <div className="auth-brand">
          <span className="brand-mark">⚡</span>
          <span className="auth-brand-name">Creator<span className="gradient-text">Hub</span></span>
        </div>
        <h1 className="auth-title">Welcome back</h1>
        <p className="auth-sub muted">Sign in to your night-owl studio</p>

        {serverError && <div style={{ marginBottom: '1rem' }}><Alert kind="error">{serverError}</Alert></div>}

        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <Field label="Username" htmlFor="username" error={errors.username?.message}>
            <input id="username" className={`input ${errors.username ? 'invalid' : ''}`} placeholder="nightowl"
              {...register('username', { required: 'Username is required' })} />
          </Field>
          <Field label="Password" htmlFor="password" error={errors.password?.message}>
            <input id="password" type="password" className={`input ${errors.password ? 'invalid' : ''}`} placeholder="••••••••"
              {...register('password', { required: 'Password is required' })} />
          </Field>
          <button className="btn btn-block" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Signing in…' : 'Sign in →'}
          </button>
        </form>

        <div className="auth-glow-line" />
        <p className="auth-foot muted">
          New here? <Link to="/register">Create an account</Link>
        </p>
      </motion.div>
    </div>
  );
}
