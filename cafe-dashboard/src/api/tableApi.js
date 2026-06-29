import axiosInstance from './axiosInstance';

export const getAreas = () =>
  axiosInstance.get('/api/v1/areas');

export const createArea = (data) =>
  axiosInstance.post('/api/v1/areas', data);

export const updateArea = (id, data) =>
  axiosInstance.put(`/api/v1/areas/${id}`, data);

export const deleteArea = (id) =>
  axiosInstance.delete(`/api/v1/areas/${id}`);

export const getTables = (status = null) =>
  axiosInstance.get('/api/v1/tables', { params: status ? { status } : {} });

export const createTable = (data) =>
  axiosInstance.post('/api/v1/tables', data);

export const updateTable = (id, data) =>
  axiosInstance.put(`/api/v1/tables/${id}`, data);

export const deleteTable = (id) =>
  axiosInstance.delete(`/api/v1/tables/${id}`);
