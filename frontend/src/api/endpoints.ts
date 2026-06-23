import { api } from './client';
import type {
  CommentResponse,
  PagedResponse,
  PostResponse,
  ProfileResponse,
  SubscriptionResponse,
  SubscriptionTierResponse,
  UserResponse,
} from '../types';

// ---- auth ----
export interface RegisterPayload { username: string; email: string; password: string; displayName?: string; }
export interface LoginPayload { username: string; password: string; rememberMe?: boolean; }

export const authApi = {
  register: (p: RegisterPayload) => api.post<UserResponse>('/api/auth/register', p).then((r) => r.data),
  login: (p: LoginPayload) => api.post<UserResponse>('/api/auth/login', p).then((r) => r.data),
  logout: () => api.post('/api/auth/logout').then(() => undefined),
  me: () => api.get<UserResponse>('/api/auth/me').then((r) => r.data),
};

// ---- posts ----
export interface PostPayload { title: string; body: string; premium: boolean; tierId?: number | null; tags?: string[]; }

export const postApi = {
  list: (page = 0, size = 9, sort = 'createdAt,desc') =>
    api.get<PagedResponse<PostResponse>>('/api/posts', { params: { page, size, sort } }).then((r) => r.data),
  get: (id: number) => api.get<PostResponse>(`/api/posts/${id}`).then((r) => r.data),
  comments: (id: number) => api.get<CommentResponse[]>(`/api/posts/${id}/comments`).then((r) => r.data),
  create: (p: PostPayload) => api.post<PostResponse>('/api/posts', p).then((r) => r.data),
  update: (id: number, p: PostPayload) => api.put<PostResponse>(`/api/posts/${id}`, p).then((r) => r.data),
  remove: (id: number) => api.delete(`/api/posts/${id}`).then(() => undefined),
};

// ---- creators ----
export const creatorApi = {
  list: (page = 0, size = 12) =>
    api.get<PagedResponse<UserResponse>>('/api/creators', { params: { page, size } }).then((r) => r.data),
  get: (id: number) => api.get<UserResponse>(`/api/creators/${id}`).then((r) => r.data),
  posts: (id: number, page = 0, size = 12, sort = 'createdAt,desc') =>
    api.get<PagedResponse<PostResponse>>(`/api/creators/${id}/posts`, { params: { page, size, sort } }).then((r) => r.data),
  tiers: (id: number) => api.get<SubscriptionTierResponse[]>(`/api/creators/${id}/tiers`).then((r) => r.data),
};

// ---- profiles ----
export interface ProfilePayload { displayName?: string; bio?: string; avatarUrl?: string; }
export const profileApi = {
  get: (id: number) => api.get<ProfileResponse>(`/api/profiles/${id}`).then((r) => r.data),
  byUser: (userId: number) => api.get<ProfileResponse>(`/api/profiles/user/${userId}`).then((r) => r.data),
  update: (id: number, p: ProfilePayload) => api.put<ProfileResponse>(`/api/profiles/${id}`, p).then((r) => r.data),
};

// ---- tiers ----
export interface TierPayload { name: string; priceMonthly: number; perks?: string; }
export const tierApi = {
  create: (p: TierPayload) => api.post<SubscriptionTierResponse>('/api/tiers', p).then((r) => r.data),
  update: (id: number, p: TierPayload) => api.put<SubscriptionTierResponse>(`/api/tiers/${id}`, p).then((r) => r.data),
  remove: (id: number) => api.delete(`/api/tiers/${id}`).then(() => undefined),
};

// ---- subscriptions ----
export const subscriptionApi = {
  mine: () => api.get<SubscriptionResponse[]>('/api/subscriptions').then((r) => r.data),
  subscribe: (tierId: number) => api.post<SubscriptionResponse>('/api/subscriptions', { tierId }).then((r) => r.data),
  cancel: (id: number) => api.post<SubscriptionResponse>(`/api/subscriptions/${id}/cancel`).then((r) => r.data),
  remove: (id: number) => api.delete(`/api/subscriptions/${id}`).then(() => undefined),
};

// ---- comments ----
export const commentApi = {
  create: (postId: number, text: string) =>
    api.post<CommentResponse>('/api/comments', { postId, text }).then((r) => r.data),
  remove: (id: number) => api.delete(`/api/comments/${id}`).then(() => undefined),
};

// ---- admin ----
export const adminApi = {
  users: () => api.get<UserResponse[]>('/api/admin/users').then((r) => r.data),
  deleteUser: (id: number) => api.delete(`/api/admin/users/${id}`).then(() => undefined),
};
