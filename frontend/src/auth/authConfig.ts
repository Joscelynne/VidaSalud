import type { Configuration } from '@azure/msal-browser';
import { LogLevel } from '@azure/msal-browser';
import { ENV } from '../config/env.config';

/**
 * Configuración oficial de MSAL para Azure AD / Microsoft Entra ID
 * Cumple con los requisitos de la evaluación EP1 VidaSalud.
 */
export const msalConfig: Configuration = {
  auth: {
    clientId: ENV.AZURE_CLIENT_ID,
    authority: `https://login.microsoftonline.com/${ENV.AZURE_TENANT_ID}`,
    redirectUri: window.location.origin,
    postLogoutRedirectUri: window.location.origin,
  },
  cache: {
    cacheLocation: 'sessionStorage', // Exigido por requerimiento de la evaluación
  },
  system: {
    loggerOptions: {
      loggerCallback: (level, message, containsPii) => {
        if (containsPii) return;
        switch (level) {
          case LogLevel.Error:
            console.error('[MSAL Error]:', message);
            return;
          case LogLevel.Info:
            console.info('[MSAL Info]:', message);
            return;
          case LogLevel.Verbose:
            console.debug('[MSAL Verbose]:', message);
            return;
          case LogLevel.Warning:
            console.warn('[MSAL Warning]:', message);
            return;
        }
      },
      logLevel: LogLevel.Warning,
    },
  },
};

export const loginRequest = {
  scopes: ['openid', 'profile', 'email', ENV.API_SCOPE],
};

export const tokenRequest = {
  scopes: [ENV.API_SCOPE],
};
