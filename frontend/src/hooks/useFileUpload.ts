import { useState, useCallback } from 'react';
// import { fileService } from '@/services/api';
import { ConversionRequest, ConversionResponse, ConversionStatus } from '@/types';

interface UseFileUploadResult {
  uploadFile: (request: ConversionRequest) => Promise<void>;
  uploadStatus: ConversionStatus | null;
  uploadProgress: number;
  error: string | null;
  response: ConversionResponse | null;
  reset: () => void;
}

export const useFileUpload = (): UseFileUploadResult => {
  const [uploadStatus, setUploadStatus] = useState<ConversionStatus | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number>(0);
  const [error, setError] = useState<string | null>(null);
  const [response, setResponse] = useState<ConversionResponse | null>(null);

  const uploadFile = useCallback(async (request: ConversionRequest) => {
    try {
      setError(null);
      setUploadStatus(ConversionStatus.UPLOADING);
      setUploadProgress(0);

      // Simulate upload progress
      const progressInterval = setInterval(() => {
        setUploadProgress(prev => {
          if (prev >= 90) {
            clearInterval(progressInterval);
            return 90;
          }
          return prev + 10;
        });
      }, 200);

      // const result = await fileService.convertFile(request);
      
      clearInterval(progressInterval);
      setUploadProgress(100);
      setUploadStatus(ConversionStatus.UPLOADED);
      // setResponse(result);
      
    } catch (err) {
      setUploadStatus(ConversionStatus.FAILED);
      setError(err instanceof Error ? err.message : 'Upload failed');
    }
  }, []);

  const reset = useCallback(() => {
    setUploadStatus(null);
    setUploadProgress(0);
    setError(null);
    setResponse(null);
  }, []);

  return {
    uploadFile,
    uploadStatus,
    uploadProgress,
    error,
    response,
    reset,
  };
};