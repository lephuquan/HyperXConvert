import React from 'react';
import { useToast } from './ToastContext';
import Toast from './Toast';

interface ToastContainerProps {
  position?: 'top-right' | 'top-left' | 'bottom-right' | 'bottom-left' | 'top-center' | 'bottom-center' | 'top-and-bottom-none';
  className?: string;
  autoClose?: number | boolean; // Cho phép autoClose nhận true/false hoặc số
}

const ToastContainer: React.FC<ToastContainerProps> = ({
  position = 'top-right',
  className = '',
  autoClose = 5000, // Giá trị mặc định vẫn là 5000ms
}) => {
  const { toasts, removeToast, isDark } = useToast();

  // Map position to CSS classes
  const getPositionClasses = () => {
    switch (position) {
      case 'top-right':
        return 'top-4 right-4';
      case 'top-left':
        return 'top-4 left-4';
      case 'bottom-right':
        return 'bottom-4 right-4';
      case 'bottom-left':
        return 'bottom-4 left-4';
      case 'top-center':
        return 'top-4 left-1/2 transform -translate-x-1/2';
      case 'bottom-center':
        return 'bottom-4 left-1/2 transform -translate-x-1/2';
      case 'top-and-bottom-none':
        return '';
      default:
        return 'top-4 right-4';
    }
  };

  return (
    <div
      className={`z-50 flex flex-col ${getPositionClasses()} ${className} overflow-x-hidden overflow-y-hidden`}
      role="region"
      aria-label="Notification messages"
    >
      {toasts.map((toast) => {
        // Ưu tiên autoClose của từng toast, nếu không có thì lấy từ props (chỉ nhận number hoặc false)
        let resolvedAutoClose: number | false;
        if (typeof toast.autoClose !== 'undefined') {
          resolvedAutoClose = toast.autoClose;
        } else if (typeof autoClose === 'number') {
          resolvedAutoClose = autoClose;
        } else if (autoClose === false) {
          resolvedAutoClose = false;
        } else {
          resolvedAutoClose = 5000;
        }

        return (
          <Toast
            key={toast.id}
            {...toast}
            autoClose={resolvedAutoClose}
            onClose={() => removeToast(toast.id)}
            isDark={isDark}
          />
        );
      })}
    </div>
  );
};

export default ToastContainer;
