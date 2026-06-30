import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import useAuthStore from '../../store/authStore';

export default function Sidebar() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user) || { fullName: 'Admin', role: 'ADMIN' };
  const clearAuth = useAuthStore((state) => state.clearAuth);

  const handleLogout = () => {
    // Clear state in Zustand and localStorage
    clearAuth();
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  const navItems = [
    { path: '/dashboard',     label: 'Tổng quan',        icon: '📊' },
    { path: '/revenue',       label: 'Doanh thu',         icon: '💰' },
    { path: '/employees',     label: 'Nhân viên',         icon: '👥' },
    { path: '/attendance',    label: 'Chấm công',         icon: '⏰' },
    { path: '/leave-requests',label: 'Đơn xin nghỉ',     icon: '📋' },
    { path: '/menu',          label: 'Menu',              icon: '🍽️' },
    { path: '/tables',        label: 'Bàn & Khu vực',    icon: '🪑' },
    { path: '/shift-templates',label: 'Mẫu ca trực',      icon: '📅' },
    { path: '/news',            label: 'Bảng tin chung',    icon: '📰' },
  ];

  return (
    <div className="w-60 h-screen bg-primary text-white flex flex-col justify-between flex-shrink-0 shadow-lg">
      <div>
        {/* Header Logo */}
        <div className="p-6 border-b border-primary-light flex items-center space-x-3">
          <span className="text-3xl">☕</span>
          <span className="text-xl font-bold tracking-wider">Cafe Admin</span>
        </div>

        {/* Navigation links */}
        <nav className="p-4 space-y-1">
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `flex items-center space-x-3 px-4 py-3 rounded-xl transition duration-150 font-medium ${
                  isActive
                    ? 'bg-accent text-primary font-bold shadow-md'
                    : 'hover:bg-primary-light text-white opacity-85 hover:opacity-100'
                }`
              }
            >
              <span className="text-xl">{item.icon}</span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
      </div>

      {/* User Info & Logout */}
      <div className="p-4 border-t border-primary-light bg-primary-light bg-opacity-20">
        <div className="flex items-center space-x-3 mb-4 px-2">
          <div className="w-10 h-10 rounded-full bg-accent text-primary flex items-center justify-center font-bold text-lg shadow flex-shrink-0">
            {user.fullName ? user.fullName.charAt(0).toUpperCase() : 'A'}
          </div>
          <div className="overflow-hidden">
            <h4 className="text-sm font-semibold truncate leading-tight">{user.fullName || 'Quản trị viên'}</h4>
            <span className="text-xs text-accent font-medium">{user.role || 'ADMIN'}</span>
          </div>
        </div>

        <button
          onClick={handleLogout}
          className="w-full flex items-center justify-center space-x-2 px-4 py-2.5 bg-red-600 hover:bg-red-700 text-white font-medium rounded-xl transition duration-150 shadow"
        >
          <span>🚪</span>
          <span>Đăng xuất</span>
        </button>
      </div>
    </div>
  );
}
