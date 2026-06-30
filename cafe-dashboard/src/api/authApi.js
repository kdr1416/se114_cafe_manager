import axiosInstance from './axiosInstance';

export const login = async (username, password) => {
  const response = await axiosInstance.post('/api/v1/auth/login', {
    username,
    password,
  });
  return response.data;
};

export const verifyOtp = async (userId, otpCode) => {
  const response = await axiosInstance.post('/api/v1/auth/verify-otp', {
    userId,
    otpCode,
  });
  return response.data;
};

export const resendOtp = async (userId) => {
  const response = await axiosInstance.post('/api/v1/auth/resend-otp', {
    userId,
  });
  return response.data;
};
