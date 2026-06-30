import React, { useState, useEffect } from 'react';
import Modal from '../ui/Modal';
import { createUser, updateUser } from '../../api/userApi';
import { useToast } from '../ui/Toast';

export default function EmployeeModal({ isOpen, onClose, employee, onSave }) {
  const isEdit = !!employee;
  const { showToast } = useToast();

  const [formData, setFormData] = useState({
    fullName: '',
    username: '',
    password: '',
    phone: '',
    email: '',
    role: 'STAFF',
  });
  const [loading, setLoading] = useState(false);
  const [validationError, setValidationError] = useState('');

  useEffect(() => {
    if (employee) {
      setFormData({
        fullName: employee.fullName || '',
        username: employee.username || '',
        password: '',
        phone: employee.phone || '',
        email: employee.email || '',
        role: employee.role || 'STAFF',
      });
    } else {
      setFormData({
        fullName: '',
        username: '',
        password: '',
        phone: '',
        email: '',
        role: 'STAFF',
      });
    }
    setValidationError('');
  }, [employee, isOpen]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setValidationError('');

    // Validation
    if (!formData.fullName.trim()) {
      setValidationError('Họ tên không được để trống');
      return;
    }
    if (!formData.username.trim()) {
      setValidationError('Tên đăng nhập không được để trống');
      return;
    }
    if (formData.username.trim().length < 3) {
      setValidationError('Tên đăng nhập phải chứa ít nhất 3 ký tự');
      return;
    }
    if ((formData.role === 'ADMIN' || formData.role === 'MANAGER') && !formData.email.trim()) {
      setValidationError('Email là bắt buộc đối với vai trò quản trị (Admin/Manager)');
      return;
    }
    if (!isEdit && (!formData.password || formData.password.length < 6)) {
      setValidationError('Mật khẩu không được để trống và phải có ít nhất 6 ký tự');
      return;
    }

    setLoading(true);
    try {
      if (isEdit) {
        // Edit payload doesn't take username/password
        const payload = {
          fullName: formData.fullName.trim(),
          phone: formData.phone.trim() || null,
          email: formData.email.trim() || null,
          role: formData.role,
        };
        await updateUser(employee.userId, payload);
        showToast('Cập nhật nhân viên thành công!', 'success');
      } else {
        const payload = {
          fullName: formData.fullName.trim(),
          username: formData.username.trim(),
          password: formData.password,
          phone: formData.phone.trim() || null,
          email: formData.email.trim() || null,
          role: formData.role,
        };
        await createUser(payload);
        showToast('Thêm nhân viên mới thành công!', 'success');
      }
      onSave();
      onClose();
    } catch (err) {
      console.error(err);
      setValidationError(err.response?.data?.message || 'Có lỗi xảy ra. Vui lòng kiểm tra lại thông tin.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={isEdit ? 'Chỉnh sửa thông tin nhân viên' : 'Thêm nhân viên mới'}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {validationError && (
          <div className="p-3 bg-red-50 text-red-600 rounded-lg text-sm border-l-4 border-danger">
            {validationError}
          </div>
        )}

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Họ tên nhân viên *</label>
          <input
            type="text"
            name="fullName"
            value={formData.fullName}
            onChange={handleChange}
            placeholder="Ví dụ: Nguyễn Văn A"
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800"
            disabled={loading}
            required
          />
        </div>

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Tên đăng nhập *</label>
          <input
            type="text"
            name="username"
            value={formData.username}
            onChange={handleChange}
            placeholder="Ví dụ: nguyenvana"
            className={`w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800 ${
              isEdit ? 'bg-gray-100 cursor-not-allowed' : ''
            }`}
            disabled={loading || isEdit}
            required
          />
        </div>

        {!isEdit && (
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">Mật khẩu ban đầu *</label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="Tối thiểu 6 ký tự"
              className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800"
              disabled={loading}
              required
            />
          </div>
        )}

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Số điện thoại</label>
          <input
            type="text"
            name="phone"
            value={formData.phone}
            onChange={handleChange}
            placeholder="Ví dụ: 0987654321"
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800"
            disabled={loading}
          />
        </div>

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Email {(formData.role === 'ADMIN' || formData.role === 'MANAGER') && '*'}</label>
          <input
            type="email"
            name="email"
            value={formData.email}
            onChange={handleChange}
            placeholder="Ví dụ: admin@gmail.com"
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800"
            disabled={loading}
            required={formData.role === 'ADMIN' || formData.role === 'MANAGER'}
          />
        </div>

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Vai trò hệ thống *</label>
          <select
            name="role"
            value={formData.role}
            onChange={handleChange}
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent bg-white text-gray-800"
            disabled={loading}
          >
            <option value="STAFF">Staff (Nhân viên)</option>
            <option value="MANAGER">Manager (Quản lý)</option>
            <option value="ADMIN">Admin (Quản trị viên)</option>
          </select>
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
            {loading ? 'Đang xử lý...' : 'Lưu lại'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
