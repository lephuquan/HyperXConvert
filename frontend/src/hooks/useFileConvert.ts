import { useState } from 'react';
import axios from '../api/api';
import { toast } from 'react-toastify';
import { useTranslation } from 'react-i18next';
import { ERROR_MESSAGES } from '../constants/file';
import { ConvertResponse } from '../types/file';

export interface UseFileConvertResult {
  convertLoading: boolean;
  jobId: string | null;
  convertStatus: string | null;
  formatError: string;
  handleConvert: (fileId: string, targetFormat: string) => Promise<void>;
  resetConvert: () => void;
  setConvertStatus: React.Dispatch<React.SetStateAction<string | null>>;
}

export function useFileConvert(): UseFileConvertResult {
  const { t } = useTranslation();
  const [convertLoading, setConvertLoading] = useState(false);
  const [jobId, setJobId] = useState<string | null>(null);
  const [convertStatus, setConvertStatus] = useState<string | null>(null);
  const [formatError, setFormatError] = useState('');

  const handleConvert = async (fileId: string, targetFormat: string) => {
    if (!fileId || !targetFormat) {
      setFormatError(t('select_target_format'));
      return;
    }
    setFormatError('');
    setConvertLoading(true);
    setJobId(null);
    setConvertStatus(null);
    try {
      const payload = { fileId, targetFormat };
      const response = await axios.post<ConvertResponse>('/file/convert', payload);
      if (response.data && response.data.jobId) {
        setJobId(response.data.jobId);
        setConvertStatus(response.data.status || 'PROCESSING');
        toast.success(t('convert_request_sent'));
      } else {
        setFormatError(t(ERROR_MESSAGES.invalidResponse));
      }
    } catch (error: any) {
      let msg = t(ERROR_MESSAGES.convertError);
      if (error?.response?.data?.error_code) {
        switch (error.response.data.error_code) {
          case 'FILE_NOT_READY':
            msg = t(ERROR_MESSAGES.fileNotReady);
            break;
          case 'UNSUPPORTED_FORMAT':
            msg = t(ERROR_MESSAGES.unsupportedConvert);
            break;
          default:
            msg = error.response.data.message || msg;
        }
      } else if (error?.response?.data?.message) {
        msg = error.response.data.message;
      }
      setFormatError(msg);
      toast.error(msg);
    } finally {
      setConvertLoading(false);
    }
  };

  const resetConvert = () => {
    setConvertLoading(false);
    setJobId(null);
    setConvertStatus(null);
    setFormatError('');
  };

  return {
    convertLoading,
    jobId,
    convertStatus,
    formatError,
    handleConvert,
    resetConvert,
    setConvertStatus,
  };
} 