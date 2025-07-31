import React, { useState } from 'react';
import { useDropzone, DropzoneRootProps, DropzoneInputProps } from 'react-dropzone';
import ReactGA from 'react-ga4';
import { useTranslation } from 'react-i18next';
import FormatSelector from './FormatSelector';
import FileStatusTracker from './FileStatusTracker';
import { ClipLoader } from 'react-spinners';
import { SUPPORTED_FORMATS, MAX_FILE_SIZE } from '../constants/file';
import { useFileUpload } from '../hooks/useFileUpload';
import { useFileConvert } from '../hooks/useFileConvert';
import UploadProgress from './UploadProgress';
import ErrorMessage from './ErrorMessage';

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
  const [fileStatus, setFileStatus] = useState<string | null>(null);
  const [localErrorMessage, setLocalErrorMessage] = useState<string>('');

  // Custom hooks
  const {
    uploadProgress,
    uploadStatus,
    errorMessage: uploadErrorMessage,
    fileId,
    handleUpload,
    resetUpload,
  } = useFileUpload();
  const {
    convertLoading,
    jobId,
    convertStatus,
    formatError,
    handleConvert,
    resetConvert,
    setConvertStatus,
  } = useFileConvert();

  // Reset tất cả state liên quan khi chọn file mới
  const resetAll = () => {
    setSelectedFile(null);
    setFileExtension(null);
    setTargetFormat(null);
    setFileStatus(null);
    resetUpload();
    resetConvert();
  };

  // Validate file (dùng chung cho onDrop và upload)
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

  // Handler for upload button
  const onUploadClick = async () => {
    if (selectedFile) {
      await handleUpload(selectedFile);
    }
  };

  // Handler for convert button
  const onConvertClick = async () => {
    if (fileId && targetFormat) {
      await handleConvert(fileId, targetFormat);
    }
  };

  // Gom lỗi upload, convert và local để hiển thị qua ErrorMessage
  const errorToShow = localErrorMessage
    ? localErrorMessage
    : uploadStatus === 'error' && uploadErrorMessage
      ? uploadErrorMessage
      : formatError
        ? formatError
        : '';

  return (
    <div className="max-w-md mx-auto p-4 sm:p-6 md:p-8 bg-white rounded-lg shadow-md mt-4">
      <h1 className="text-2xl font-bold text-center mb-4">{t('upload_title')}</h1>
      <div
        {...(getRootProps() as DropzoneRootProps)}
        className={`border-2 border-dashed p-6 sm:p-8 text-center rounded-lg cursor-pointer transition-colors duration-200 ${
          isDragActive ? 'border-blue-500 bg-blue-50' : 'border-gray-300 bg-gray-50'
        }`}
      >
        {/* Fix getInputProps typing issue by destructuring */}
        {(() => {
          const inputProps = getInputProps() as DropzoneInputProps;
          const { refKey, ...rest } = inputProps;
          return <input {...rest} />;
        })()}
        <p className="text-base sm:text-lg">
          {isDragActive
            ? t('drop_here')
            : t('drag_drop_or_click')}
        </p>
        <p className="text-xs sm:text-sm text-gray-500 mt-2">
          {t('supported_formats')}: {SUPPORTED_FORMATS.join(', ').toUpperCase()} ({t('max_file_size')})
        </p>
      </div>
      {selectedFile && (
        <div className="mt-4">
          <p className="text-base sm:text-lg">
            {t('selected_file')}: {selectedFile.name}
          </p>
          <button
            className="mt-4 bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600 disabled:bg-gray-400 w-full"
            onClick={onUploadClick}
            disabled={uploadStatus === 'uploading' || uploadStatus === 'completed'}
          >
            {uploadStatus === 'uploading' ? (
              <span className="flex items-center justify-center">
                <ClipLoader size={20} color="#fff" />
                {t('uploading')}
              </span>
            ) : uploadStatus === 'completed' ? (
              t('uploaded')
            ) : (
                              t('upload_btn')
            )}
          </button>
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
          disabled={convertLoading || fileStatus === 'UPLOADED'}
        />
        {/* Nút Chuyển đổi */}
        <button
          className="mt-4 bg-blue-500 text-white font-semibold px-4 py-3 rounded-lg w-full hover:bg-blue-600 disabled:bg-gray-300 disabled:text-gray-400 transition flex items-center justify-center"
          onClick={onConvertClick}
          disabled={
            !targetFormat ||
            !fileId ||
            convertLoading ||
            (!!fileStatus && fileStatus !== 'QUEUED_AND_VALIDATED') ||
            jobId !== null // Disable nếu đã gửi yêu cầu chuyển đổi
          }
        >
          {convertLoading ? (
            <span className="flex items-center justify-center">
              <ClipLoader size={20} color="#fff" />
              <span className="ml-2">{t('sending_request')}</span>
            </span>
          ) : convertStatus === 'SUCCESS' ? (
            t('convert_success')
          ) : convertStatus === 'FAILED' ? (
                          t('convert_failed')
          ) : jobId !== null ? (
            t('converting')
          ) : (
                          t('convert_btn')
          )}
        </button>
        {/* Loader trạng thái xác thực file */}
        {fileId && uploadStatus === 'completed' && (
          <div className="mt-6">
            <FileStatusTracker
              fileId={fileId}
              onStatusChange={(status) => {
                setFileStatus(status);
                if (status === 'SUCCESS' || status === 'FAILED') {
                  setConvertStatus(status);
                }
              }}
            />
          </div>
        )}
      </div>
      {/* Hiển thị mọi lỗi qua ErrorMessage */}
      {errorToShow && <ErrorMessage message={errorToShow} />}
    </div>
  );
};

export default FileConverter; 