import { useAuth } from './useAuth';

export const useAccessToken = () => {
  const { accessToken, acquireToken } = useAuth();

  const getValidToken = async (): Promise<string | null> => {
    if (accessToken) return accessToken;
    return await acquireToken();
  };

  return {
    accessToken,
    getValidToken,
  };
};
