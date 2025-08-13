import React, { createContext, useContext, useState, useCallback } from 'react';
import useDarkMode from '../../hooks/useDarkMode';

export type ToastType = 'info' | 'success' | 'warning' | 'error' | 'default';

export interface ToastProps {
  id: string;
  message: string;
  type: ToastType;
  autoClose?: number | false;
  onClose?: () => void;
}

interface ToastContextProps {
  toasts: ToastProps[];
  addToast: (message: string, type: ToastType, options?: { autoClose?: number | false }) => string;
  removeToast: (id: string) => void;
  clearToasts: () => void;
  isDark: boolean;
}

const ToastContext = createContext<ToastContextProps>({
  toasts: [],
  addToast: () => '',
  removeToast: () => {},
  clearToasts: () => {},
  isDark: false,
});

export const useToast = () => useContext(ToastContext);

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<ToastProps[]>([]);
  const [isDark] = useDarkMode();

  const addToast = useCallback(
    (message: string, type: ToastType, options?: { autoClose?: number | false }): string => {
      const id = Date.now().toString();
      const newToast: ToastProps = {
        id,
        message,
        type,
        autoClose: options?.autoClose ?? 5000, // Default 5 seconds
      };
      setToasts((prevToasts) => [...prevToasts, newToast]);
      return id;
    },
    []
  );

  const removeToast = useCallback((id: string) => {
    setToasts((prevToasts) => prevToasts.filter((toast) => toast.id !== id));
  }, []);

  const clearToasts = useCallback(() => {
    setToasts([]);
  }, []);

  // Đã loại bỏ useEffect để xử lý auto-close ở đây, vì nó được xử lý trong Toast.tsx

  return (
    <ToastContext.Provider
      value={{
        toasts,
        addToast,
        removeToast,
        clearToasts,
        isDark,
      }}
    >
      {children}
    </ToastContext.Provider>
  );
};
