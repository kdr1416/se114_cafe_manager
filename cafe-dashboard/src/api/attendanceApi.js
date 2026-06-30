import axiosInstance from './axiosInstance';

export const getTeamAttendanceReport = (year, month) =>
  axiosInstance.get('/api/v1/attendances/report/team', { params: { year, month } });

export const getUserAttendanceDetails = (userId, year, month) =>
  axiosInstance.get('/api/v1/attendances/report/details', { params: { userId, year, month } });
