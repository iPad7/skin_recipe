import axios from 'axios';

const client = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080' });

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    const isAscii = /^[\x00-\x7F]*$/.test(token);
    if (!isAscii) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.dispatchEvent(new Event('auth:logout'));
      return Promise.reject(new Error('Invalid token'));
    }
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.dispatchEvent(new Event('auth:logout'));
    }
    return Promise.reject(err);
  }
);

export default client;
