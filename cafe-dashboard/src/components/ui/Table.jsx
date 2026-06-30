import React from 'react';

export default function Table({ columns = [], data = [], loading = false, emptyMessage = 'Không có dữ liệu', className = '' }) {
  return (
    <div className={`overflow-x-auto bg-surface rounded-2xl border border-gray-100 shadow-sm ${className}`}>
      <table className="min-w-full divide-y divide-gray-100">
        <thead className="bg-gray-50 bg-opacity-70">
          <tr>
            {columns.map((col, idx) => (
              <th
                key={idx}
                scope="col"
                className={`px-6 py-4 text-left text-xs font-bold text-gray-500 uppercase tracking-wider ${col.className || ''}`}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-100">
          {loading ? (
            // Skeleton rows
            [...Array(5)].map((_, rIdx) => (
              <tr key={rIdx} className="animate-pulse">
                {columns.map((col, cIdx) => (
                  <td key={cIdx} className="px-6 py-4 whitespace-nowrap">
                    <div className="h-4 bg-gray-100 rounded w-full"></div>
                  </td>
                ))}
              </tr>
            ))
          ) : data.length === 0 ? (
            // Empty state
            <tr>
              <td colSpan={columns.length} className="px-6 py-12 text-center text-gray-400 font-medium">
                <span className="text-3xl block mb-2">📭</span>
                {emptyMessage}
              </td>
            </tr>
          ) : (
            // Data rows
            data.map((row, rIdx) => (
              <tr key={rIdx} className="hover:bg-gray-50 transition duration-150">
                {columns.map((col, cIdx) => (
                  <td key={cIdx} className={`px-6 py-4 whitespace-nowrap text-sm text-gray-700 ${col.className || ''}`}>
                    {col.render ? col.render(row, rIdx) : (row[col.accessor] ?? '--')}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
