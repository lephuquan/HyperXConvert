import React, { useState, useEffect } from 'react';

// Định nghĩa các định dạng chuyển đổi hỗ trợ
const formatOptionsMap: Record<string, Array<{ label: string; value: string }>> = {
  pdf: [
    { label: 'PDF sang Word', value: 'DOCX' },
    { label: 'Nén PDF', value: 'COMPRESSED_PDF' },
  ],
  docx: [
    { label: 'Word sang PDF', value: 'PDF' },
  ],
  jpg: [
    { label: 'JPG sang PNG', value: 'PNG' },
  ],
  png: [
    { label: 'PNG sang JPG', value: 'JPG' },
  ],
  mp4: [
    { label: 'MP4 sang MP3', value: 'MP3' },
    { label: 'Nén video', value: 'COMPRESSED_VIDEO' },
  ],
};

interface FormatSelectorProps {
  fileExtension: string | null; // Định dạng file đã upload (vd: 'pdf', 'docx', ...)
  onFormatChange: (format: string | null) => void; // Callback khi chọn định dạng
  disabled?: boolean; // Disable dropdown nếu cần
}

const FormatSelector: React.FC<FormatSelectorProps> = ({ fileExtension, onFormatChange, disabled }) => {
  const [targetFormat, setTargetFormat] = useState<string | null>(null);
  const [error, setError] = useState<string>('');

  // Khi fileExtension thay đổi, reset targetFormat
  useEffect(() => {
    setTargetFormat(null);
    setError('');
    onFormatChange(null);
  }, [fileExtension]);

  // Lấy danh sách định dạng hợp lệ dựa trên fileExtension
  const options = fileExtension ? formatOptionsMap[fileExtension.toLowerCase()] || [] : [];

  // Xử lý khi chọn định dạng
  const handleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const value = e.target.value || null;
    setTargetFormat(value);
    setError('');
    onFormatChange(value);
  };

  // Nếu không có fileExtension hoặc không có định dạng hợp lệ
  if (!fileExtension || options.length === 0) {
    return (
      <div className="mt-4 text-sm text-gray-500 text-center">
        Vui lòng tải lên file hợp lệ để chọn định dạng chuyển đổi.
      </div>
    );
  }

  return (
    <div className="mt-4 w-full">
      <label htmlFor="format-select" className="block mb-2 text-base font-medium text-gray-700">
        Chọn định dạng đích
      </label>
      <select
        id="format-select"
        className="block w-full px-4 py-3 text-base border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition disabled:bg-gray-100 disabled:text-gray-400"
        value={targetFormat || ''}
        onChange={handleChange}
        disabled={disabled}
      >
        <option value="" disabled>
          -- Chọn định dạng đích --
        </option>
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
      {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
    </div>
  );
};

export default FormatSelector; 