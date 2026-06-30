import axiosInstance from './axiosInstance';

export const getShifts = (params) =>
  axiosInstance.get('/api/v1/shifts', { params });

export const getShiftReport = (shiftId) =>
  axiosInstance.get(`/api/v1/shifts/${shiftId}/report`);

export const getDailyShiftReport = (date) =>
  axiosInstance.get('/api/v1/shifts/daily-report', { params: { date } });
