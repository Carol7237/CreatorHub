import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { Avatar } from './ui';
import './layout.css';

function Logo() {
  return (
    <Link to="/" className="brand" aria-label="CreatorHub home">
      <span className="brand-mark">⚡</span>
      <span className="brand-name">Creator<span className="gradient-text">Hub</span></span>
    </Link>
  );
}

export function Layout() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  const doLogout = async () => {
    await logout();
    navigate('/login');
  };

  const links = [
    { to: '/', label: 'Feed', end: true },
    { to: '/creators', label: 'Creators' },
    ...(user ? [{ to: '/subscriptions', label: 'My subs' }, { to: '/posts/new', label: 'Create' }] : []),
    ...(isAdmin ? [{ to: '/admin', label: 'Admin' }] : []),
  ];

  return (
    <>
      <motion.header
        className="site-header"
        initial={{ y: -70 }}
        animate={{ y: 0 }}
        transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
      >
        <div className="container header-inner">
          <Logo />
          <nav className={`nav ${menuOpen ? 'open' : ''}`}>
            {links.map((l) => (
              <NavLink
                key={l.to}
                to={l.to}
                end={l.end}
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                onClick={() => setMenuOpen(false)}
              >
                {l.label}
              </NavLink>
            ))}
          </nav>
          <div className="header-right">
            {user ? (
              <div className="user-chip">
                <Link to={`/creators/${user.id}`} className="row" style={{ gap: '0.55rem' }}>
                  <Avatar name={user.displayName ?? user.username} />
                  <span className="user-name">{user.displayName ?? user.username}</span>
                </Link>
                <button className="btn btn-ghost btn-sm" onClick={doLogout}>Log out</button>
              </div>
            ) : (
              <div className="row" style={{ gap: '0.6rem' }}>
                <Link to="/login" className="btn btn-ghost btn-sm">Log in</Link>
                <Link to="/register" className="btn btn-sm">Join</Link>
              </div>
            )}
            <button className="burger" onClick={() => setMenuOpen((o) => !o)} aria-label="Menu">☰</button>
          </div>
        </div>
      </motion.header>
      <main>
        <Outlet />
      </main>
      <footer className="site-footer">
        <div className="container">
          <span className="muted">CreatorHub — where night owls create. © 2026</span>
        </div>
      </footer>
    </>
  );
}
