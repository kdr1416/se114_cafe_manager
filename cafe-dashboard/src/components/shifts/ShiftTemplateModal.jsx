import React, { useState, useEffect } from 'react';
import Modal from '../ui/Modal';
import { createTemplate, updateTemplate } from '../../api/shiftTemplateApi';
import { useToast } from '../ui/Toast';

// Convert date string "YYYY-MM-DD" to Epoch Milliseconds in Vietnam timezone (GMT+7)
const parseVnDateToEpoch = (dateStr) => {
  if (!dateStr) return null;
  const [year, month, day] = dateStr.split('-').map(Number);
  const utcMs = Date.UTC(year, month - 1, day, 0, 0, 0);
  return utcMs - 7 * 60 * 60 * 1000; // Shift back 7 hours to align to GMT+7 start of day
};

// Convert Epoch Milliseconds back to "YYYY-MM-DD" in Vietnam timezone (GMT+7)
const formatEpochToVnDateInput = (epochMs) => {
  if (!epochMs) return '';
  const vnDate = new Date(epochMs + 7 * 60 * 60 * 1000); // Shift forward 7 hours to extract local UTC components
  const yyyy = vnDate.getUTCFullYear();
  const mm = String(vnDate.getUTCMonth() + 1).padStart(2, '0');
  const dd = String(vnDate.getUTCDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
};

export default function ShiftTemplateModal({ isOpen, onClose, template, onSave }) {
  const isEdit = !!template;
  const { showToast } = useToast();

  const [templateName, setTemplateName] = useState('');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [minStaff, setMinStaff] = useState('2');
  const [isActive, setIsActive] = useState(true);
  const [effectiveFrom, setEffectiveFrom] = useState('');
  const [effectiveTo, setEffectiveTo] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (template) {
      setTemplateName(template.templateName || '');
      setStartTime(template.startTime || '');
      setEndTime(template.endTime || '');
      setMinStaff(String(template.minStaff || '2'));
      setIsActive(template.isActive ?? true);
      setEffectiveFrom(formatEpochToVnDateInput(template.effectiveFromDate));
      setEffectiveTo(formatEpochToVnDateInput(template.effectiveToDate));
    } else {
      setTemplateName('');
      setStartTime('');
      setEndTime('');
      setMinStaff('2');
      setIsActive(true);
      setEffectiveFrom('');
      setEffectiveTo('');
    }
    setError('');
  }, [template, isOpen]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    // Validation checks
    if (!templateName.trim()) {
      setError('Tên mẫu ca không được để trống.');
      return;
    }
    if (!startTime || !endTime) {
      setError('Thời gian bắt đầu và kết thúc không được để trống.');
      return;
    }
    if (parseInt(minStaff, 10) < 1) {
      setError('Số nhân viên tối thiểu phải từ 1 người.');
      return;
    }

    setLoading(true);
    try {
      const payload = {
        templateName: templateName.trim(),
        startTime: startTime.trim(),
        endTime: endTime.trim(),
        minStaff: parseInt(minStaff, 10),
        isActive,
        effectiveFromDate: parseVnDateToEpoch(effectiveFrom),
        effectiveToDate: parseVnDateToEpoch(effectiveTo),
      };

      if (isEdit) {
        await updateTemplate(template.templateId, payload);
        showToast('Cập nhật mẫu ca thành công!', 'success');
      } else {
        await createTemplate(payload);
        showToast('Tạo mẫu ca mới thành công!', 'success');
      }
      onSave();
      onClose();
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Không thể lưu mẫu ca. Vui lòng kiểm tra lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={isEdit ? 'Chỉnh sửa mẫu ca trực' : 'Thêm mẫu ca trực mới'}>
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-red-50 text-red-600 rounded-lg text-sm border-l-4 border-danger">
            {error}
          </div>
        )}

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Tên mẫu ca *</label>
          <input
            type="text"
            value={templateName}
            onChange={(e) => setTemplateName(e.target.value)}
            placeholder="Ví dụ: Ca Sáng, Ca Chiều, Ca Gãy..."
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800 text-sm"
            disabled={loading}
            required
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">Giờ bắt đầu *</label>
            <input
              type="time"
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800 text-sm"
              disabled={loading}
              required
            />
          </div>
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">Giờ kết thúc *</label>
            <input
              type="time"
              value={endTime}
              onChange={(e) => setTemplateName(prev => prev) || setEndTime(e.target.value)}
              className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800 text-sm"
              disabled={loading}
              required
            />
          </div>
        </div>

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Số nhân viên tối thiểu *</label>
          <input
            type="number"
            value={minStaff}
            onChange={(e) => setMinStaff(e.target.value)}
            placeholder="Ví dụ: 2"
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800 text-sm"
            disabled={loading}
            min="1"
            required
          />
        </div>

        <div className="border-t border-gray-100 pt-3">
          <span className="block text-sm font-bold text-gray-800 mb-2">Thời gian áp dụng mẫu ca (Múi giờ Việt Nam)</span>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-gray-500 mb-1">Từ ngày</label>
              <input
                type="date"
                value={effectiveFrom}
                onChange={(e) => setEffectiveFrom(e.target.value)}
                className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800 text-sm"
                disabled={loading}
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-gray-500 mb-1">Đến ngày (không bắt buộc)</label>
              <input
                type="date"
                value={effectiveTo}
                onChange={(e) => setEffectiveTo(e.target.value)}
                className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800 text-sm"
                disabled={loading}
              />
            </div>
          </div>
        </div>

        <div className="flex items-center space-x-2 py-2 border-t border-gray-100">
          <input
            type="checkbox"
            id="isActiveTemplate"
            checked={isActive}
            onChange={(e) => setIsActive(e.target.checked)}
            className="w-4 h-4 text-primary focus:ring-accent border-gray-300 rounded"
            disabled={loading}
          />
          <label htmlFor="isActiveTemplate" className="text-sm font-semibold text-gray-700 select-none">
            Mẫu ca trực này đang hoạt động
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
            {loading ? 'Đang lưu...' : 'Lưu lại'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
