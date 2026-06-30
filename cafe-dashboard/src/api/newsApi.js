import axiosInstance from './axiosInstance';

export const getAllNews = () =>
  axiosInstance.get('/api/v1/news');

export const getNewsById = (id) =>
  axiosInstance.get(`/api/v1/news/${id}`);

export const createNews = (data) =>
  axiosInstance.post('/api/v1/news', data);

export const updateNews = (id, data) =>
  axiosInstance.put(`/api/v1/news/${id}`, data);

export const deleteNews = (id) =>
  axiosInstance.delete(`/api/v1/news/${id}`);

export const markNewsRead = (id) =>
  axiosInstance.post(`/api/v1/news/${id}/read`);

export const getUnreadNewsCount = () =>
  axiosInstance.get('/api/v1/news/unread-count');
