import React from 'react';

export default function Button({ children, className = '', variant = 'primary', ...props }) {
  const baseStyle = 'px-4 py-2 rounded-md font-medium transition duration-150 ease-in-out focus:outline-none';
  const variants = {
    primary: 'bg-primary text-white hover:bg-primary-light',
    secondary: 'bg-gray-200 text-gray-800 hover:bg-gray-300',
    accent: 'bg-accent text-white hover:bg-opacity-95',
    danger: 'bg-danger text-white hover:bg-opacity-95',
  };
  return (
    <button className={`${baseStyle} ${variants[variant]} ${className}`} {...props}>
      {children}
    </button>
  );
}
