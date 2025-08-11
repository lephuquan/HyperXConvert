import React, { useState, useEffect } from 'react';
import { useDropzone, DropzoneRootProps, DropzoneInputProps } from 'react-dropzone';
import ReactGA from 'react-ga4';
import { useTranslation } from 'react-i18next';
import FormatSelector from './FormatSelector';
import FileStatusTracker from './FileStatusTracker';
import { ClipLoader } from 'react-spinners';
import { SUPPORTED_FORMATS, MAX_FILE_SIZE, FILE_STATUS } from '../constants/file';
import { useFileConvert } from '../hooks/useFileConvert';
import UploadProgress from './UploadProgress';
import ErrorMessage from './ErrorMessage';
import { FileStatus } from '../types/file';
import axios from '../api/api';

const ACCEPT_MIME: Record<string, string[]> = {
  'application/pdf': ['.pdf'],
  'image/jpeg': ['.jpg', '.jpeg'],
  'image/png': ['.png'],
  'video/mp4': ['.mp4'],
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
};

const FileConverter: React.FC = () => {
  const { t } = useTranslation();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileExtension, setFileExtension] = useState<string | null>(null);
  const [targetFormat, setTargetFormat] = useState<string | null>(null);
  const [fileStatus, setFileStatus] = useState<FileStatus | null>(null);
  const [localErrorMessage, setLocalErrorMessage] = useState<string>('');

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

  // Reset tất cả state liên quan khi chọn file mới
  const resetAll = () => {
    setSelectedFile(null);
    setFileExtension(null);
    setTargetFormat(null);
    setFileStatus(null);
    setLocalErrorMessage('');
    resetConvert();
  };

  // Validate file (dùng chung cho onDrop)
  const validateFile = (file: File): boolean => {
    const extension = file.name.split('.').pop()?.toLowerCase();
    if (!extension || !SUPPORTED_FORMATS.includes(extension as any)) {
      const msg = t('unsupported_format');
      setLocalErrorMessage(msg);
      return false;
    }
    if (file.size > MAX_FILE_SIZE) {
      const msg = t('file_too_large');
      setLocalErrorMessage(msg);
      return false;
    }
    setLocalErrorMessage('');
    return true;
  };

  // Dropzone
  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop: (acceptedFiles: File[]) => {
      const file = acceptedFiles[0];
      if (file) {
        if (!validateFile(file)) {
          resetAll(); // clear các state liên quan nếu có file lỗi
          return;
        }
        resetAll();
        setSelectedFile(file);
        setFileExtension(file.name.split('.').pop()?.toLowerCase() || null);
        // Google Analytics event
        ReactGA.event({ category: 'File', action: 'Upload', label: file.name });
      }
    },
    multiple: false,
    accept: ACCEPT_MIME,
  });

  // Handler for convert button - giờ sẽ xử lý cả upload và convert
  const onConvertClick = async () => {
    if (selectedFile && targetFormat) {
      // Google Analytics event
      ReactGA.event({ 
        category: 'Conversion', 
        action: 'Start', 
        label: `${selectedFile.name} -> ${targetFormat}` 
      });
      await handleConvert(selectedFile, targetFormat);
    }
  };

  // Effect để gọi API convert khi status là VALIDATED
  useEffect(() => {
    const callConvertAPI = async () => {
      if (fileStatus === FILE_STATUS.VALIDATED && fileId && targetFormat && !jobId) {
        try {
          const payload = { fileId, targetFormat };
          const response = await axios.post('/file/convert', payload);
          if (response.data && response.data.jobId) {
            setConvertStatus(response.data.status);
            // Google Analytics event
            ReactGA.event({ 
              category: 'Conversion', 
              action: 'ConvertRequest', 
              label: `${selectedFile?.name} -> ${targetFormat}` 
            });
          }
        } catch (error: any) {
          console.error('Error calling convert API:', error);
          let msg = t('convert_error');
          if (error?.response?.data?.message) {
            msg = error.response.data.message;
          }
          setLocalErrorMessage(msg);
        }
      }
    };

    callConvertAPI();
  }, [fileStatus, fileId, targetFormat, jobId, setConvertStatus, t, selectedFile]);

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
    (fileStatus !== FILE_STATUS.VALIDATION_FAILED && 
     fileStatus !== FILE_STATUS.CONVERSION_FAILED);

  return (
    <div className="mt-4 max-w-md mx-auto p-[2px] bg-gradient-to-r from-[#00FFC6]/50 to-[#5B00FF]/50 rounded-lg shadow-md">
      <div className="lg:w-full p-[1rem] sm:p-6 md:p-8 bg-white rounded-lg dark:bg-black dark:shadow-slate-300/40 dark:text-white">
        <h1 className="text-2xl font-bold text-center mb-4 dark:text-gray-300">{t('upload_title')}</h1>
        <div
          {...(getRootProps() as DropzoneRootProps)}
          className={`border-2 border-dashed p-6 sm:p-8 text-center hover:bg-[linear-gradient(90deg,rgba(0,255,198,0.2)_0%,rgba(255,255,255,0.2)_50%,rgba(91,0,255,0.2)_100%)] rounded-lg cursor-pointer transition-colors duration-200 dark:text-gray-300 ${
            isDragActive ? 'border-emerald-500 bg-[linear-gradient(90deg,rgba(0,255,198,0.2)_0%,rgba(255,255,255,0.2)_50%,rgba(91,0,255,0.2)_100%)]' : 'border-emerald-200 dark:border-gray-500 dark:bg-[#64748b]/5 bg-gray-50'
          }`}
        >
          {/* Fix getInputProps typing issue by destructuring */}
          {(() => {
            const inputProps = getInputProps() as DropzoneInputProps;
            const { refKey, ...rest } = inputProps;
            return <input {...rest} />;
          })()}
          <p className="text-base sm:text-lg ">
            {isDragActive
              ? t('drop_here')
              : t('drag_drop_or_click')}
          </p>
          <p className="text-xs sm:text-sm text-gray-500 mt-2 dark:text-gray-300">
            {t('supported_formats')}: {SUPPORTED_FORMATS.join(', ').toUpperCase()} ({t('max_file_size')})
          </p>
        </div>
        {selectedFile && (
          <div className="mt-4">
            <p className="text-base sm:text-lg ">
              {t('selected_file')}: {selectedFile.name}
            </p>
          </div>
        )}
        {uploadStatus === 'uploading' && (
          <UploadProgress progress={uploadProgress} />
        )}
        <div className="mt-6">
          {/* Component chọn định dạng chuyển đổi */}
          <FormatSelector
            fileExtension={fileExtension}
            onFormatChange={setTargetFormat}
            disabled={convertLoading || uploadStatus === 'uploading' || jobId !== null || (!!fileId && uploadStatus === 'completed')}
          />
          {/* Nút Chuyển đổi - giờ sẽ xử lý cả upload và convert */}
          <button
            className={`mt-4 px-4 py-3 font-medium rounded-lg w-full transition flex items-center justify-center
              ${selectedFile && targetFormat && fileStatus !== FILE_STATUS.CONVERTED
                ? 'bg-cyber-gradient hover:bg-cyber-hover text-white drop-shadow-[0_0_6px_rgba(255,255,255,0.6)] dark:drop-shadow-[0_0_6px_rgb(9_242_29/30%)]'
                : 'bg-gray-300 dark:bg-gray-900 text-gray-400 cursor-not-allowed'}
            `}
            onClick={onConvertClick}
            disabled={shouldDisableConvertButton || fileStatus === FILE_STATUS.CONVERTED}
          >
            {convertLoading || uploadStatus === 'uploading' ? (
              <span className="flex items-center justify-center">
              <ClipLoader size={20} color="#fff" />
              <span className="ml-2">
                {uploadStatus === 'uploading' ? t('uploading') : t('sending_request')}
              </span>
            </span>
            ) : fileStatus === FILE_STATUS.CONVERTED ? (
              t('convert_success')
            ) : fileStatus === FILE_STATUS.VALIDATION_FAILED || fileStatus === FILE_STATUS.CONVERSION_FAILED ? (
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
                  if (status === FILE_STATUS.CONVERTED ||
                    status === FILE_STATUS.VALIDATION_FAILED ||
                    status === FILE_STATUS.CONVERSION_FAILED) {
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
