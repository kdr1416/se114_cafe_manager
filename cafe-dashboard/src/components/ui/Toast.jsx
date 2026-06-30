import React, { createContext, useContext, useState, useCallback } from 'react';

const ToastContext = createContext(null);

export const useToast = () => {
  const context = useContext(ToastContext);
  if (!context) throw new Error('useToast must be used within a ToastProvider');
  return context;
};

export const ToastProvider = ({ children }) => {
  const [toasts, setToasts] = useState([]);

  const showToast = useCallback((message, type = 'success') => {
    const id = Date.now();
    setToasts((prev) => [...prev, { id, message, type }]);
    
    // Auto dismiss after 3 seconds
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 3000);
  }, []);

  const removeToast = (id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      {/* Toast container - top-right */}
      <div className="fixed top-5 right-5 z-50 flex flex-col space-y-3 pointer-events-none">
        {toasts.map((toast) => {
          const typeClasses = {
            success: 'bg-green-50 border-green-500 text-green-800',
            error: 'bg-red-50 border-red-500 text-red-800',
            warning: 'bg-yellow-50 border-yellow-500 text-yellow-800',
          };
          const icons = {
            success: '✅',
            error: '❌',
            warning: '⚠️',
          };
          return (
            <div
              key={toast.id}
              className={`flex items-center space-x-3 p-4 rounded-xl border-l-4 shadow-lg pointer-events-auto transition-all duration-300 transform translate-y-0 ${typeClasses[toast.type]}`}
            >
              <span className="text-lg">{icons[toast.type]}</span>
              <span className="text-sm font-semibold">{toast.message}</span>
              <button
                onClick={() => removeToast(toast.id)}
                className="text-gray-400 hover:text-gray-600 focus:outline-none text-xs ml-4"
              >
                ✕
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
};
