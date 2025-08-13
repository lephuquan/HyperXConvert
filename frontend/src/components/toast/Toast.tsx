import React, { useState, useEffect, useRef, useCallback } from 'react';
import { ToastProps } from './ToastContext';

interface ToastComponentProps extends ToastProps {
  onClose: () => void;
  isDark: boolean;
}

const Toast: React.FC<ToastComponentProps> = ({
  message,
  type,
  autoClose,
  onClose,
  id,
}) => {
  const [isExiting, setIsExiting] = useState(false);
  const [progress, setProgress] = useState(100);
  const [isPaused, setIsPaused] = useState(false);
  const startTimeRef = useRef<number | null>(null);
  const remainingTimeRef = useRef<number | null>(null);
  // const animationRef = useRef<number | null>(null);


  // Handle toast close with animation
  const handleClose = useCallback(() => {
    setIsExiting(true);
    setTimeout(() => {
      onClose();
    }, 300); // Match CSS transition duration
  }, [onClose]);

  // Get icon based on toast type
  const getIcon = () => {
    switch (type) {
      case 'info':
        return (
          <svg className="w-5 h-5 text-blue-500" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd"></path>
          </svg>
        );
      case 'success':
        return (
          <svg className="w-5 h-5 text-green-500" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd"></path>
          </svg>
        );
      case 'warning':
        return (
          <svg className="w-5 h-5 text-yellow-500" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
            <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd"></path>
          </svg>
        );
      case 'error':
        return (
          <svg className="w-5 h-5 text-red-500" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd"></path>
          </svg>
        );
      default:
        return (
          <svg className="w-5 h-5 text-gray-500" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
            <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd"></path>
          </svg>
        );
    }
  };

  // Get background color based on toast type and theme
  const getBackgroundColor = () => {
    switch (type) {
      case 'info':
        return 'bg-white dark:bg-gray-800 border-blue-500';
      case 'success':
        return 'bg-white dark:bg-gray-800 border-green-500';
      case 'warning':
        return 'bg-white dark:bg-gray-800 border-yellow-500';
      case 'error':
        return 'bg-white dark:bg-gray-800 border-red-500';
      default:
        return 'bg-white dark:bg-gray-800 border-gray-300 dark:border-gray-500';
    }
  };

  // Get text color based on toast type and theme
  const getTextColor = () => {
    switch (type) {
      case 'info':
        return 'text-blue-800 dark:text-white';
      case 'success':
        return 'text-green-800 dark:text-white';
      case 'warning':
        return 'text-yellow-800 dark:text-white';
      case 'error':
        return 'text-red-800 dark:text-white';
      default:
        return 'text-gray-800 dark:text-white';
    }
  };

  // Get progress bar color based on toast type
  const getProgressColor = () => {
    switch (type) {
      case 'info':
        return 'bg-blue-500';
      case 'success':
        return 'bg-green-500';
      case 'warning':
        return 'bg-yellow-500';
      case 'error':
        return 'bg-red-500';
      default:
        return 'bg-gray-500';
    }
  };

  // Handle progress bar animation for auto close
  useEffect(() => {
    if (autoClose === false) {
      return;
    }
    let duration = typeof autoClose === 'number' ? autoClose : 5000;
    let interval: NodeJS.Timeout;
    let startTime = Date.now();
    let localElapsed = 0;
    if (isPaused) {
      // Nếu pause, không chạy interval
      return;
    }
    if (remainingTimeRef.current !== null) {
      // Nếu vừa resume, tiếp tục từ thời gian còn lại
      localElapsed = remainingTimeRef.current;
      startTime = Date.now() - localElapsed;
      remainingTimeRef.current = null;
    }
    startTimeRef.current = startTime;
    // Không reset progress khi hover, chỉ khi mount hoặc resume
    interval = setInterval(() => {
      if (startTimeRef.current === null) return;
      const elapsed = Date.now() - startTimeRef.current;
      const newProgress = 100 - (elapsed / duration) * 100;
      setProgress(newProgress > 0 ? newProgress : 0);
      if (newProgress <= 0) {
        clearInterval(interval);
        handleClose();
      }
    }, 100);
    return () => {
      clearInterval(interval);
    };
  }, [id, isPaused]);

  // Đảm bảo thanh progress không bị reset về 100 khi hover, chỉ reset khi mount
  useEffect(() => {
    setProgress(100);
  }, [id]);

  // Pause/resume logic
  const handleMouseEnter = () => {
    if (autoClose === false) return;
    setIsPaused(true);
    if (startTimeRef.current) {
      remainingTimeRef.current = Date.now() - startTimeRef.current;
    }
  };
  const handleMouseLeave = () => {
    if (autoClose === false) return;
    setIsPaused(false);
    // Không reset startTimeRef ở đây, sẽ được xử lý trong useEffect
  };

  return (
    <div
      className={`flex flex-col w-full max-w-xs md:max-w-md p-4 pl-2 mb-4 ${getBackgroundColor()} ${getTextColor()} rounded-lg shadow transition-opacity duration-300 ease-in-out relative ${
        isExiting ? 'opacity-0 translate-x-full' : 'opacity-100'
      }`}
      role="alert"
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      <div className="flex items-start">
        <div className="flex-shrink-0 w-4">{getIcon()}</div>
        <div className="ml-3 text-sm font-normal min-w-40">{message}</div>
        <div className="bg-green-400 w-5">
          <button
            type="button"
            className="absolute top-[5px] right-[5px] bg-white dark:bg-gray-700 text-gray-500 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-600 rounded-lg p-1.5 inline-flex h-6 w-6 focus:ring-2 focus:ring-gray-300"
            onClick={handleClose}
            aria-label="Close"
          >
            <span className="sr-only">Close</span>
            <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
              <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd"></path>
            </svg>
          </button>
        </div>
      </div>

      {/* Thanh đếm ngược thời gian */}
      {autoClose !== false && (
        <div className="absolute bottom-0 left-0 right-0 h-1 bg-gray-300 bg-opacity-30 overflow-hidden rounded-b-lg">
          <div
            className={`h-full ${getProgressColor()} rounded-b-lg`}
            style={{
              width: `${progress}%`,
              transition: 'width 0.1s linear',
              boxShadow: `0 0 4px ${type === 'info' ? '#3b82f6' : 
                           type === 'success' ? '#10b981' : 
                           type === 'warning' ? '#f59e0b' : 
                           type === 'error' ? '#ef4444' : '#6b7280'}`
            }}
          ></div>
        </div>
      )}
    </div>
  );
};

export default Toast;
