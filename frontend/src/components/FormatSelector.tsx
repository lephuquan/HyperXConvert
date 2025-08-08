import React, { useState, useEffect, useRef } from 'react';
import { formatOptionsMap, FormatOption } from '../constants/file';
import { useTranslation } from 'react-i18next';

interface FormatSelectorProps {
  fileExtension: string | null; // Định dạng file đã upload (vd: 'pdf', 'docx', ...)
  onFormatChange: (format: string | null) => void; // Callback khi chọn định dạng
  disabled?: boolean; // Disable dropdown nếu cần
}

const FormatSelector: React.FC<FormatSelectorProps> = ({ fileExtension, onFormatChange, disabled }) => {
  const { t } = useTranslation();
  const [selectedFormat, setSelectedFormat] = useState<string | null>(null);
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Khi fileExtension thay đổi, reset selectedFormat
  useEffect(() => {
    setSelectedFormat(null);
    onFormatChange(null);
  }, [fileExtension, onFormatChange]);

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

  // Nếu không có fileExtension hoặc không có định dạng hợp lệ
  if (!fileExtension || options.length === 0) {
    return (
      <div className="mt-4 text-sm dark:text-slate-900 text-gray-500 text-center">
        {t('upload_valid_file_to_select_format')}
      </div>
    );
  }

  return (
    <div className="mt-4 w-full relative" ref={dropdownRef}>
      <label htmlFor="format-select" className="block mb-2 text-base font-medium text-gray-700">
        {t('select_target_format_label')}
      </label>
      <button
        type="button"
        className="block w-full px-4 py-3 text-base border border-gray-300 rounded-3xl focus:outline-none focus:ring-2 focus:ring-cyan-400 focus:border-cyan-400 transition disabled:bg-gray-100 disabled:text-gray-400 shadow-lg focus:shadow-cyber animate-fade-in appearance-none bg-white dark:bg-gray-900/90 text-gray-900 dark:text-gray-100 text-left flex items-center justify-between"
        onClick={() => setIsOpen((prev) => !prev)}
        disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
      >
        <span>
          {selectedFormat
            ? t(options.find((opt) => opt.value === selectedFormat)?.label || '')
            : t('select_target_format_placeholder')}
        </span>
        <svg className={`w-5 h-5 text-gray-400 ml-2 transition-transform ${isOpen ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
        </svg>
      </button>
      {isOpen && (
        <div className="absolute left-0 right-0 mt-1 z-50 bg-gray-200 dark:bg-gray-800 border border-cyan-300 dark:border-gray-700 rounded-2xl shadow-lg animate-fade-in overflow-hidden">
          <ul tabIndex={-1} role="listbox">
            {options.map((opt, idx) => (
              <li
                key={opt.value}
                role="option"
                aria-selected={selectedFormat === opt.value}
                className={`px-4 py-3 cursor-pointer transition-colors text-base flex items-center gap-2
                  ${selectedFormat === opt.value
                    ? 'bg-cyan-100 text-cyan-700 dark:bg-cyan-900/30 dark:text-cyan-300'
                    : 'text-gray-700 hover:bg-gray-100 dark:text-gray-200 dark:hover:bg-gray-700'}
                  ${idx === 0 ? 'rounded-t-2xl' : ''} ${idx === options.length - 1 ? 'rounded-b-2xl' : ''}`}
                onClick={() => {
                  setSelectedFormat(opt.value);
                  onFormatChange(opt.value);
                  setIsOpen(false);
                }}
              >
                <span className="flex-1">{t(opt.label)}</span>
                {selectedFormat === opt.value && (
                  <svg className="w-4 h-4 text-cyan-400 ml-2" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                  </svg>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};

export default FormatSelector;
