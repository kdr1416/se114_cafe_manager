import React from 'react';

export default function Card({ title, value, subtitle, icon, color = 'primary', loading = false, className = '' }) {
  if (loading) {
    return (
      <div className={`bg-surface border border-gray-100 shadow-sm rounded-2xl p-6 animate-pulse ${className}`}>
        <div className="flex justify-between items-center mb-4">
          <div className="h-4 bg-gray-200 rounded w-24"></div>
          <div className="w-10 h-10 bg-gray-200 rounded-full"></div>
        </div>
        <div className="h-8 bg-gray-200 rounded w-32 mb-2"></div>
        <div className="h-3 bg-gray-200 rounded w-20"></div>
      </div>
    );
  }

  const colorStyles = {
    primary: 'bg-primary bg-opacity-10 text-primary border-primary',
    accent: 'bg-accent bg-opacity-10 text-primary border-accent',
    success: 'bg-green-50 text-success border-green-200',
    warning: 'bg-yellow-50 text-warning border-yellow-200',
    danger: 'bg-red-50 text-danger border-red-200',
    blue: 'bg-blue-50 text-blue-800 border-blue-200',
  };

  return (
    <div className={`bg-surface border border-gray-100 shadow-sm rounded-2xl p-6 transition-all duration-300 hover:shadow-md ${className}`}>
      <div className="flex justify-between items-center mb-4">
        <span className="text-gray-500 text-sm font-semibold tracking-wide uppercase">{title}</span>
        {icon && (
          <div className={`w-10 h-10 rounded-full flex items-center justify-center text-xl ${colorStyles[color] || colorStyles.primary}`}>
            {icon}
          </div>
        )}
      </div>
      <div className="text-2xl font-bold text-gray-800 tracking-tight">{value}</div>
      {subtitle && (
        <div className="mt-2 text-xs text-gray-400 font-medium">
          {subtitle}
        </div>
      )}
    </div>
  );
}
