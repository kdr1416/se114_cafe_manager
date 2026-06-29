import React, { useState, useEffect } from 'react';
import Modal from '../ui/Modal';
import { createCategory, updateCategory } from '../../api/menuApi';
import { useToast } from '../ui/Toast';

export default function CategoryModal({ isOpen, onClose, category, onSave }) {
  const isEdit = !!category;
  const { showToast } = useToast();

  const [categoryName, setCategoryName] = useState('');
  const [description, setDescription] = useState('');
  const [isActive, setIsActive] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (category) {
      setCategoryName(category.categoryName || '');
      setDescription(category.description || '');
      setIsActive(category.isActive ?? true);
    } else {
      setCategoryName('');
      setDescription('');
      setIsActive(true);
    }
    setError('');
  }, [category, isOpen]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!categoryName.trim()) {
      setError('Tên danh mục không được để trống.');
      return;
    }

    setLoading(true);
    try {
      const payload = {
        categoryName: categoryName.trim(),
        description: description.trim() || null,
        isActive,
      };

      if (isEdit) {
        await updateCategory(category.categoryId, payload);
        showToast('Cập nhật danh mục thành công!', 'success');
      } else {
        await createCategory(payload);
        showToast('Tạo danh mục mới thành công!', 'success');
      }
      onSave();
      onClose();
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Không thể lưu danh mục. Vui lòng kiểm tra lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={isEdit ? 'Chỉnh sửa danh mục' : 'Thêm danh mục mới'}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-red-50 text-red-600 rounded-lg text-sm border-l-4 border-danger">
            {error}
          </div>
        )}

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Tên danh mục *</label>
          <input
            type="text"
            value={categoryName}
            onChange={(e) => setCategoryName(e.target.value)}
            placeholder="Ví dụ: Cà phê, Sinh tố..."
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800"
            disabled={loading}
            required
          />
        </div>

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Mô tả ngắn</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Nhập mô tả cho danh mục..."
            rows="3"
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-sm text-gray-800"
            disabled={loading}
          />
        </div>

        <div className="flex items-center space-x-2 py-2">
          <input
            type="checkbox"
            id="isActiveCat"
            checked={isActive}
            onChange={(e) => setIsActive(e.target.checked)}
            className="w-4 h-4 text-primary focus:ring-accent border-gray-300 rounded"
            disabled={loading}
          />
          <label htmlFor="isActiveCat" className="text-sm font-semibold text-gray-700 select-none">
            Cho phép hoạt động (Hiển thị ở POS bán hàng)
          </label>
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
            {loading ? 'Đang lưu...' : 'Lưu danh mục'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
