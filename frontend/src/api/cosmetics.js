import client from './client';
export const getCosmetics    = ()         => client.get('/cosmetics');
export const createCosmetic  = (data)     => client.post('/cosmetics', data);
export const updateCosmetic  = (id, data) => client.put(`/cosmetics/${id}`, data);
export const deleteCosmetic  = (id)       => client.delete(`/cosmetics/${id}`);
export const uploadOcr       = (formData) =>
  client.post('/cosmetics/ocr', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
export const confirmOcr      = (data)     => client.post('/cosmetics/ocr/confirm', data);
