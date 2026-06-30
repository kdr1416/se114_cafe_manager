import axiosInstance from './axiosInstance';

export const getUsers = () =>
  axiosInstance.get('/api/v1/users');

export const getUserById = (id) =>
  axiosInstance.get(`/api/v1/users/${id}`);

export const createUser = (data) =>
  axiosInstance.post('/api/v1/users', data);

export const updateUser = (id, data) =>
  axiosInstance.put(`/api/v1/users/${id}`, data);

export const updateUserStatus = (id, isActive) =>
  axiosInstance.put(`/api/v1/users/${id}/status`, { isActive });

export const resetPassword = (id, newPassword) =>
  axiosInstance.put(`/api/v1/users/${id}/password`, { newPassword });
