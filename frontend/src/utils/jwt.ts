import type { JwtClaims, UserProfile, UserRole } from '../types/auth.types';

export function decodeJwt(token: string): JwtClaims | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload) as JwtClaims;
  } catch (error) {
    console.error('Error decodificando JWT:', error);
    return null;
  }
}

export function extractUserProfileFromJwt(token: string): UserProfile | null {
  const claims = decodeJwt(token);
  if (!claims) return null;

  const name = claims.name || claims.sub || 'Usuario Registrado';
  const preferredUsername = claims.preferred_username || claims.email || 'usuario@vidasalud.cl';
  const oid = claims.oid || claims.sub || '00000000-0000-0000-0000-000000000000';
  
  let roles: UserRole[] = [];
  if (Array.isArray(claims.roles) && claims.roles.length > 0) {
    roles = claims.roles as UserRole[];
  } else {
    roles = ['Paciente'];
  }

  const scopes = claims.scp ? claims.scp.split(' ') : [];

  return {
    oid,
    name,
    preferredUsername,
    email: preferredUsername,
    roles,
    scopes,
    exp: claims.exp,
    iss: claims.iss,
  };
}
