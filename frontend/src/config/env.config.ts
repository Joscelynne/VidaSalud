/**
 * Configuración centralizada de variables de entorno para VidaSalud
 */
export const ENV = {
  AZURE_CLIENT_ID: import.meta.env.VITE_AZURE_CLIENT_ID || '00000000-0000-0000-0000-000000000000',
  AZURE_TENANT_ID: import.meta.env.VITE_AZURE_TENANT_ID || 'common',
  API_SCOPE: import.meta.env.VITE_API_SCOPE || 'api://00000000-0000-0000-0000-000000000000/access_as_user',
  API_GATEWAY_URL: import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080',
};
