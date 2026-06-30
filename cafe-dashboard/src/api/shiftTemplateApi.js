import axiosInstance from './axiosInstance';

export const getAllTemplates = () =>
  axiosInstance.get('/api/v1/shift-templates');

export const getTemplateById = (id) =>
  axiosInstance.get(`/api/v1/shift-templates/${id}`);

export const createTemplate = (data) =>
  axiosInstance.post('/api/v1/shift-templates', data);

export const updateTemplate = (id, data) =>
  axiosInstance.put(`/api/v1/shift-templates/${id}`, data);

export const deactivateTemplate = (id) =>
  axiosInstance.put(`/api/v1/shift-templates/${id}/deactivate`);

export const deleteTemplate = (id) =>
  axiosInstance.delete(`/api/v1/shift-templates/${id}`);
