import React, { useState, useEffect } from 'react';
import Modal from '../ui/Modal';
import { createNews, updateNews } from '../../api/newsApi';
import { uploadImage } from '../../api/uploadApi';
import { useToast } from '../ui/Toast';

export default function NewsModal({ isOpen, onClose, post, onSave }) {
  const isEdit = !!post;
  const { showToast } = useToast();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [type, setType] = useState('GENERAL');
  const [priority, setPriority] = useState('NORMAL');
  const [targetType, setTargetType] = useState('ALL');
  const [targetRole, setTargetRole] = useState('STAFF');
  const [isPinned, setIsPinned] = useState(false);
  
  const [imageUrl, setImageUrl] = useState('');
  const [imageFile, setImageFile] = useState(null);
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (post) {
      setTitle(post.title || '');
      setContent(post.content || '');
      setType(post.type || 'GENERAL');
      setPriority(post.priority || 'NORMAL');
      setTargetType(post.targetType || 'ALL');
      setTargetRole(post.targetRole || 'STAFF');
      setIsPinned(post.isPinned ?? false);
      setImageUrl(post.imageUrl || '');
    } else {
      setTitle('');
      setContent('');
      setType('GENERAL');
      setPriority('NORMAL');
      setTargetType('ALL');
      setTargetRole('STAFF');
      setIsPinned(false);
      setImageUrl('');
    }
    setImageFile(null);
    setError('');
  }, [post, isOpen]);

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setImageFile(file);
      setImageUrl(URL.createObjectURL(file));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!title.trim()) {
      setError('Tiêu đề không được để trống.');
      return;
    }
    if (!content.trim()) {
      setError('Nội dung tin tức không được để trống.');
      return;
    }

    setLoading(true);
    try {
      let finalImageUrl = imageUrl;
      if (imageFile) {
        try {
          finalImageUrl = await uploadImage(imageFile, 'news');
        } catch (uploadErr) {
          console.error(uploadErr);
          setError('Không thể tải hình ảnh lên Storage. Vui lòng kiểm tra cấu hình Supabase.');
          setLoading(false);
          return;
        }
      }

      const payload = {
        title: title.trim(),
        content: content.trim(),
        type,
        priority,
        targetType,
        targetRole: targetType === 'ROLE' ? targetRole : null,
        isPinned,
        imageUrl: finalImageUrl || null,
      };

      if (isEdit) {
        await updateNews(post.postId, payload);
        showToast('Cập nhật bản tin thành công!', 'success');
      } else {
        await createNews(payload);
        showToast('Đăng bản tin mới thành công!', 'success');
      }
      onSave();
      onClose();
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Có lỗi xảy ra khi lưu bản tin.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={isEdit ? 'Chỉnh sửa bản tin' : 'Đăng bản tin mới'} size="lg">
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-red-50 text-red-600 rounded-lg text-sm border-l-4 border-danger">
            {error}
          </div>
        )}

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Tiêu đề bản tin *</label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Nhập tiêu đề ngắn gọn (Tối đa 100 ký tự)"
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-gray-800 text-sm"
            disabled={loading}
            maxLength={100}
            required
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">Loại tin *</label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value)}
              className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent bg-white text-gray-800 text-sm"
              disabled={loading}
            >
              <option value="GENERAL">Tin chung (General)</option>
              <option value="INTERNAL">Thông báo nội bộ (Internal)</option>
              <option value="SHIFT">Lịch trực (Shift)</option>
              <option value="PROMOTION">Khuyến mãi / Sự kiện</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">Độ ưu tiên *</label>
            <select
              value={priority}
              onChange={(e) => setPriority(e.target.value)}
              className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent bg-white text-gray-800 text-sm"
              disabled={loading}
            >
              <option value="NORMAL">Bình thường (Normal)</option>
              <option value="HIGH">Quan trọng (High)</option>
              <option value="URGENT">Khẩn cấp (Urgent)</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">Đối tượng hướng tới *</label>
            <select
              value={targetType}
              onChange={(e) => setTargetType(e.target.value)}
              className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent bg-white text-gray-800 text-sm"
              disabled={loading}
            >
              <option value="ALL">Tất cả mọi người</option>
              <option value="ROLE">Theo vai trò tài khoản</option>
            </select>
          </div>
        </div>

        {targetType === 'ROLE' && (
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-1">Chọn vai trò đích</label>
            <select
              value={targetRole}
              onChange={(e) => setTargetRole(e.target.value)}
              className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent bg-white text-gray-800 text-sm"
              disabled={loading}
            >
              <option value="STAFF">Nhân viên (Staff)</option>
              <option value="MANAGER">Quản lý (Manager)</option>
              <option value="ADMIN">Quản trị viên (Admin)</option>
            </select>
          </div>
        )}

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Nội dung thông báo *</label>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="Nhập chi tiết nội dung thông báo... (Tối đa 2000 ký tự)"
            rows="5"
            className="w-full px-4 py-2 border rounded-xl outline-none focus:ring-2 focus:ring-accent text-sm text-gray-800"
            disabled={loading}
            maxLength={2000}
            required
          />
        </div>

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-1">Ảnh Banner Bản Tin (Tùy chọn)</label>
          <div className="flex items-center space-x-4">
            <input
              type="file"
              accept="image/*"
              onChange={handleFileChange}
              className="hidden"
              id="news-image-upload"
              disabled={loading}
            />
            <label
              htmlFor="news-image-upload"
              className={`px-4 py-2 border border-dashed border-gray-300 rounded-xl hover:bg-gray-50 transition cursor-pointer text-sm font-semibold text-gray-600 ${
                loading ? 'opacity-50 cursor-not-allowed' : ''
              }`}
            >
              Chọn ảnh banner từ thiết bị
            </label>
            {imageFile && (
              <span className="text-xs text-gray-400 max-w-[250px] truncate">
                {imageFile.name}
              </span>
            )}
          </div>
          {imageUrl && (
            <div className="mt-3 border rounded-xl overflow-hidden w-full max-w-sm h-36 bg-gray-50 flex items-center justify-center relative">
              <img
                src={imageUrl}
                alt="Banner preview"
                onError={(e) => {
                  e.target.onerror = null;
                  e.target.src = 'https://placehold.co/400x150?text=Loi+anh';
                }}
                className="object-cover w-full h-full"
              />
            </div>
          )}
        </div>

        <div className="flex items-center space-x-2 py-2 border-t border-gray-100">
          <input
            type="checkbox"
            id="isPinned"
            checked={isPinned}
            onChange={(e) => setIsPinned(e.target.checked)}
            className="w-4 h-4 text-primary focus:ring-accent border-gray-300 rounded"
            disabled={loading}
          />
          <label htmlFor="isPinned" className="text-sm font-semibold text-gray-700 select-none">
            Ghim thông báo này lên hàng đầu
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
