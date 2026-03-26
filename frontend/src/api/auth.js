import client from './client';
export const signup    = (data) => client.post('/auth/signup', data);
export const login     = (data) => client.post('/auth/login', data);
export const getMe     = ()     => client.get('/auth/me');
export const updateMe  = (data) => client.put('/auth/me', data);
export const deleteMe  = ()     => client.delete('/auth/me');
