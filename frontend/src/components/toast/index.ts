import { ToastProvider, useToast as useToastContext } from './ToastContext';
import ToastContainer from './ToastContainer';
import type { ToastType } from './ToastContext';

// Custom hook để sử dụng toast dễ dàng
export const useCustomToast = () => {
  const { addToast } = useToastContext();

  return {
    info: (message: string, options?: { autoClose?: number | false }) =>
      addToast(message, 'info', options),
    success: (message: string, options?: { autoClose?: number | false }) =>
      addToast(message, 'success', options),
    warning: (message: string, options?: { autoClose?: number | false }) =>
      addToast(message, 'warning', options),
    error: (message: string, options?: { autoClose?: number | false }) =>
      addToast(message, 'error', options),
    default: (message: string, options?: { autoClose?: number | false }) =>
      addToast(message, 'default', options),
  };
};

export { ToastProvider, ToastContainer };
export type { ToastType };
