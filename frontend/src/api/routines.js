import client from './client';
export const getRoutines    = ()     => client.get('/routines');
export const createRoutine  = (data) => client.post('/routines', data);
export const deleteRoutine  = (id)   => client.delete(`/routines/${id}`);
