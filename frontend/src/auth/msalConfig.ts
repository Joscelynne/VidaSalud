import { PublicClientApplication } from '@azure/msal-browser';
import { msalConfig, loginRequest, tokenRequest } from './authConfig';

/**
 * Instancia única (Singleton) de MSAL PublicClientApplication
 */
export const msalInstance = new PublicClientApplication(msalConfig);

export { msalConfig, loginRequest, tokenRequest };
export default msalInstance;
