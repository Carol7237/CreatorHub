// Mirrors the backend DTOs (CLAUDE.md §14).

export type Role = 'USER' | 'ADMIN';
export type SubStatus = 'ACTIVE' | 'CANCELLED' | 'EXPIRED';

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  role: Role;
  enabled: boolean;
  profileId: number | null;
  displayName: string | null;
}

export interface ProfileResponse {
  id: number;
  displayName: string;
  bio: string | null;
  avatarUrl: string | null;
  userId: number;
}

export interface PostResponse {
  id: number;
  title: string;
  body?: string | null; // omitted when locked
  premium: boolean;
  locked: boolean;
  createdAt: string;
  creatorId: number;
  creatorUsername: string;
  tierId?: number | null;
  tierName?: string | null;
  tags: string[];
}

export interface SubscriptionTierResponse {
  id: number;
  name: string;
  priceMonthly: number;
  perks: string | null;
  creatorId: number;
  creatorUsername: string;
}

export interface SubscriptionResponse {
  id: number;
  fanId: number;
  fanUsername: string;
  tierId: number;
  tierName: string;
  creatorId: number;
  startDate: string;
  status: SubStatus;
}

export interface CommentResponse {
  id: number;
  text: string;
  createdAt: string;
  postId: number;
  authorId: number;
  authorUsername: string;
}

export interface TagResponse {
  id: number;
  name: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ApiErrorResponse {
  timestamp?: string;
  status: number;
  error: string;
  message: string;
  path?: string;
  fieldErrors?: Record<string, string>;
}
