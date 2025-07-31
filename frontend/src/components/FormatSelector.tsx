import React, { useState, useEffect } from 'react';
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

  // Khi fileExtension thay đổi, reset selectedFormat
  useEffect(() => {
    setSelectedFormat(null);
    onFormatChange(null);
  }, [fileExtension, onFormatChange]);

  // Lấy danh sách định dạng hợp lệ dựa trên fileExtension
  const options: FormatOption[] = fileExtension ? formatOptionsMap[fileExtension.toLowerCase()] || [] : [];

  // Xử lý khi chọn định dạng
  const handleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const value = e.target.value || null;
    setSelectedFormat(value);
    onFormatChange(value);
  };

  // Nếu không có fileExtension hoặc không có định dạng hợp lệ
  if (!fileExtension || options.length === 0) {
    return (
      <div className="mt-4 text-sm text-gray-500 text-center">
        {t('upload_valid_file_to_select_format')}
      </div>
    );
  }

  return (
    <div className="mt-4 w-full">
      <label htmlFor="format-select" className="block mb-2 text-base font-medium text-gray-700">
        {t('select_target_format_label')}
      </label>
      <select
        id="format-select"
        className="block w-full px-4 py-3 text-base border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition disabled:bg-gray-100 disabled:text-gray-400"
        value={selectedFormat || ''}
        onChange={handleChange}
        disabled={disabled}
      >
        <option value="" disabled>
          {t('select_target_format_placeholder')}
        </option>
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {t(opt.label)}
          </option>
        ))}
      </select>
    </div>
  );
};

export default FormatSelector;
