import axiosInstance from './axiosInstance';

export const getMonthlyRevenue = (year, month) =>
  axiosInstance.get('/api/v1/reports/revenue', { params: { year, month } });

export const getYearlySummary = (year) =>
  axiosInstance.get('/api/v1/reports/revenue/summary', { params: { year } });
