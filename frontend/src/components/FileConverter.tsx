import React, { useState, useEffect } from 'react';
import {
  useDropzone,
  DropzoneRootProps,
  DropzoneInputProps,
} from 'react-dropzone';
import ReactGA from 'react-ga4';
import { useTranslation } from 'react-i18next';
import FormatSelector from './FormatSelector';
import FileStatusTracker from './FileStatusTracker';
import { ClipLoader } from 'react-spinners';
import {
  SUPPORTED_FORMATS,
  MAX_FILE_SIZE,
  FILE_STATUS,
} from '../constants/file';
import { useFileConvert } from '../hooks/useFileConvert';
import UploadProgress from './UploadProgress';
import ErrorMessage from './ErrorMessage';
import { FileStatus } from '../types/file';
import axios from '../api/api';
import { useCustomToast } from './toast';

const ACCEPT_MIME: Record<string, string[]> = {
  'application/pdf': ['.pdf'],
  'image/jpeg': ['.jpg', '.jpeg'],
  'image/png': ['.png'],
  'video/mp4': ['.mp4'],
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': [
    '.docx',
  ],
};

interface FileConverterProps {
  onUserInteract?: () => void;
  onTimelineStateChange?: (state: {
    selectedFile: File | null;
    uploadStatus: string;
    fileStatus: string | null;
  }) => void;
  selectedFormat?: { from: string; to: string } | null;
  resetTrigger?: number;
  onReset?: (opts?: { force?: boolean }) => void; // Sửa lại kiểu cho phép truyền opts
  onFileIdChange?: (fileId: string | null) => void; // Thêm prop onFileIdChange
  onFileInfoChange?: (info: { ext: string; size: string } | null) => void; // Thêm prop onFileInfoChange
  onCompletedTimeChange?: (time: string) => void; // Thêm prop này
}

