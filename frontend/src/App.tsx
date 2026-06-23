import { Routes, Route, useLocation } from 'react-router-dom';
import { AnimatePresence } from 'framer-motion';
import { Layout } from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { FeedPage } from './pages/FeedPage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { PostDetailPage } from './pages/PostDetailPage';
import { CreatorPage } from './pages/CreatorPage';
import { CreatePostPage } from './pages/CreatePostPage';
import { TiersPage } from './pages/TiersPage';
import { SubscriptionsPage } from './pages/SubscriptionsPage';
import { ProfileEditPage } from './pages/ProfileEditPage';
import { AdminPage } from './pages/AdminPage';
import { NotFoundPage } from './pages/NotFoundPage';

export default function App() {
  const location = useLocation();
  return (
    <AnimatePresence mode="wait">
      <Routes location={location} key={location.pathname}>
        <Route element={<Layout />}>
          <Route path="/" element={<FeedPage />} />
          <Route path="/creators/:id" element={<CreatorPage />} />
          <Route path="/creators" element={<CreatorPage list />} />
          <Route path="/posts/:id" element={<PostDetailPage />} />
          <Route path="/posts/new" element={<ProtectedRoute><CreatePostPage /></ProtectedRoute>} />
          <Route path="/posts/:id/edit" element={<ProtectedRoute><CreatePostPage /></ProtectedRoute>} />
          <Route path="/tiers" element={<ProtectedRoute><TiersPage /></ProtectedRoute>} />
          <Route path="/subscriptions" element={<ProtectedRoute><SubscriptionsPage /></ProtectedRoute>} />
          <Route path="/profile/edit" element={<ProtectedRoute><ProfileEditPage /></ProtectedRoute>} />
          <Route path="/admin" element={<ProtectedRoute requireAdmin><AdminPage /></ProtectedRoute>} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Routes>
    </AnimatePresence>
  );
}
