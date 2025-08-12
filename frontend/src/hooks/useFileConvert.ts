import { useState } from 'react';
import axios from '../api/api';
import { toast } from 'react-toastify';
import { useTranslation } from 'react-i18next';
import { ERROR_MESSAGES, SUPPORTED_FORMATS, MAX_FILE_SIZE, FILE_STATUS } from '../constants/file';
import { UploadUrlResponse, FileStatus } from '../types/file';

export interface UseFileConvertResult {
  convertLoading: boolean;
  jobId: string | null;
  convertStatus: FileStatus | null;
  formatError: string;
  uploadProgress: number;
  uploadStatus: 'idle' | 'uploading' | 'completed' | 'error';
  fileId: string | null;
  handleConvert: (file: File, targetFormat: string) => Promise<void>;
  resetConvert: () => void;
  setConvertStatus: React.Dispatch<React.SetStateAction<FileStatus | null>>;
  resetConvertLoading: () => void;
}

export function useFileConvert(): UseFileConvertResult {
  const { t } = useTranslation();
  const [convertLoading, setConvertLoading] = useState(false);
  const [jobId, setJobId] = useState<string | null>(null);
  const [convertStatus, setConvertStatus] = useState<FileStatus | null>(null);
  const [formatError, setFormatError] = useState('');
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadStatus, setUploadStatus] = useState<'idle' | 'uploading' | 'completed' | 'error'>('idle');
  const [fileId, setFileId] = useState<string | null>(null);

  // Validate file
  const validateFile = (file: File): boolean => {
    const extension = file.name.split('.').pop()?.toLowerCase();
    if (!extension || !SUPPORTED_FORMATS.includes(extension as any)) {
      const msg = t('unsupported_format', ERROR_MESSAGES.unsupportedFormat);
      toast.error(msg);
      setFormatError(msg);
      return false;
    }
    if (file.size > MAX_FILE_SIZE) {
      const msg = t('file_too_large', ERROR_MESSAGES.fileTooLarge);
      toast.error(msg);
      setFormatError(msg);
      return false;
    }
    return true;
  };

  // Upload file to S3
  const uploadFile = async (file: File): Promise<string | null> => {
    setUploadStatus('uploading');
    setUploadProgress(0);
    
    try {
      const body = {
        fileName: file.name,
        fileSize: file.size,
        contentType: file.type,
      };
      const uploadResponse = await axios.post<UploadUrlResponse>('/file/upload-url', body);
      const { uploadUrl, fileId: uploadedFileId } = uploadResponse.data;

      if (!uploadUrl) {
        toast.error(t('no_upload_url', 'Không lấy được uploadUrl từ backend!'));
        setUploadStatus('error');
        return null;
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
      setFileId(uploadedFileId);
      setConvertStatus(FILE_STATUS.UPLOADED);
      toast.info(t('upload_success', 'File uploaded successfully!'));
      return uploadedFileId;
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
      setFormatError(msg);
      toast.error(msg);
      return null;
    }
  };

  const handleConvert = async (file: File, targetFormat: string) => {
    if (!targetFormat) {
      setFormatError(t('select_target_format'));
      return;
    }
    
    if (!validateFile(file)) return;
    
    setFormatError('');
    setConvertLoading(true);
    setJobId(null);
    setConvertStatus(null);
    
    try {
      // Step 1: Upload file to S3
      const uploadedFileId = await uploadFile(file);
      if (!uploadedFileId) {
        setConvertLoading(false);
        return;
      }
      
      // Step 2: Không gọi API convert ở đây nữa
      // API convert sẽ được gọi bởi FileConverter component khi status là VALIDATED
      // Chỉ set convertLoading = false để cho phép FileStatusTracker hoạt động
      setConvertLoading(false);
    } catch (error: any) {
      console.error('Error in handleConvert:', error);
      setConvertLoading(false);
    }
  };

  const resetConvert = () => {
    setConvertLoading(false);
    setJobId(null);
    setConvertStatus(null);
    setFormatError('');
    setUploadProgress(0);
    setUploadStatus('idle');
    setFileId(null);
  };

  const resetConvertLoading = () => {
    setConvertLoading(false);
  };

  return {
    convertLoading,
    jobId,
    convertStatus,
    formatError,
    uploadProgress,
    uploadStatus,
    fileId,
    handleConvert,
    resetConvert,
    setConvertStatus,
    resetConvertLoading,
  };
} 