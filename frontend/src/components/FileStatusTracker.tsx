import React, { useEffect, useState, useRef, useCallback } from 'react';
import axios from '../api/api';
import Loading from './ui/Loading';
import FileDownloader from './FileDownloader';
import { useCustomToast } from './toast';
import { useTranslation } from 'react-i18next';
import { FileStatus, StatusResponse } from '../types/file';
import { FILE_STATUS } from '../constants/file';

interface FileStatusTrackerProps {
  fileId: string;
  onStatusChange?: (status: FileStatus | null) => void;
}

const getErrorMessage = (err: any): string => {
  if (err.response) {
    if (err.response.status === 404) {
      return 'file_not_found';
    } else if (err.response.status === 500) {
      return 'download_server_error';
    } else {
      return 'download_error';
    }
  } else {
    return 'cannot_connect_server';
  }
};

const StatusContent: React.FC<{ status: FileStatus | null; loading: boolean; fileId: string }> = ({ status, loading, fileId }) => {
  const { t } = useTranslation();
  if (loading) return <Loading text={t('checking_status')} size="md" />;
  
  switch (status) {
    case FILE_STATUS.UPLOADED:
      return (
        <div className="flex flex-col items-center">
          <svg
            className="animate-spin h-8 w-8 text-green-400 dark:text-green-500"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            ></circle>
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
            ></path>
          </svg>
          <p className="mt-2 text-base text-gray-700 dark:text-gray-300">
            {t('file_uploaded_waiting_validation')}
          </p>
        </div>
      );
    case FILE_STATUS.VALIDATING:
      return (
        <div className="flex flex-col items-center">
          <svg
            className="animate-spin h-8 w-8 text-green-400 dark:text-green-500"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
          >
            <circle
              className="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              strokeWidth="4"
            ></circle>
            <path
              className="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
            ></path>
          </svg>
          <p className="mt-2 text-base text-gray-700 dark:text-gray-300">{t('validating_file')}</p>
        </div>
      );
    case FILE_STATUS.VALIDATION_FAILED:
      return (
        <div className="flex flex-col items-center">
          <div className="text-red-500 text-3xl mb-2">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="40"
              height="40"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              className="lucide lucide-circle-x-icon lucide-circle-x"
            >
              <circle cx="12" cy="12" r="10" />
              <path d="m15 9-6 6" />
              <path d="m9 9 6 6" />
            </svg>
          </div>
          <p className="text-lg font-semibold mb-2">{t('validation_failed')}</p>
          <p className="text-base text-gray-700 dark:text-gray-300">
            {t('file_validation_failed_message')}
          </p>
        </div>
      );
    case FILE_STATUS.VALIDATED:
      // Hiển thị loader khi file đã được xác thực thành công và đang chờ convert
      return (
        <div className="flex flex-col items-center text-gray-700 dark:text-gray-300">
          <Loading text={t('preparing_conversion')} size="md" />
        </div>
      );
    case FILE_STATUS.CONVERTING:
      return (
        <div className="flex flex-col items-center">
          <Loading text={t('converting_file')} size="md" />
        </div>
      );
    case FILE_STATUS.CONVERSION_FAILED:
      return (
        <div className="flex flex-col items-center">
          <div className="text-red-500 text-3xl mb-2">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="40"
              height="40"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              className="lucide lucide-circle-x-icon lucide-circle-x"
            >
              <circle cx="12" cy="12" r="10" />
              <path d="m15 9-6 6" />
              <path d="m9 9 6 6" />
            </svg>
          </div>
          <p className="text-lg font-semibold mb-2">{t('conversion_failed')}</p>
          <p className="text-base text-gray-700 dark:text-gray-300">
            {t('file_conversion_failed_message')}
          </p>
        </div>
      );
    case FILE_STATUS.CONVERTED:
      return (
        <div className="flex flex-col items-center">
          <div className="text-green-600 dark:text-green-500 text-3xl mb-2">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="40"
              height="40"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              className="lucide lucide-circle-check-icon lucide-circle-check"
            >
              <circle cx="12" cy="12" r="10" />
              <path d="m9 12 2 2 4-4" />
            </svg>
          </div>
          <p className="text-lg font-semibold mb-2">{t('convert_success')}</p>
          <p className="text-base text-gray-700 dark:text-gray-300 mb-4">
            {t('file_ready_for_download')}
          </p>
          <div className="mt-2 w-full flex justify-center">
            <FileDownloader fileId={fileId} />
          </div>
        </div>
      );
    default:
      return null;
  }
};

const FileStatusTracker: React.FC<FileStatusTrackerProps> = ({
  fileId,
  onStatusChange,
}) => {
  const { t } = useTranslation();
  const toast = useCustomToast(); // Sử dụng custom toast hook
  const [status, setStatus] = useState<FileStatus | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const pollingRef = useRef<NodeJS.Timeout | null>(null);
  const prevStatusRef = useRef<FileStatus | null>(null);

  const clearPolling = () => {
    if (pollingRef.current) {
      clearInterval(pollingRef.current);
      pollingRef.current = null;
    }
  };

  const fetchStatus = useCallback(async () => {
    setError(null);
    try {
      const res = await axios.get<StatusResponse>(`/file/status/${fileId}`);
      setStatus(res.data.status);
      if (onStatusChange) onStatusChange(res.data.status);
      setLoading(false);
      // Only show toast when status transitions to CONVERTED
      if (
        res.data.status === FILE_STATUS.CONVERTED && prevStatusRef.current !== FILE_STATUS.CONVERTED
      ) {
        toast.success(t('file_converted_successfully'));
      } else if (
        res.data.status === FILE_STATUS.CONVERSION_FAILED ||
        res.data.status === FILE_STATUS.VALIDATION_FAILED
      ) {
        toast.error(t('convert_failed'));
      }
      prevStatusRef.current = res.data.status;
      if (
        res.data.status === FILE_STATUS.CONVERTED ||
        res.data.status === FILE_STATUS.CONVERSION_FAILED ||
        res.data.status === FILE_STATUS.VALIDATION_FAILED
      ) {
        clearPolling();
      }
    } catch (err: any) {
      setLoading(false);
      setStatus(null);
      if (onStatusChange) onStatusChange(null);
      setError(getErrorMessage(err));
      toast.error(t('fetch_status_error'));
      clearPolling();
    }
  }, [fileId, onStatusChange, t]);

  useEffect(() => {
    setLoading(true);
    setStatus(null);
    setError(null);
    fetchStatus();
    pollingRef.current = setInterval(fetchStatus, 3000);
    return () => {
      clearPolling();
    };
  }, [fileId, fetchStatus]);

  return (
    <div className="w-full max-w-md mx-auto p-4 sm:p-6 dark:bg-[rgb(104_189_185/58%)] bg-gray-50 rounded-lg shadow-md mt-4 flex flex-col items-center">
      <StatusContent status={status} loading={loading} fileId={fileId} />
      {!loading && error && (
        <div className="flex flex-col items-center mt-4">
          <div className="text-red-500 text-2xl mb-2">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="40"
              height="40"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              className="lucide lucide-circle-alert-icon lucide-circle-alert"
            >
              <circle cx="12" cy="12" r="10" />
              <line x1="12" x2="12" y1="8" y2="12" />
              <line x1="12" x2="12.01" y1="16" y2="16" />
            </svg>
            ️
          </div>
          <p className="text-base text-red-600 text-center">{t(error)}</p>
        </div>
      )}
    </div>
  );
};

export default FileStatusTracker;
