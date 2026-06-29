import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { login, verifyOtp, resendOtp } from '../api/authApi';
import useAuthStore from '../store/authStore';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  // 2FA OTP States
  const [requiresOtp, setRequiresOtp] = useState(false);
  const [otpCode, setOtpCode] = useState('');
  const [otpUserId, setOtpUserId] = useState(null);
  const [resendTimer, setResendTimer] = useState(0);
  const [infoMessage, setInfoMessage] = useState('');

  const navigate = useNavigate();
  const setAuth = useAuthStore((state) => state.setAuth);

  useEffect(() => {
    let interval = null;
    if (resendTimer > 0) {
      interval = setInterval(() => {
        setResendTimer((prev) => prev - 1);
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [resendTimer]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!username || !password) {
      setError('Vui lòng nhập đầy đủ tài khoản và mật khẩu');
      return;
    }

    setLoading(true);
    setError('');
    setInfoMessage('');

    try {
      const data = await login(username, password);
      
      if (data.requiresVerification) {
        setRequiresOtp(true);
        setOtpUserId(data.userId);
        setResendTimer(60);
        setInfoMessage(data.message || 'Mã OTP đã được gửi đến email của bạn.');
        setLoading(false);
        return;
      }

      completeLogin(data);
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.');
      setLoading(false);
    }
  };

  const handleOtpSubmit = async (e) => {
    e.preventDefault();
    if (!otpCode || otpCode.length !== 6) {
      setError('Vui lòng nhập mã OTP gồm 6 chữ số');
      return;
    }

    setLoading(true);
    setError('');
    setInfoMessage('');

    try {
      const data = await verifyOtp(otpUserId, otpCode);
      completeLogin(data);
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Mã OTP không chính xác hoặc đã hết hạn.');
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (resendTimer > 0) return;

    setError('');
    setInfoMessage('');
    try {
      await resendOtp(otpUserId);
      setResendTimer(60);
      setInfoMessage('Mã OTP mới đã được gửi thành công.');
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Gửi lại mã OTP thất bại.');
    }
  };

  const completeLogin = (data) => {
    if (data.role !== 'ADMIN') {
      setError('Chỉ Admin mới có thể đăng nhập');
      setLoading(false);
      return;
    }

    const user = {
      userId: data.userId,
      username: data.username,
      fullName: data.fullName,
      role: data.role,
    };

    // Set to Zustand store
    setAuth(data.token, user);
    
    // Keep direct localStorage sync as expected by interceptors & isAuthenticated
    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify(user));

    navigate('/dashboard');
  };

  const handleBackToLogin = () => {
    setRequiresOtp(false);
    setOtpCode('');
    setOtpUserId(null);
    setError('');
    setInfoMessage('');
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background px-4">
      <div className="max-w-md w-full bg-surface rounded-2xl shadow-xl border border-gray-100 overflow-hidden transition-all duration-300 hover:shadow-2xl">
        <div className="p-8">
          <div className="text-center mb-8">
            <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-primary bg-opacity-10 text-primary text-4xl mb-3">
              ☕
            </div>
            <h2 className="text-3xl font-bold text-primary">Cafe Manager</h2>
            <p className="text-gray-500 mt-2 text-sm">Hệ thống quản trị dành cho Admin</p>
          </div>

          {infoMessage && (
            <div className="mb-6 p-4 rounded-lg bg-green-50 border-l-4 border-green-500 text-green-700 text-sm flex items-center">
              <span className="mr-2">✉️</span>
              {infoMessage}
            </div>
          )}

          {error && (
            <div className="mb-6 p-4 rounded-lg bg-red-50 border-l-4 border-danger text-danger text-sm flex items-center">
              <span className="mr-2">⚠️</span>
              {error}
            </div>
          )}

          {requiresOtp ? (
            <form onSubmit={handleOtpSubmit} className="space-y-6">
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">Mã xác thực OTP (6 chữ số)</label>
                <input
                  type="text"
                  maxLength="6"
                  value={otpCode}
                  onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ''))}
                  placeholder="Nhập 6 chữ số"
                  className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:ring-2 focus:ring-accent focus:border-transparent outline-none text-center font-bold tracking-widest text-2xl text-gray-800"
                  disabled={loading}
                  required
                />
              </div>

              <div className="flex items-center justify-between text-sm">
                <button
                  type="button"
                  onClick={handleBackToLogin}
                  className="text-gray-500 hover:text-gray-700 font-semibold focus:outline-none"
                  disabled={loading}
                >
                  ← Quay lại đăng nhập
                </button>

                <button
                  type="button"
                  onClick={handleResend}
                  disabled={resendTimer > 0 || loading}
                  className={`font-semibold focus:outline-none ${
                    resendTimer > 0
                      ? 'text-gray-400 cursor-not-allowed'
                      : 'text-accent hover:text-accent-dark'
                  }`}
                >
                  {resendTimer > 0 ? `Gửi lại sau ${resendTimer}s` : 'Gửi lại mã'}
                </button>
              </div>

              <button
                type="submit"
                disabled={loading}
                className={`w-full py-3.5 rounded-xl font-bold text-white bg-primary hover:bg-primary-light focus:outline-none transition duration-200 flex items-center justify-center space-x-2 ${
                  loading ? 'opacity-70 cursor-not-allowed' : ''
                }`}
              >
                {loading ? (
                  <>
                    <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Đang xác thực...</span>
                  </>
                ) : (
                  <span>Xác Nhận OTP</span>
                )}
              </button>
            </form>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-6">
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">Tên đăng nhập</label>
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="Nhập tên đăng nhập"
                  className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:ring-2 focus:ring-accent focus:border-transparent outline-none transition duration-150 text-gray-800"
                  disabled={loading}
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">Mật khẩu</label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Nhập mật khẩu"
                    className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:ring-2 focus:ring-accent focus:border-transparent outline-none transition duration-150 text-gray-800"
                    disabled={loading}
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 focus:outline-none"
                    disabled={loading}
                  >
                    {showPassword ? '👁️' : '🔒'}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className={`w-full py-3.5 rounded-xl font-bold text-white bg-primary hover:bg-primary-light focus:outline-none transition duration-200 flex items-center justify-center space-x-2 ${
                  loading ? 'opacity-70 cursor-not-allowed' : ''
                }`}
              >
                {loading ? (
                  <>
                    <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Đang xử lý...</span>
                  </>
                ) : (
                  <span>Đăng Nhập</span>
                )}
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
