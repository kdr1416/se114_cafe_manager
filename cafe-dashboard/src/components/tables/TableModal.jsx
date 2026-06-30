import React, { useState, useEffect } from 'react';
import Modal from '../ui/Modal';
import { createTable, updateTable } from '../../api/tableApi';
import { useToast } from '../ui/Toast';

export default function TableModal({ isOpen, onClose, table, areas = [], onSave }) {
  const isEdit = !!table;
  const { showToast } = useToast();

  const [tableName, setTableName] = useState('');
  const [area, setArea] = useState('');
  const [capacity, setCapacity] = useState('4');
  const [status, setStatus] = useState('AVAILABLE');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (table) {
      setTableName(table.tableName || '');
      setArea(table.area || '');
      setCapacity(table.capacity || '4');
      setStatus(table.status || 'AVAILABLE');
    } else {
      setTableName('');
      setArea(areas[0]?.areaName || '');
      setCapacity('4');
      setStatus('AVAILABLE');
    }
    setError('');
  }, [table, areas, isOpen]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!tableName.trim()) {
      setError('Tên bàn không được để trống.');
      return;
    }
    if (!area) {
      setError('Vui lòng chọn khu vực.');
      return;
    }
    if (!capacity || parseInt(capacity, 10) <= 0) {
      setError('Sức chứa không hợp lệ.');
      return;
    }

    setLoading(true);
    try {
      const payload = {
        tableName: tableName.trim(),
        status: status.toUpperCase(),
        capacity: parseInt(capacity, 10),
        area: area.trim(),
      };

      if (isEdit) {
        await updateTable(table.tableId, payload);
        showToast('Cập nhật bàn thành công!', 'success');
      } else {
        await createTable(payload);
        showToast('Tạo bàn mới thành công!', 'success');
      }
      onSave();
      onClose();
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Có lỗi xảy ra. Vui lòng kiểm tra lại thông tin.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={isEdit ? 'Chỉnh sửa bàn' : 'Thêm bàn mới'}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-red-50 text-red-600 rounded-lg text-sm border-l-4 border-danger">
            {error}
          </div>
        )}

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Tên bàn / Số bàn *</label>
          <input
            type="text"
            value={tableName}
            onChange={(e) => setTableName(e.target.value)}
            placeholder="Ví dụ: Bàn 01, VIP 02..."
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800"
            disabled={loading}
            required
          />
        </div>

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Khu vực *</label>
          <select
            value={area}
            onChange={(e) => setArea(e.target.value)}
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent bg-white text-gray-800"
            disabled={loading}
            required
          >
            <option value="" disabled>-- Chọn khu vực --</option>
            {areas.map((a) => (
              <option key={a.areaId} value={a.areaName}>
                {a.areaName} ({a.prefix})
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Sức chứa (Chỗ ngồi) *</label>
          <input
            type="number"
            value={capacity}
            onChange={(e) => setCapacity(e.target.value)}
            placeholder="Ví dụ: 4"
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800"
            disabled={loading}
            min="1"
            required
          />
        </div>

        {isEdit && (
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">Trạng thái bàn *</label>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent bg-white text-gray-800"
              disabled={loading}
              required
            >
              <option value="AVAILABLE">AVAILABLE (Trống)</option>
              <option value="OCCUPIED">OCCUPIED (Đang dùng)</option>
              <option value="RESERVED">RESERVED (Đặt trước)</option>
            </select>
          </div>
        )}

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
            {loading ? 'Đang lưu...' : 'Lưu thông tin'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
