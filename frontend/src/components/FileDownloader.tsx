import React, { useState } from 'react';
import axios from '../api/api';
import Loading from './ui/Loading';

interface FileDownloaderProps {
  fileId: string;
  buttonText?: string;
  className?: string;
}

const FileDownloader: React.FC<FileDownloaderProps> = ({ fileId, buttonText = 'Tải file về', className = '' }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleDownload = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await axios.get(`/file/download/${fileId}`);
      const preSignedUrl = res.data?.preSignedUrl || res.data?.downloadUrl;
      if (!preSignedUrl) {
        setError('Không nhận được liên kết tải file. Vui lòng thử lại.');
        setLoading(false);
        return;
      }
      // Tạo thẻ <a> động để tải file
      const link = document.createElement('a');
      link.href = preSignedUrl;
      link.setAttribute('download', '');
      link.setAttribute('target', '_blank');
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      setLoading(false);
    } catch (err: any) {
      setLoading(false);
      if (err.response) {
        if (err.response.status === 404) {
          setError('File không tồn tại hoặc chưa được chuyển đổi.');
        } else if (err.response.status === 500) {
          setError('Lỗi hệ thống khi tải file. Vui lòng thử lại sau.');
        } else {
          setError('Đã xảy ra lỗi khi tải file. Vui lòng thử lại.');
        }
      } else {
        setError('Không thể kết nối tới máy chủ. Vui lòng kiểm tra mạng.');
      }
    }
  };

  return (
    <div className={`w-full flex flex-col items-center ${className}`}>
      <button
        className="bg-blue-500 text-white px-6 py-2 rounded-lg shadow hover:bg-blue-600 transition text-base font-medium w-full max-w-xs disabled:bg-gray-400 disabled:cursor-not-allowed"
        onClick={handleDownload}
        disabled={loading}
      >
        {loading ? <Loading text="Đang chuẩn bị tải..." size="sm" /> : buttonText}
      </button>
      {error && (
        <p className="mt-2 text-sm text-red-600 text-center w-full max-w-xs">{error}</p>
      )}
    </div>
  );
};

export default FileDownloader; 