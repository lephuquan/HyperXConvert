import React, { useState, useEffect, useRef } from 'react';
import { formatOptionsMap, FormatOption } from '../constants/file';
import { useTranslation } from 'react-i18next';
import { useCustomToast } from './toast';

interface FormatSelectorProps {
  fileExtension: string | null; // Định dạng file đã upload (vd: 'pdf', 'docx', ...)
  onFormatChange: (format: string | null) => void; // Callback khi chọn định dạng
  disabled?: boolean; // Disable dropdown nếu cần
  value?: string | null; // Thêm prop value để điều khiển selectedFormat từ ngoài
}

const FormatSelector: React.FC<FormatSelectorProps> = ({ fileExtension, onFormatChange, disabled, value }) => {
  const { t } = useTranslation();
  const toast = useCustomToast();
  const [selectedFormat, setSelectedFormat] = useState<string | null>(null);
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Lấy danh sách định dạng hợp lệ dựa trên fileExtension
  const options: FormatOption[] = fileExtension ? formatOptionsMap[fileExtension.toLowerCase()] || [] : [];

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // Nếu prop value thay đổi thì cập nhật selectedFormat
  useEffect(() => {
    if (value !== undefined && value !== selectedFormat) {
      setSelectedFormat(value);
    } else if (fileExtension === null) {
      setSelectedFormat(null);
    }
  }, [fileExtension, value]);

  // Hiển thị toast lỗi chỉ 1 lần khi thiếu fileExtension hoặc không có option hợp lệ
  const [hasShownToast, setHasShownToast] = useState(false);
  useEffect(() => {
    // Chỉ hiện toast nếu không phải lần đầu mount (tránh hiện khi reload page)
    if ((!fileExtension || options.length === 0) && !hasShownToast) {
      if (fileExtension !== null) {
        toast.error(t('upload_valid_file_to_select_format'));
        setHasShownToast(true);
      }
    } else if (fileExtension && options.length > 0 && hasShownToast) {
      setHasShownToast(false);
    }
  }, [fileExtension, options.length, t, toast, hasShownToast]);

  // Nếu không có fileExtension hoặc không có định dạng hợp lệ
  if (!fileExtension || options.length === 0) {
    return (
      <div className="mt-4 text-sm dark:text-gray-300 text-gray-400 text-center">
        {t('upload_valid_file_to_select_format')}
      </div>
    );
  }

  // Khi chọn option
  const handleSelect = (option: FormatOption) => {
    setSelectedFormat(option.value);
    onFormatChange(option.value);
    setIsOpen(false);
  };

  return (
    <div className="mt-4 w-full relative" ref={dropdownRef}>
      <label htmlFor="format-select" className="block mb-2 text-base font-medium text-gray-700 dark:text-gray-300">
        {t('select_target_format_label')}
      </label>
      <button
        type="button"
        className={`block w-full px-4 py-3 text-base border border-gray-300 rounded-3xl focus:outline-none focus:ring-1 focus:ring-cyan-400 focus:border-cyan-400 transition shadow-lg focus:shadow-cyber animate-fade-in appearance-none text-left flex items-center justify-between
          ${disabled ? 'bg-gray-200 dark:bg-gray-700 text-gray-400 cursor-not-allowed' : 'bg-white dark:bg-gray-900/90 text-gray-900 dark:text-gray-300'}
          ${fileExtension && !selectedFormat && !disabled ? 'animate-border-cyan' : ''}
        `}
        onClick={() => setIsOpen((prev) => !prev)}
        disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
      >
        <span>
          {selectedFormat
            ? t(options.find((o) => o.value === selectedFormat)?.label || selectedFormat)
            : t('select_target_format_placeholder')}
        </span>
        <svg className={`w-5 h-5 text-gray-400 ml-2 transition-transform ${isOpen ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
        </svg>
      </button>
      {isOpen && (
        <div className="absolute left-0 right-0 mt-1 z-50 bg-white dark:bg-gray-800 border border-cyan-300 dark:border-gray-700 rounded-2xl shadow-lg animate-fade-in overflow-hidden">
          <ul tabIndex={-1} role="listbox">
            {options.map((option, idx) => (
              <li
                key={option.value}
                className={`px-4 py-2 cursor-pointer hover:bg-cyan-100 dark:hover:bg-cyan-900/40 ${selectedFormat === option.value ? 'bg-cyan-100 dark:bg-cyan-900/40 font-bold' : ''}`}
                onClick={() => handleSelect(option)}
                role="option"
                aria-selected={selectedFormat === option.value}
              >
                {t(option.label)}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};

export default FormatSelector;
