import axios from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';
import { msalInstance, tokenRequest } from '../auth/msalConfig';
import { ENV } from '../config/env.config';

export const api = axios.create({
  baseURL: ENV.API_GATEWAY_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

api.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    try {
      const accounts = msalInstance.getAllAccounts();
      if (accounts.length > 0) {
        let activeAccount = msalInstance.getActiveAccount();
        if (!activeAccount) {
          activeAccount = accounts[0];
          msalInstance.setActiveAccount(activeAccount);
        }

        const response = await msalInstance.acquireTokenSilent({
          ...tokenRequest,
          account: activeAccount,
        });

        if (response?.accessToken) {
          config.headers.Authorization = `Bearer ${response.accessToken}`;
        }
      }
    } catch (error) {
      console.warn('[Axios Interceptor] Falló acquireTokenSilent:', error);
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      console.warn('[BFF API] 401 Unauthorized - Token no válido o ausente.');
    } else if (error.response?.status === 403) {
      console.warn('[BFF API] 403 Forbidden - Acceso denegado.');
    }
    return Promise.reject(error);
  }
);

export default api;
