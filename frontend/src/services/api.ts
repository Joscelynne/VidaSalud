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
        const activeAccount = msalInstance.getActiveAccount() || accounts[0];
        
        const response = await msalInstance.acquireTokenSilent({
          ...tokenRequest,
          account: activeAccount,
        });

        if (response.accessToken) {
          config.headers.Authorization = `Bearer ${response.accessToken}`;
        }
      }
    } catch (error) {
      console.warn('Falló acquireTokenSilent en Interceptor de Axios:', error);
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      console.error('API 401 Unauthorized: Token inválido o expirado. Redireccionando a /login');
      window.location.href = '/login';
    } else if (error.response?.status === 403) {
      console.error('API 403 Forbidden: Sin permisos suficientes. Redireccionando a /unauthorized');
      window.location.href = '/unauthorized';
    }
    return Promise.reject(error);
  }
);

export default api;
