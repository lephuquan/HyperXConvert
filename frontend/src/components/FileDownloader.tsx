import React, { useState } from 'react';
import axios from '../api/api';
import Loading from './ui/Loading';
import { useTranslation } from 'react-i18next';
import { useCustomToast } from './toast';

interface FileDownloaderProps {
  fileId: string;
  buttonText?: string;
  className?: string;
}

const FileDownloader: React.FC<FileDownloaderProps> = ({ fileId, buttonText = '', className = '' }) => {
  const { t } = useTranslation();
  const toast = useCustomToast(); // Sử dụng custom toast hook đúng cách
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleDownload = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await axios.get(`/file/download/${fileId}`);
      const preSignedUrl = res.data?.preSignedUrl || res.data?.downloadUrl;
      if (!preSignedUrl) {
        setError(t('no_download_link'));
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
          setError(t('file_not_found'));
          toast.error(t('file_not_found'));
        } else if (err.response.status === 500) {
          setError(t('download_server_error'));
          toast.error(t('download_server_error'));
        } else {
          setError(t('download_error'));
          toast.error(t('download_error'));
        }
      } else {
        setError(t('cannot_connect_server'));
        toast.error(t('cannot_connect_server'));
      }
    }
  };

  return (
    <div className={`w-full flex flex-col items-center ${className}`}>
      <button
        className=" text-white px-6 py-2 rounded-lg shadow hover:bg-cyber-hover bg-cyber-gradient transition text-base font-medium w-full max-w-xs disabled:bg-gray-400 disabled:cursor-not-allowed"
        onClick={handleDownload}
        disabled={loading}
      >
        {loading ? <Loading text={t('preparing_download')} size="sm" /> : (buttonText || t('download_btn'))}
      </button>
      {error && (
        <p className="mt-2 text-sm text-red-600 text-center w-full max-w-xs">{error}</p>
      )}
    </div>
  );
};

export default FileDownloader;
