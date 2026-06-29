import React, { useState } from 'react';
import Modal from '../ui/Modal';
import { rejectLeave } from '../../api/leaveApi';
import { useToast } from '../ui/Toast';
import { formatEpoch } from '../../utils/formatDate';

export default function RejectModal({ isOpen, onClose, request, onConfirm }) {
  const { showToast } = useToast();
  const [reviewNote, setReviewNote] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!reviewNote.trim()) {
      setError('Bắt buộc phải nhập lý do từ chối đơn nghỉ.');
      return;
    }

    setLoading(true);
    try {
      await rejectLeave(request.leaveRequestId, reviewNote.trim());
      showToast('Đã từ chối đơn xin nghỉ thành công.', 'success');
      setReviewNote('');
      onConfirm();
      onClose();
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Thao tác từ chối thất bại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Từ chối đơn xin nghỉ">
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-red-50 text-red-600 rounded-lg text-sm border-l-4 border-danger">
            {error}
          </div>
        )}

        {request && (
          <div className="bg-gray-50 p-4 rounded-xl space-y-2 text-sm text-gray-700">
            <div>
              <span className="font-semibold text-gray-500">Nhân viên:</span>{' '}
              <span className="font-bold text-gray-800">{request.userName}</span>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <div>
                <span className="font-semibold text-gray-500">Từ ngày:</span>{' '}
                <span className="font-bold">{formatEpoch(request.startAt)}</span>
              </div>
              <div>
                <span className="font-semibold text-gray-500">Đến ngày:</span>{' '}
                <span className="font-bold">{formatEpoch(request.endAt)}</span>
              </div>
            </div>
            <div>
              <span className="font-semibold text-gray-500">Lý do nghỉ:</span>{' '}
              <span className="italic">"{request.reason}"</span>
            </div>
          </div>
        )}

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Lý do từ chối (Bắt buộc) *</label>
          <textarea
            value={reviewNote}
            onChange={(e) => setReviewNote(e.target.value)}
            placeholder="Nhập lý do không đồng ý phê duyệt đơn nghỉ..."
            rows="3"
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-sm text-gray-800"
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
            className="px-4 py-2 text-sm font-semibold text-white bg-danger hover:bg-opacity-90 rounded-xl shadow transition"
            disabled={loading}
          >
            {loading ? 'Đang từ chối...' : 'Từ chối đơn'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
