import axiosInstance from './axiosInstance';

export const getLeaveRequests = (status = null) =>
  axiosInstance.get('/api/v1/leave-requests', {
    params: status && status !== 'ALL' ? { status } : {}
  });

export const approveLeave = (id, reviewNote = '') =>
  axiosInstance.put(`/api/v1/leave-requests/${id}/approve`, { reviewNote });

export const rejectLeave = (id, reviewNote = '') =>
  axiosInstance.put(`/api/v1/leave-requests/${id}/reject`, { reviewNote });
