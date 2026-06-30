import React, { useState } from 'react';
import Modal from '../ui/Modal';
import { resetPassword } from '../../api/userApi';
import { useToast } from '../ui/Toast';

export default function PasswordModal({ isOpen, onClose, employee }) {
  const { showToast } = useToast();
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!newPassword) {
      setError('Mật khẩu không được để trống');
      return;
    }
    if (newPassword.length < 6) {
      setError('Mật khẩu phải chứa ít nhất 6 ký tự');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Xác nhận mật khẩu không trùng khớp');
      return;
    }

    setLoading(true);
    try {
      await resetPassword(employee.userId, newPassword);
      showToast(`Đổi mật khẩu cho tài khoản "${employee.username}" thành công!`, 'success');
      setNewPassword('');
      setConfirmPassword('');
      onClose();
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Không thể đổi mật khẩu. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Đổi mật khẩu: ${employee?.fullName}`}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-red-50 text-red-600 rounded-lg text-sm border-l-4 border-danger">
            {error}
          </div>
        )}

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Mật khẩu mới *</label>
          <div className="relative">
            <input
              type={showPassword ? 'text' : 'password'}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="Tối thiểu 6 ký tự"
              className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800"
              disabled={loading}
              required
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 text-sm focus:outline-none"
            >
              {showPassword ? 'Ẩn' : 'Hiện'}
            </button>
          </div>
        </div>

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Xác nhận mật khẩu mới *</label>
          <input
            type={showPassword ? 'text' : 'password'}
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder="Nhập lại mật khẩu mới"
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800"
            disabled={loading}
            required
          />
        </div>

        <div className="flex justify-end space-x-3 pt-4 border-t border-gray-100">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-gray-500 hover:bg-gray-100 rounded-xl transition"
            disabled={loading}
          >
            Hủy
          </button>
          <button
            type="submit"
            className="px-4 py-2 text-sm font-semibold text-white bg-primary hover:bg-primary-light rounded-xl shadow transition"
            disabled={loading}
          >
            {loading ? 'Đang xử lý...' : 'Xác nhận đổi'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
