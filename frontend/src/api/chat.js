import client from './client';
export const getSessions   = ()                    => client.get('/chat/sessions');
export const createSession = ()                    => client.post('/chat/sessions');
export const deleteSession = (id)                  => client.delete(`/chat/sessions/${id}`);
export const getMessages   = (id)                  => client.get(`/chat/sessions/${id}/messages`);
export const sendMessage   = (id, message)         => client.post(`/chat/sessions/${id}/messages`, { message });
