export type UserRole = 'Admin' | 'Operator' | 'Client' | 'Auditor';

export interface UserProfile {
  oid: string;
  name: string;
  preferredUsername: string;
  email: string;
  roles: UserRole[];
  scopes: string[];
  exp?: number;
  iss?: string;
}

export interface AuthContextType {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  roles: UserRole[];
  accessToken: string | null;
  login: () => Promise<void>;
  logout: () => Promise<void>;
  acquireToken: () => Promise<string | null>;
}

export interface JwtClaims {
  oid?: string;
  sub?: string;
  name?: string;
  preferred_username?: string;
  email?: string;
  roles?: string[];
  scp?: string;
  exp?: number;
  iss?: string;
  aud?: string;
}
