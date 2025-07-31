import React, { useEffect, useState, useRef, useCallback } from 'react';
import axios from '../api/api';
import Loading from './ui/Loading';
import FileDownloader from './FileDownloader';
import { useTranslation } from 'react-i18next';

interface FileStatusTrackerProps {
  fileId: string;
  onStatusChange?: (status: Status | null) => void;
}

type Status = 'UPLOADED' | 'QUEUED_AND_VALIDATED' | 'QUEUED_AND_CONVERTED' | 'PROCESSING' | 'SUCCESS' | 'FAILED';

interface StatusResponse {
  status: Status;
  downloadUrl?: string;
  message?: string;
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

const StatusContent: React.FC<{ status: Status | null; loading: boolean; fileId: string }> = ({ status, loading, fileId }) => {
  const { t } = useTranslation();
  if (loading) return <Loading text={t('checking_status')} size="md" />;
  switch (status) {
    case 'QUEUED_AND_CONVERTED':
    case 'PROCESSING':
      return (
        <div className="flex flex-col items-center">
          <Loading text={t('converting_file')} size="md" />
        </div>
      );
    case 'UPLOADED':
      return (
        <div className="flex flex-col items-center">
          <svg className="animate-spin h-8 w-8 text-blue-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"></path>
          </svg>
          <p className="mt-2 text-base text-gray-700">{t('verifying_file')}</p>
        </div>
      );
    case 'QUEUED_AND_VALIDATED':
      return (
        <div className="flex flex-col items-center">
          <p className="mt-2 text-base text-gray-700">{t('ready_to_convert')}</p>
        </div>
      );
    case 'SUCCESS':
      return (
        <div className="flex flex-col items-center">
          <div className="text-green-600 text-3xl mb-2">✔️</div>
          <p className="text-lg font-semibold mb-2">{t('convert_success')}</p>
          <div className="mt-2 w-full flex justify-center">
            <FileDownloader fileId={fileId} />
          </div>
        </div>
      );
    case 'FAILED':
      return (
        <div className="flex flex-col items-center">
          <div className="text-red-500 text-3xl mb-2">❌</div>
          <p className="text-lg font-semibold mb-2">{t('convert_failed')}</p>
          <p className="text-base text-gray-700">{t('try_again_or_contact_support')}</p>
        </div>
      );
    default:
      return null;
  }
};

const FileStatusTracker: React.FC<FileStatusTrackerProps> = ({ fileId, onStatusChange }) => {
  const { t } = useTranslation();
  const [status, setStatus] = useState<Status | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const pollingRef = useRef<NodeJS.Timeout | null>(null);

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
      if (res.data.status === 'SUCCESS' || res.data.status === 'FAILED') {
        clearPolling();
      }
    } catch (err: any) {
      setLoading(false);
      setStatus(null);
      if (onStatusChange) onStatusChange(null);
      setError(getErrorMessage(err));
      clearPolling();
    }
  }, [fileId, onStatusChange]);

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
    <div className="w-full max-w-md mx-auto p-4 sm:p-6 bg-white rounded-lg shadow-md mt-4 flex flex-col items-center">
      <StatusContent status={status} loading={loading} fileId={fileId} />
      {!loading && error && (
        <div className="flex flex-col items-center mt-4">
          <div className="text-red-500 text-2xl mb-2">⚠️</div>
          <p className="text-base text-red-600 text-center">{t(error)}</p>
        </div>
      )}
    </div>
  );
};

export default FileStatusTracker; 