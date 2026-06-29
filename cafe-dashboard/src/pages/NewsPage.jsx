import React, { useEffect, useState, useMemo } from 'react';
import { getAllNews, markNewsRead, deleteNews } from '../api/newsApi';
import useAuthStore from '../store/authStore';
import Button from '../components/ui/Button';
import Badge from '../components/ui/Badge';
import Spinner from '../components/ui/Spinner';
import { useToast } from '../components/ui/Toast';
import { formatEpoch } from '../utils/formatDate';
import NewsModal from '../components/news/NewsModal';

export default function NewsPage() {
  const { showToast } = useToast();
  const currentUser = useAuthStore((state) => state.user) || { role: 'STAFF', userId: null };
  const canManageNews = currentUser.role === 'ADMIN' || currentUser.role === 'MANAGER';

  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Modals state
  const [isOpen, setIsOpen] = useState(false);
  const [selectedPost, setSelectedPost] = useState(null);

  const fetchNewsList = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await getAllNews();
      // Sort news: Pinned first, then by createdAt descending
      const sorted = (res.data || []).sort((a, b) => {
        if (a.isPinned && !b.isPinned) return -1;
        if (!a.isPinned && b.isPinned) return 1;
        return b.createdAt - a.createdAt;
      });
      setPosts(sorted);
    } catch (err) {
      console.error(err);
      setError('Lỗi khi tải bảng tin chung. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNewsList();
  }, []);

  const handleCreateClick = () => {
    setSelectedPost(null);
    setIsOpen(true);
  };

  const handleEditClick = (post) => {
    setSelectedPost(post);
    setIsOpen(true);
  };

  const handleDeleteClick = async (post) => {
    if (window.confirm(`Bạn có chắc chắn muốn xóa bản tin "${post.title}"?`)) {
      try {
        await deleteNews(post.postId);
        showToast('Xóa bản tin thành công!', 'success');
        fetchNewsList();
      } catch (err) {
        console.error(err);
        showToast('Không thể xóa bản tin.', 'error');
      }
    }
  };

  const handleReadClick = async (post) => {
    if (post.isRead) return;
    try {
      await markNewsRead(post.postId);
      // Update locally
      setPosts((prev) =>
        prev.map((p) => (p.postId === post.postId ? { ...p, isRead: true } : p))
      );
      showToast('Đã đánh dấu đã đọc.', 'success');
    } catch (err) {
      console.error(err);
    }
  };

  const getTypeLabel = (type) => {
    const types = {
      GENERAL: { label: 'Tin chung', color: 'blue' },
      INTERNAL: { label: 'Nội bộ', color: 'gray' },
      SHIFT: { label: 'Lịch trực', color: 'green' },
      PROMOTION: { label: 'Khuyến mãi', color: 'orange' },
    };
    return types[type] || { label: type, color: 'blue' };
  };

  const getPriorityBadge = (prio) => {
    if (prio === 'URGENT') return <Badge color="red">🚨 Khẩn cấp</Badge>;
    if (prio === 'HIGH') return <Badge color="orange">🔥 Quan trọng</Badge>;
    return null;
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-full min-h-[500px]">
        <Spinner />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col justify-center items-center h-full min-h-[400px] bg-surface rounded-2xl border border-red-100 p-8 shadow-sm">
        <span className="text-4xl mb-3">⚠️</span>
        <p className="text-red-600 font-semibold mb-4">{error}</p>
        <button onClick={fetchNewsList} className="px-5 py-2.5 bg-primary hover:bg-primary-light text-white font-bold rounded-xl transition shadow">
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header toolbar */}
      <div className="flex justify-between items-center bg-surface p-4 rounded-2xl border border-gray-100 shadow-sm">
        <h1 className="text-xl font-extrabold text-primary">📰 Bảng tin chung</h1>
        {canManageNews && (
          <Button onClick={handleCreateClick} className="flex items-center justify-center space-x-1.5 py-2">
            <span>➕</span> <span>Đăng bản tin</span>
          </Button>
        )}
      </div>

      {/* Announcements Cards Grid */}
      {posts.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-full min-h-[300px] bg-surface rounded-2xl border border-gray-100 p-12 text-gray-400">
          <span className="text-4xl mb-2">📰</span>
          <p className="font-semibold text-sm">Chưa có thông báo nào trong bảng tin</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {posts.map((post) => {
            const typeInfo = getTypeLabel(post.type);
            const isAuthor = currentUser.userId === post.createdByUserId;
            const canEditOrDelete = canManageNews || isAuthor;

            return (
              <div
                key={post.postId}
                className={`bg-surface rounded-2xl border transition-all duration-200 overflow-hidden flex flex-col justify-between shadow-sm relative ${
                  post.isPinned
                    ? 'border-accent border-2 bg-gradient-to-br from-white to-amber-50/10'
                    : 'border-gray-100 hover:shadow-md'
                }`}
              >
                {/* Status Dot for Unread news */}
                {!post.isRead && (
                  <span className="absolute top-4 right-4 w-3.5 h-3.5 bg-success rounded-full ring-4 ring-white animate-pulse" title="Tin mới chưa đọc"></span>
                )}

                <div>
                  {/* Banner Image */}
                  {post.imageUrl && (
                    <div className="w-full h-40 bg-gray-50 border-b border-gray-100 overflow-hidden">
                      <img
                        src={post.imageUrl}
                        alt="News banner"
                        onError={(e) => {
                          e.target.onerror = null;
                          e.target.src = 'https://placehold.co/600x200?text=CAFE+NEWS';
                        }}
                        className="object-cover w-full h-full"
                      />
                    </div>
                  )}

                  <div className="p-6 space-y-3">
                    {/* Tags row */}
                    <div className="flex flex-wrap items-center gap-2 text-xs">
                      {post.isPinned && <Badge color="yellow">📌 Đã ghim</Badge>}
                      <Badge color={typeInfo.color}>{typeInfo.label}</Badge>
                      {getPriorityBadge(post.priority)}
                    </div>

                    {/* Title */}
                    <h3 className="text-base font-extrabold text-gray-800 leading-tight">
                      {post.title}
                    </h3>

                    {/* Meta info */}
                    <div className="text-xs text-gray-400 font-semibold flex items-center space-x-2">
                      <span>✍️ {post.authorName || 'Người quản lý'}</span>
                      <span>•</span>
                      <span>📅 {formatEpoch(post.createdAt)}</span>
                    </div>

                    {/* Content */}
                    <p className="text-sm text-gray-600 leading-relaxed whitespace-pre-wrap">
                      {post.content}
                    </p>
                  </div>
                </div>

                {/* Footer Actions */}
                <div className="bg-gray-50 border-t border-gray-100 px-6 py-3 flex items-center justify-between">
                  <div>
                    {!post.isRead ? (
                      <button
                        onClick={() => handleReadClick(post)}
                        className="text-xs text-success hover:underline font-bold"
                      >
                        ✔ Đánh dấu đã đọc
                      </button>
                    ) : (
                      <span className="text-xs text-gray-400 font-medium">✓ Đã đọc</span>
                    )}
                  </div>

                  {canEditOrDelete && (
                    <div className="flex space-x-3 text-xs font-bold">
                      <button
                        onClick={() => handleEditClick(post)}
                        className="text-blue-600 hover:text-blue-800"
                      >
                        Sửa
                      </button>
                      <button
                        onClick={() => handleDeleteClick(post)}
                        className="text-red-500 hover:text-red-700"
                      >
                        Xóa
                      </button>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* News Modal */}
      <NewsModal
        isOpen={isOpen}
        onClose={() => setIsOpen(false)}
        post={selectedPost}
        onSave={fetchNewsList}
      />
    </div>
  );
}
