import React, { useEffect, useState, useRef } from 'react';
import axios from '../api/api';
import Loading from './ui/Loading';
import FileDownloader from './FileDownloader';

interface FileStatusTrackerProps {
  fileId: string;
}

type Status = 'PROCESSING' | 'SUCCESS' | 'FAILED';

interface StatusResponse {
  status: Status;
  downloadUrl?: string;
  message?: string;
}

const FileStatusTracker: React.FC<FileStatusTrackerProps> = ({ fileId }) => {
  const [status, setStatus] = useState<Status | null>(null);
  const [downloadUrl, setDownloadUrl] = useState<string | undefined>(undefined);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const pollingRef = useRef<NodeJS.Timeout | null>(null);

  const fetchStatus = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await axios.get<StatusResponse>(`/file/status/${fileId}`);
      setStatus(res.data.status);
      setDownloadUrl(res.data.downloadUrl);
      setLoading(false);
      if (res.data.status === 'SUCCESS' || res.data.status === 'FAILED') {
        if (pollingRef.current) clearInterval(pollingRef.current);
      }
    } catch (err: any) {
      setLoading(false);
      if (err.response) {
        if (err.response.status === 404) {
          setError('Không tìm thấy file. Vui lòng kiểm tra lại hoặc thử lại sau.');
        } else if (err.response.status === 500) {
          setError('Lỗi hệ thống. Vui lòng thử lại sau.');
        } else {
          setError('Đã xảy ra lỗi. Vui lòng thử lại.');
        }
      } else {
        setError('Không thể kết nối tới máy chủ. Vui lòng kiểm tra mạng.');
      }
      if (pollingRef.current) clearInterval(pollingRef.current);
    }
  };

  useEffect(() => {
    fetchStatus();
    pollingRef.current = setInterval(fetchStatus, 3000);
    return () => {
      if (pollingRef.current) clearInterval(pollingRef.current);
    };
    // eslint-disable-next-line
  }, [fileId]);

  return (
    <div className="w-full max-w-md mx-auto p-4 sm:p-6 bg-white rounded-lg shadow-md mt-4 flex flex-col items-center">
      {loading && <Loading text="Đang kiểm tra trạng thái..." size="md" />}
      {!loading && status === 'PROCESSING' && (
        <div className="flex flex-col items-center">
          <Loading text="Đang xử lý..." size="md" />
          <p className="mt-2 text-base text-gray-700">Vui lòng chờ trong giây lát.</p>
        </div>
      )}
      {!loading && status === 'SUCCESS' && (
        <div className="flex flex-col items-center">
          <div className="text-green-600 text-3xl mb-2">✔️</div>
          <p className="text-lg font-semibold mb-2">Chuyển đổi thành công!</p>
          <div className="mt-2 w-full flex justify-center">
            <FileDownloader fileId={fileId} />
          </div>
        </div>
      )}
      {!loading && status === 'FAILED' && (
        <div className="flex flex-col items-center">
          <div className="text-red-500 text-3xl mb-2">❌</div>
          <p className="text-lg font-semibold mb-2">Chuyển đổi thất bại.</p>
          <p className="text-base text-gray-700">Vui lòng thử lại hoặc liên hệ hỗ trợ.</p>
        </div>
      )}
      {!loading && error && (
        <div className="flex flex-col items-center mt-4">
          <div className="text-red-500 text-2xl mb-2">⚠️</div>
          <p className="text-base text-red-600 text-center">{error}</p>
        </div>
      )}
    </div>
  );
};

export default FileStatusTracker; 