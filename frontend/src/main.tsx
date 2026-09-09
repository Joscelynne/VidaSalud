import React from 'react';
import ReactDOM from 'react-dom/client';
import { MsalProvider } from '@azure/msal-react';
import { msalInstance } from './auth/msalConfig';
import App from './App';
import './index.css';

/**
 * Inicialización asíncrona oficial de MSAL v3 antes de montar el árbol React
 */
async function main() {
  await msalInstance.initialize();

  // Escuchar respuesta de redirección tras login
  const response = await msalInstance.handleRedirectPromise();
  if (response && response.account) {
    msalInstance.setActiveAccount(response.account);
  } else {
    const accounts = msalInstance.getAllAccounts();
    if (accounts.length > 0) {
      msalInstance.setActiveAccount(accounts[0]);
    }
  }

  ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
      <MsalProvider instance={msalInstance}>
        <App />
      </MsalProvider>
    </React.StrictMode>
  );
}

main().catch((err) => {
  console.error('Error fatal al inicializar MSAL React:', err);
});