const FileConverter: React.FC<FileConverterProps> = ({
  onUserInteract,
  onTimelineStateChange,
  selectedFormat,
  resetTrigger,
  onReset,
  onFileIdChange,
  onFileInfoChange,
  onCompletedTimeChange,
}) => {
  const { t } = useTranslation();
  const toast = useCustomToast(); // Sử dụng custom toast hook
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileExtension, setFileExtension] = useState<string | null>(null);
  const [targetFormat, setTargetFormat] = useState<string | null>(null);
  const [fileStatus, setFileStatus] = useState<FileStatus | null>(null);
  const [localErrorMessage, setLocalErrorMessage] = useState<string>('');

  // State để kiểm soát toast lỗi file sai định dạng
  const [hasShownInvalidFormatToast, setHasShownInvalidFormatToast] =
    useState(false);

  // Custom hooks - chỉ sử dụng useFileConvert mới
  const {
    convertLoading,
    jobId,
    formatError,
    uploadProgress,
    uploadStatus,
    fileId,
    handleConvert,
    resetConvert,
    setConvertStatus,
    resetConvertLoading,
  } = useFileConvert();

  // Reset chỉ file, status, error (KHÔNG reset fileExtension, targetFormat)
  const resetFileOnly = () => {
    setSelectedFile(null);
    setFileStatus(null);
    setLocalErrorMessage('');
    resetConvert();
    setCompletedTime('--');
    if (onCompletedTimeChange) onCompletedTimeChange('--');
  };

  // Reset tất cả state liên quan khi chọn file mới hoặc đổi format
  const resetAll = () => {
    setSelectedFile(null);
    setFileExtension(null);
    setFileStatus(null);
    setLocalErrorMessage('');
    resetConvert();
    setCompletedTime('--');
    if (onCompletedTimeChange) onCompletedTimeChange('--');
    // Chỉ reset targetFormat nếu selectedFormat là null
    if (!selectedFormat) {
      setTargetFormat(null);
    }
  };

  // Reset state khi resetTrigger thay đổi
  React.useEffect(() => {
    setSelectedFile(null);
    setFileExtension(null);
    setTargetFormat(null);
    setFileStatus(null);
    setLocalErrorMessage('');
    setHasShownInvalidFormatToast(false);
  }, [resetTrigger]);

  // Luôn đồng bộ selectedFormat, nhưng nếu resetTrigger vừa thay đổi thì bỏ qua effect này
  const prevResetTrigger = React.useRef(resetTrigger);
  useEffect(() => {
    if (prevResetTrigger.current !== resetTrigger) {
      prevResetTrigger.current = resetTrigger;
      return;
    }
    if (selectedFormat) {
      setFileExtension(selectedFormat.from);
      setTargetFormat(selectedFormat.to);
    } else {
      setFileExtension(null);
      setTargetFormat(null);
    }
  }, [selectedFormat, resetTrigger]);

  // Reset toast khi đổi format hoặc file
  useEffect(() => {
    setHasShownInvalidFormatToast(false);
  }, [selectedFormat, selectedFile]);

  // Validate file (dùng chung cho onDrop)
  const validateFile = (file: File): boolean => {
    const extension = file.name.split('.').pop()?.toLowerCase();
    // Nếu đã chọn format, chỉ nhận đúng định dạng from
    if (selectedFormat && extension !== selectedFormat.from.toLowerCase()) {
      const msg = t('invalid_file_format', { format: selectedFormat.from });
      setLocalErrorMessage(msg);
      if (!hasShownInvalidFormatToast) {
        toast.error(msg);
        setHasShownInvalidFormatToast(true);
      }
      // Gọi hàm onReset với opts.force=true để không hiện toast.info khi reset do lỗi
      if (typeof onReset === 'function') {
        onReset({ force: true });
      }
      return false;
    }
    if (!extension || !SUPPORTED_FORMATS.includes(extension as any)) {
      const msg = t('unsupported_format');
      setLocalErrorMessage(msg);
      toast.error(msg);
      return false;
    }
    if (file.size > MAX_FILE_SIZE) {
      const msg = t('file_too_large');
      setLocalErrorMessage(msg);
      toast.error(msg);
      return false;
    }
    setLocalErrorMessage('');
    return true;
  };

  // Helper to format file size
  const formatFileSize = (size: number) => {
    if (size >= 1024 * 1024) return (size / (1024 * 1024)).toFixed(2) + ' MB';
    if (size >= 1024) return (size / 1024).toFixed(2) + ' KB';
    return size + ' B';
  };

  // State for file info to pass to ConversionInfoTable
  const [fileInfo, setFileInfo] = useState<{
    ext: string;
    size: string;
  } | null>(null);

  // Timer for conversion duration
  const [convertTimer, setConvertTimer] = useState<number>(0); // seconds
  const convertTimerRef = React.useRef<number>(0); // always holds latest value
  const [timerInterval, setTimerInterval] = useState<NodeJS.Timeout | null>(
    null
  );
  const [completedTime, setCompletedTime] = useState<string>('-- s');

  // Update ref whenever timer changes
  useEffect(() => {
    convertTimerRef.current = convertTimer;
  }, [convertTimer]);

  // Format seconds to mm:ss
  const formatDuration = (secs: number) => {
    const m = Math.floor(secs / 60)
      .toString()
      .padStart(2, '0');
    const s = (secs % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  };

  // Dropzone
  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop: (acceptedFiles: File[]) => {
      const file = acceptedFiles[0];
      if (file) {
        if (!validateFile(file)) {
          resetFileOnly();
          setFileInfo(null);
          return;
        }
        resetAll();
        setSelectedFile(file);
        const ext = file.name.split('.').pop()?.toUpperCase() || '';
        const size = formatFileSize(file.size);
        setFileInfo({ ext, size });
        setFileExtension(file.name.split('.').pop()?.toLowerCase() || null);
        ReactGA.event({ category: 'File', action: 'Upload', label: file.name });
        onUserInteract?.();
      } else {
        setFileInfo(null);
      }
    },
    multiple: false,
    accept: ACCEPT_MIME,
  });

  // Handler for convert button - giờ sẽ xử lý cả upload và convert
  const onConvertClick = async () => {
    if (selectedFile && targetFormat) {
      // Chỉ gọi handleConvert, KHÔNG khởi động timer ở đây nữa
      setConvertTimer(0);
      convertTimerRef.current = 0;
      setCompletedTime('--');
      if (onCompletedTimeChange) onCompletedTimeChange('--');
      if (timerInterval) clearInterval(timerInterval);
      // Google Analytics event
      ReactGA.event({
        category: 'Conversion',
        action: 'Start',
        label: `${selectedFile.name} -> ${targetFormat}`,
      });
      await handleConvert(selectedFile, targetFormat);
    }
  };

  // Start timer when status is UPLOADED, stop when CONVERTED
  useEffect(() => {
    if (fileStatus === FILE_STATUS.UPLOADED) {
      setConvertTimer(0);
      convertTimerRef.current = 0;
      setCompletedTime('--');
      if (onCompletedTimeChange) onCompletedTimeChange('--');
      if (timerInterval) clearInterval(timerInterval);
      const interval = setInterval(() => {
        setConvertTimer((prev) => {
          const next = prev + 1;
          convertTimerRef.current = next;
          return next;
        });
      }, 1000);
      setTimerInterval(interval);
    } else if (fileStatus === FILE_STATUS.CONVERTED) {
      if (timerInterval) {
        clearInterval(timerInterval);
        setTimerInterval(null);
      }
      let timeVal = convertTimerRef.current;
      if (timeVal === 0) timeVal = 1; // Nếu quá nhanh, tối thiểu 1s
      const timeStr = formatDuration(timeVal);
      setCompletedTime(timeStr);
      if (onCompletedTimeChange) onCompletedTimeChange(timeStr);
    } else if (
      fileStatus === FILE_STATUS.VALIDATION_FAILED ||
      fileStatus === FILE_STATUS.CONVERSION_FAILED ||
      fileStatus === null
    ) {
      setCompletedTime('--');
      if (onCompletedTimeChange) onCompletedTimeChange('--');
      if (timerInterval) {
        clearInterval(timerInterval);
        setTimerInterval(null);
      }
    }
  }, [fileStatus]);

  // Effect để gọi API convert khi status là VALIDATED
  useEffect(() => {
    const callConvertAPI = async () => {
      if (
        fileStatus === FILE_STATUS.VALIDATED &&
        fileId &&
        targetFormat &&
        !jobId
      ) {
        try {
          const payload = { fileId, targetFormat };
          const response = await axios.post('/file/convert', payload);
          if (response.data && response.data.jobId) {
            setConvertStatus(response.data.status);
            // Google Analytics event
            ReactGA.event({
              category: 'Conversion',
              action: 'ConvertRequest',
              label: `${selectedFile?.name} -> ${targetFormat}`,
            });
          }
        } catch (error: any) {
          console.error('Error calling convert API:', error);
          let msg = t('convert_error');
          if (error?.response?.data?.message) {
            msg = error.response.data.message;
          }
          setLocalErrorMessage(msg);
          // Sử dụng toast từ hệ thống toast tùy chỉnh
          toast.error(msg);
        }
      }
    };

    callConvertAPI();
  }, [
    fileStatus,
    fileId,
    targetFormat,
    jobId,
    setConvertStatus,
    t,
    selectedFile,
  ]);

  // Gom lỗi convert và local để hiển thị qua ErrorMessage
  const errorToShow = localErrorMessage
    ? localErrorMessage
    : formatError
    ? formatError
    : '';

  // Kiểm tra xem có nên disable nút convert không
  const shouldDisableConvertButton =
    !targetFormat ||
    !selectedFile ||
    convertLoading ||
    uploadStatus === 'uploading' ||
    jobId !== null || // Disable nếu đã gửi yêu cầu chuyển đổi
    (!!fileId && uploadStatus === 'completed') || // Disable nếu đã upload và đang chờ xử lý
    fileStatus === FILE_STATUS.VALIDATION_FAILED || // Disable nếu validation failed
    fileStatus === FILE_STATUS.CONVERSION_FAILED; // Disable nếu conversion failed

  // Kiểm tra xem có nên hiển thị FileStatusTracker không
  const shouldShowStatusTracker =
    fileId &&
    uploadStatus === 'completed' &&
    fileStatus !== FILE_STATUS.VALIDATION_FAILED &&
    fileStatus !== FILE_STATUS.CONVERSION_FAILED;

  // Notify timeline state on any relevant change
  useEffect(() => {
    if (onTimelineStateChange) {
      onTimelineStateChange({
        selectedFile,
        uploadStatus,
        fileStatus,
      });
    }
  }, [selectedFile, uploadStatus, fileStatus, onTimelineStateChange]);

  // Notify parent component of fileId changes
  useEffect(() => {
    if (onFileIdChange) {
      onFileIdChange(fileId);
    }
  }, [fileId, onFileIdChange]);

  // Notify parent component of fileInfo changes
  useEffect(() => {
    if (typeof onFileInfoChange === 'function') {
      onFileInfoChange(fileInfo);
    }
  }, [fileInfo, onFileInfoChange]);

  // Whenever completedTime changes, notify parent
  useEffect(() => {
    if (onCompletedTimeChange) {
      onCompletedTimeChange(completedTime);
    }
  }, [completedTime, onCompletedTimeChange]);

  return (
    <div className="mt-4 max-w-md mx-auto p-[2px] bg-gradient-to-r from-[#00FFC6]/50 to-[#5B00FF]/50 rounded-lg shadow-md">
      <div className="lg:w-full p-[1rem] sm:p-6 md:p-8 bg-white rounded-lg dark:bg-black dark:shadow-slate-300/40 dark:text-white">
        <h1 className="text-2xl font-bold text-center mb-4 dark:text-gray-300">
          {t('upload_title')}
        </h1>
        <div
          {...(getRootProps() as DropzoneRootProps)}
          className={`border-2 border-dashed p-6 sm:p-8 text-center hover:bg-[linear-gradient(90deg,rgba(0,255,198,0.2)_0%,rgba(255,255,255,0.2)_50%,rgba(91,0,255,0.2)_100%)] rounded-lg cursor-pointer transition-colors duration-200 dark:text-gray-300 ${
            isDragActive
              ? 'border-emerald-500 bg-[linear-gradient(90deg,rgba(0,255,198,0.2)_0%,rgba(255,255,255,0.2)_50%,rgba(91,0,255,0.2)_100%)]'
              : 'border-emerald-200 dark:border-gray-500 dark:bg-[#64748b]/5 bg-gray-50'
          }`}
        >
          {/* Fix getInputProps typing issue by destructuring */}
          {(() => {
            const inputProps = getInputProps() as DropzoneInputProps;
            const { refKey, ...rest } = inputProps;
            return (
              <input
                {...rest}
                onChange={(e) => {
                  if (rest.onChange) rest.onChange(e);
                }}
              />
            );
          })()}
          <p className="text-base sm:text-lg ">
            {isDragActive ? t('drop_here') : t('drag_drop_or_click')}
          </p>
          <p className="text-xs sm:text-sm text-gray-500 mt-2 dark:text-gray-300">
            {t('supported_formats')}:{' '}
            {SUPPORTED_FORMATS.join(', ').toUpperCase()} ({t('max_file_size')})
          </p>
        </div>
        {/* Pass fileInfo to children via context or props if needed */}
        {selectedFile && (
          <div className="mt-4">
            <p className="text-base sm:text-lg ">
              {t('selected_file')}:{' '}
              <span className="break-words text-transparent bg-clip-text bg-gradient-to-r from-green-400 via-blue-500 to-purple-600 text-ellipsis">
                {selectedFile.name}
              </span>
            </p>
          </div>
        )}
        {uploadStatus === 'uploading' && (
          <UploadProgress progress={uploadProgress} />
        )}
        <div className="mt-6">
          {/* Component chọn định dạng chuyển đổi */}
          {fileExtension && (
            <FormatSelector
              fileExtension={fileExtension}
              onFormatChange={(to) => {
                setTargetFormat(to);
                if (fileExtension && to) {
                  // Cập nhật selectedFormat ở Layout qua prop nếu có
                  if (typeof window !== 'undefined' && window.dispatchEvent) {
                    // Gửi custom event để Layout cập nhật selectedFormat
                    window.dispatchEvent(
                      new CustomEvent('updateSelectedFormat', {
                        detail: { from: fileExtension, to },
                      })
                    );
                  }
                }
              }}
              disabled={
                convertLoading ||
                uploadStatus === 'uploading' ||
                jobId !== null ||
                (!!fileId && uploadStatus === 'completed')
              }
              value={targetFormat} // Truyền targetFormat vào prop value
            />
          )}
          {/* Nút Chuyển đổi - giờ sẽ xử lý cả upload v�� convert */}
          <button
            className={`mt-4 px-4 py-3 font-medium rounded-lg w-full transition flex items-center justify-center
              ${
                selectedFile &&
                targetFormat &&
                fileStatus !== FILE_STATUS.CONVERTED
                  ? 'bg-cyber-gradient hover:bg-cyber-hover text-white drop-shadow-[0_0_6px_rgba(255,255,255,0.6)] dark:drop-shadow-[0_0_6px_rgb(9_242_29/30%)]'
                  : 'bg-gray-300 dark:bg-gray-900 text-gray-400 cursor-not-allowed'
              }
            `}
            onClick={onConvertClick}
            disabled={
              shouldDisableConvertButton || fileStatus === FILE_STATUS.CONVERTED
            }
          >
            {convertLoading || uploadStatus === 'uploading' ? (
              <span className="flex items-center justify-center">
                <ClipLoader size={20} color="#fff" />
                <span className="ml-2">
                  {uploadStatus === 'uploading'
                    ? t('uploading')
                    : t('sending_request')}
                </span>
              </span>
            ) : fileStatus === FILE_STATUS.CONVERTED ? (
              t('convert_success')
            ) : fileStatus === FILE_STATUS.VALIDATION_FAILED ||
              fileStatus === FILE_STATUS.CONVERSION_FAILED ? (
              t('convert_failed')
            ) : jobId !== null || (!!fileId && uploadStatus === 'completed') ? (
              t('converting')
            ) : (
              t('convert_btn')
            )}
          </button>
          {/* Loader trạng thái xác thực file */}
          {shouldShowStatusTracker && (
            <div className="mt-6">
              <FileStatusTracker
                fileId={fileId}
                onStatusChange={(status) => {
                  setFileStatus(status);
                  if (
                    status === FILE_STATUS.CONVERTED ||
                    status === FILE_STATUS.VALIDATION_FAILED ||
                    status === FILE_STATUS.CONVERSION_FAILED
                  ) {
                    setConvertStatus(status);
                    // Reset convertLoading khi có kết quả cuối cùng
                    resetConvertLoading();
                  }
                }}
              />
            </div>
          )}
        </div>
        {/* Hiển thị mọi lỗi qua ErrorMessage */}
        {errorToShow && <ErrorMessage message={errorToShow} />}
      </div>
    </div>
  );
};

export default FileConverter;
