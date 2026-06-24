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

/** Login result: the signed JWT access token plus the authenticated user. */
export interface LoginResponse {
  token: string;
  type: string;       // "Bearer"
  expiresIn: number;  // seconds
  user: UserResponse;
}

export interface ProfileResponse {
  id: number;
  displayName: string;
  bio: string | null;
  avatarUrl: string | null;
  userId: number;
}

// NOTE: cross-service "display" fields (a user's username looked up from another
// service) are NOT returned by the microservices — only the ids are (see CLAUDE.md
// §18). They are marked optional here and the UI falls back to the id.
export interface PostResponse {
  id: number;
  title: string;
  body?: string | null; // omitted when locked
  premium: boolean;
  locked: boolean;
  createdAt: string;
  creatorId: number;
  creatorUsername?: string | null; // cross-service: not returned by content-service
  tierId?: number | null;
  tierName?: string | null;        // cross-service
  tags: string[];
}

export interface SubscriptionTierResponse {
  id: number;
  name: string;
  priceMonthly: number;
  perks: string | null;
  creatorId: number;
  creatorUsername?: string | null; // cross-service
}

export interface SubscriptionResponse {
  id: number;
  fanId: number;
  fanUsername?: string | null; // cross-service
  tierId: number;
  tierName: string;            // tier is local to subscription-service -> returned
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
  authorUsername?: string | null; // cross-service
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
