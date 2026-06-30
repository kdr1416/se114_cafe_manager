import React, { useState, useEffect } from 'react';
import Modal from '../ui/Modal';
import { createArea, updateArea } from '../../api/tableApi';
import { useToast } from '../ui/Toast';

export default function AreaModal({ isOpen, onClose, area, onSave }) {
  const isEdit = !!area;
  const { showToast } = useToast();

  const [areaName, setAreaName] = useState('');
  const [prefix, setPrefix] = useState('');
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (area) {
      setAreaName(area.areaName || '');
      setPrefix(area.prefix || '');
      setDescription(area.description || '');
    } else {
      setAreaName('');
      setPrefix('');
      setDescription('');
    }
    setError('');
  }, [area, isOpen]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!areaName.trim()) {
      setError('Tên khu vực không được để trống.');
      return;
    }
    if (!prefix.trim()) {
      setError('Tiền tố không được để trống.');
      return;
    }

    setLoading(true);
    try {
      const payload = {
        areaName: areaName.trim(),
        prefix: prefix.trim().toUpperCase(),
        description: description.trim() || null,
      };

      if (isEdit) {
        await updateArea(area.areaId, payload);
        showToast('Cập nhật khu vực thành công!', 'success');
      } else {
        await createArea(payload);
        showToast('Tạo khu vực mới thành công!', 'success');
      }
      onSave();
      onClose();
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Không thể lưu khu vực. Vui lòng kiểm tra lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={isEdit ? 'Chỉnh sửa khu vực' : 'Thêm khu vực mới'}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-red-50 text-red-600 rounded-lg text-sm border-l-4 border-danger">
            {error}
          </div>
        )}

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Tên khu vực *</label>
          <input
            type="text"
            value={areaName}
            onChange={(e) => setAreaName(e.target.value)}
            placeholder="Ví dụ: Tầng trệt, Sân thượng..."
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800"
            disabled={loading}
            required
          />
        </div>

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Tiền tố bàn *</label>
          <input
            type="text"
            value={prefix}
            onChange={(e) => setPrefix(e.target.value)}
            placeholder="Ví dụ: A, B, VIP (dùng tạo mã bàn như A01, B05)"
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
            placeholder="Nhập mô tả cho khu vực..."
            rows="3"
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-sm text-gray-800"
            disabled={loading}
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
            {loading ? 'Đang lưu...' : 'Lưu khu vực'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
