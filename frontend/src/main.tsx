import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.tsx';
import './index.css';
import axios from 'axios';

// Initialize global Axios interceptor to attach JWT token to all requests
axios.interceptors.request.use((config) => {
  const token = localStorage.getItem('sentinelgate_token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Auto-authenticate as default admin if no token is saved yet
async function initAuth() {
  const existingToken = localStorage.getItem('sentinelgate_token');
  if (!existingToken) {
    try {
      const res = await axios.post('/api/v1/auth/login', {
        username: 'admin',
        password: 'AdminSecret123!'
      });
      if (res.data?.accessToken) {
        localStorage.setItem('sentinelgate_token', res.data.accessToken);
        localStorage.setItem('sentinelgate_user', JSON.stringify({
          username: res.data.username || 'admin',
          role: res.data.role || 'ADMIN'
        }));
      }
    } catch {
      // Ignored if backend not yet ready
    }
  }
}

initAuth().finally(() => {
  ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>
  );
});
