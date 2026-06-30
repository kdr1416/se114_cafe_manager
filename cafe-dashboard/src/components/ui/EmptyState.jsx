import React from 'react';

export default function EmptyState({ message = 'Không có dữ liệu', icon = '📭', className = '' }) {
  return (
    <div className={`flex flex-col items-center justify-center p-8 text-gray-500 text-center ${className}`}>
      <span className="text-4xl mb-2">{icon}</span>
      <p className="text-sm font-medium">{message}</p>
    </div>
  );
}
