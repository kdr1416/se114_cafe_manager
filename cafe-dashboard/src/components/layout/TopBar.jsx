import React from 'react';
import { useLocation } from 'react-router-dom';
import useAuthStore from '../../store/authStore';

export default function TopBar() {
  const location = useLocation();
  const user = useAuthStore((state) => state.user) || { fullName: 'Admin', role: 'ADMIN' };

  const getTitle = () => {
    switch (location.pathname) {
      case '/dashboard':
        return 'Tổng quan';
      case '/revenue':
        return 'Báo cáo doanh thu';
      case '/employees':
        return 'Quản lý nhân viên';
      case '/attendance':
        return 'Quản lý chấm công';
      case '/leave-requests':
        return 'Đơn xin nghỉ phép';
      case '/menu':
        return 'Quản lý Menu';
      case '/tables':
        return 'Bàn & Khu vực';
      case '/shift-templates':
        return 'Quản lý mẫu ca trực';
      case '/news':
        return 'Bảng tin chung';
      default:
        return 'Cafe Manager';
    }
  };

  return (
    <header className="h-16 bg-surface border-b border-gray-100 flex items-center justify-between px-6 flex-shrink-0 shadow-sm">
      {/* Title */}
      <h2 className="text-xl font-bold text-primary">{getTitle()}</h2>

      {/* User profile info */}
      <div className="flex items-center space-x-3">
        <span className="text-sm font-semibold text-gray-700">{user.fullName || 'Admin'}</span>
        <div className="w-9 h-9 rounded-full bg-accent bg-opacity-20 text-primary flex items-center justify-center font-bold border border-accent">
          👤
        </div>
      </div>
    </header>
  );
}
