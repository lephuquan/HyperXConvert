import { useState } from 'react';
import axios from '../api/api';
import { toast } from 'react-toastify';
import { useTranslation } from 'react-i18next';
import { SUPPORTED_FORMATS, MAX_FILE_SIZE, ERROR_MESSAGES } from '../constants/file';
import { UploadUrlResponse } from '../types/file';

export interface UseFileUploadResult {
  uploadProgress: number;
  uploadStatus: 'idle' | 'uploading' | 'completed' | 'error';
  errorMessage: string;
  fileId: string | null;
  handleUpload: (file: File) => Promise<void>;
  resetUpload: () => void;
}

export function useFileUpload(): UseFileUploadResult {
  const { t } = useTranslation();
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadStatus, setUploadStatus] = useState<'idle' | 'uploading' | 'completed' | 'error'>('idle');
  const [errorMessage, setErrorMessage] = useState('');
  const [fileId, setFileId] = useState<string | null>(null);

  // Validate file
  const validateFile = (file: File): boolean => {
    const extension = file.name.split('.').pop()?.toLowerCase();
    if (!extension || !SUPPORTED_FORMATS.includes(extension as any)) {
      toast.error(t('unsupported_format', ERROR_MESSAGES.unsupportedFormat));
      return false;
    }
    if (file.size > MAX_FILE_SIZE) {
      toast.error(t('file_too_large', ERROR_MESSAGES.fileTooLarge));
      return false;
    }
    return true;
  };

  // Upload handler
  const handleUpload = async (file: File) => {
    if (!validateFile(file)) return;
    setUploadStatus('uploading');
    setUploadProgress(0);
    setErrorMessage('');
    try {
      const body = {
        fileName: file.name,
        fileSize: file.size,
        contentType: file.type,
      };
      const uploadResponse = await axios.post<UploadUrlResponse>('/file/upload-url', body);
      const { uploadUrl, fileId: uploadedFileId } = uploadResponse.data;

      if (!uploadUrl) {
        toast.error('Không lấy được uploadUrl từ backend!');
        setUploadStatus('error');
        return;
      }

      if (uploadedFileId) {
        setFileId(uploadedFileId);
      }

      // Upload to S3
      await axios.put(uploadUrl, file, {
        headers: { 'Content-Type': file.type },
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total) {
            const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            setUploadProgress(percentCompleted);
          }
        },
      });
      setUploadStatus('completed');
      toast.success(t('upload_success', 'Upload thành công!'));
    } catch (error: any) {
      setUploadStatus('error');
      let msg = t('upload_error', ERROR_MESSAGES.uploadError);
      if (error?.response?.data?.error) {
        if (error.response.data.error === 'FILE_TOO_LARGE') {
          msg = t('file_too_large', ERROR_MESSAGES.fileTooLarge);
        } else if (error.response.data.error === 'UNSUPPORTED_FORMAT') {
          msg = t('unsupported_format', ERROR_MESSAGES.unsupportedFormat);
        } else if (error.response.data.error === 'S3_ERROR') {
          msg = t('s3_error', ERROR_MESSAGES.s3Error);
        } else {
          msg = error.response.data.error;
        }
      }
      setErrorMessage(msg);
      toast.error(msg);
    }
  };

  const resetUpload = () => {
    setUploadProgress(0);
    setUploadStatus('idle');
    setErrorMessage('');
    setFileId(null);
  };

  return {
    uploadProgress,
    uploadStatus,
    errorMessage,
    fileId,
    handleUpload,
    resetUpload,
  };
} 