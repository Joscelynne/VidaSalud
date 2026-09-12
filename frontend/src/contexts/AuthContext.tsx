import React, { createContext, useEffect, useState, useCallback } from 'react';
import { useMsal, useIsAuthenticated } from '@azure/msal-react';
import type { AuthContextType, UserProfile, UserRole } from '../types/auth.types';
import { loginRequest, tokenRequest } from '../auth/authConfig';
import { extractUserProfileFromJwt } from '../utils/jwt';

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { instance, accounts, inProgress } = useMsal();
  const msalIsAuthenticated = useIsAuthenticated();

  const [user, setUser] = useState<UserProfile | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [roles, setRoles] = useState<UserRole[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const acquireToken = useCallback(async (): Promise<string | null> => {
    const activeAccount = instance.getActiveAccount() || accounts[0];
    if (!activeAccount) {
      return null;
    }

    try {
      const response = await instance.acquireTokenSilent({
        ...tokenRequest,
        account: activeAccount,
      });
      
      const token = response.accessToken;
      setAccessToken(token);

      const profile = extractUserProfileFromJwt(token);
      if (profile) {
        setUser(profile);
        setRoles(profile.roles);
      } else {
        const fallbackProfile: UserProfile = {
          oid: activeAccount.homeAccountId || activeAccount.localAccountId,
          name: activeAccount.name || 'Usuario Autenticado',
          preferredUsername: activeAccount.username,
          email: activeAccount.username,
          roles: (activeAccount.idTokenClaims?.roles as UserRole[]) || ['Client'],
          scopes: response.scopes || [],
        };
        setUser(fallbackProfile);
        setRoles(fallbackProfile.roles);
      }

      return token;
    } catch (error) {
      console.warn('[AuthContext] Falló acquireTokenSilent:', error);
      return null;
    }
  }, [instance, accounts]);

  useEffect(() => {
    const initializeAuth = async () => {
      if (inProgress !== 'none') return;

      if (msalIsAuthenticated && (accounts.length > 0)) {
        const activeAccount = instance.getActiveAccount() || accounts[0];
        if (!instance.getActiveAccount()) {
          instance.setActiveAccount(activeAccount);
        }

        await acquireToken();
      } else {
        setUser(null);
        setAccessToken(null);
        setRoles([]);
      }
      setIsLoading(false);
    };

    initializeAuth();
  }, [msalIsAuthenticated, accounts, inProgress, instance, acquireToken]);

  const login = async (): Promise<void> => {
    try {
      setIsLoading(true);
      await instance.loginRedirect(loginRequest);
    } catch (error) {
      console.error('Error al iniciar sesión con MSAL:', error);
      setIsLoading(false);
    }
  };

  const logout = async (): Promise<void> => {
    try {
      setIsLoading(true);
      await instance.logoutRedirect({
        postLogoutRedirectUri: window.location.origin,
      });
    } catch (error) {
      console.error('Error al cerrar sesión con MSAL:', error);
      setIsLoading(false);
    }
  };

  const isAuthenticated = msalIsAuthenticated && !!user;

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated,
        isLoading,
        roles,
        accessToken,
        login,
        logout,
        acquireToken,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
