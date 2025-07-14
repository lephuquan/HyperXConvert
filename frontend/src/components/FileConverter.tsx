import React, { useState } from 'react';
import { useDropzone } from 'react-dropzone';
import axios from '../api/api';
import { toast } from 'react-toastify';
import { ClipLoader } from 'react-spinners';
import ReactGA from 'react-ga4';
import { useTranslation } from 'react-i18next';
import FormatSelector from './FormatSelector';

interface FileConverterProps {}

const SUPPORTED_FORMATS = ['pdf', 'jpg', 'png', 'mp4', 'docx'];
const MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

const FileConverter: React.FC<FileConverterProps> = () => {
  const { t } = useTranslation();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number>(0);
  const [uploadStatus, setUploadStatus] = useState<'idle' | 'uploading' | 'completed' | 'error'>('idle');
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [fileExtension, setFileExtension] = useState<string | null>(null); // Lưu đuôi file
  const [targetFormat, setTargetFormat] = useState<string | null>(null); // Lưu định dạng đích
  const [formatError, setFormatError] = useState<string>(''); // Lỗi chọn định dạng
  const [fileId, setFileId] = useState<string | null>(null); // Lưu fileId sau upload
  const [convertLoading, setConvertLoading] = useState<boolean>(false); // Trạng thái loading khi chuyển đổi
  const [jobId, setJobId] = useState<string | null>(null); // Lưu jobId sau khi gửi convert
  const [convertStatus, setConvertStatus] = useState<string | null>(null); // Lưu trạng thái PROCESSING

  // Validate file
  const validateFile = (file: File): boolean => {
    const extension = file.name.split('.').pop()?.toLowerCase();
    if (!extension || !SUPPORTED_FORMATS.includes(extension)) {
      toast.error(t('unsupported_format', 'Định dạng file không được hỗ trợ. Vui lòng chọn PDF, JPG, PNG, MP4 hoặc DOCX'));
      return false;
    }
    if (file.size > MAX_FILE_SIZE) {
      toast.error(t('file_too_large', 'File quá lớn, tối đa 50MB'));
      return false;
    }
    return true;
  };

  // Dropzone
  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop: (acceptedFiles: Array<File & { path?: string }>) => {
      const file = acceptedFiles[0];
      if (file && validateFile(file)) {
        setSelectedFile(file);
        setFileExtension(file.name.split('.').pop()?.toLowerCase() || null); // Lưu đuôi file
        setUploadStatus('idle');
        setUploadProgress(0);
        setErrorMessage('');
        setTargetFormat(null); // Reset định dạng đích khi chọn file mới
        setFormatError('');
        // Google Analytics event
        ReactGA.event({ category: 'File', action: 'Upload', label: file.name });
      }
    },
    multiple: false,
    accept: {
      'application/pdf': ['.pdf'],
      'image/jpeg': ['.jpg', '.jpeg'],
      'image/png': ['.png'],
      'video/mp4': ['.mp4'],
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
    } as any,
  } as any);

  // Upload handler
  const handleUpload = async () => {
    if (!selectedFile) return;
    setUploadStatus('uploading');
    setUploadProgress(0);
    setErrorMessage('');
    try {
      const body = {
        fileName: selectedFile.name,
        fileSize: selectedFile.size,
        contentType: selectedFile.type,
      };
      const uploadResponse = await axios.post('/file/upload-url', body);
      console.log('uploadResponse:', uploadResponse.data);
      const { uploadUrl, fileId: uploadedFileId } = uploadResponse.data;
      console.log('uploadUrl:', uploadUrl);

      if (!uploadUrl) {
        toast.error('Không lấy được uploadUrl từ backend!');
        setUploadStatus('error');
        return;
      }

      if (uploadedFileId) {
        setFileId(uploadedFileId);
      }

      // Upload to S3
      await axios.put(uploadUrl, selectedFile, {
        headers: { 'Content-Type': selectedFile.type },
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
      let msg = t('upload_error', 'Lỗi upload, vui lòng thử lại');
      if (error?.response?.data?.error) {
        if (error.response.data.error === 'FILE_TOO_LARGE') {
          msg = t('file_too_large', 'File quá lớn, tối đa 50MB');
        } else if (error.response.data.error === 'UNSUPPORTED_FORMAT') {
          msg = t('unsupported_format', 'Định dạng file không được hỗ trợ. Vui lòng chọn PDF, JPG, PNG, MP4 hoặc DOCX');
        } else if (error.response.data.error === 'S3_ERROR') {
          msg = t('s3_error', 'Lỗi upload lên S3, vui lòng thử lại');
        } else {
          msg = error.response.data.error;
        }
      }
      setErrorMessage(msg);
      toast.error(msg);
    }
  };

  // Thêm hàm xử lý khi nhấn nút Chuyển đổi (giả lập, không gọi API ở bước này)
  const handleConvert = async () => {
    if (!fileId || !targetFormat) {
      setFormatError('Vui lòng chọn định dạng đích trước khi chuyển đổi.');
      return;
    }
    setFormatError('');
    setConvertLoading(true);
    setJobId(null);
    setConvertStatus(null);
    try {
      const payload = { fileId, targetFormat };
      const response = await axios.post('/file/convert', payload);
      if (response.data && response.data.jobId) {
        setJobId(response.data.jobId);
        setConvertStatus(response.data.status || 'PROCESSING');
        toast.success('Yêu cầu chuyển đổi đã được gửi. Đang xử lý...');
      } else {
        setFormatError('Không nhận được phản hồi hợp lệ từ hệ thống.');
      }
    } catch (error: any) {
      let msg = 'Lỗi chuyển đổi. Vui lòng thử lại.';
      if (error?.response?.data?.error_code) {
        switch (error.response.data.error_code) {
          case 'FILE_NOT_READY':
            msg = 'File chưa sẵn sàng để chuyển đổi. Vui lòng thử lại.';
            break;
          case 'UNSUPPORTED_FORMAT':
            msg = 'Định dạng chuyển đổi không được hỗ trợ.';
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

  return (
    <div className="max-w-md mx-auto p-4 sm:p-6 md:p-8 bg-white rounded-lg shadow-md mt-4">
      <h1 className="text-2xl font-bold text-center mb-4">{t('upload_title', 'Tải File Lên')}</h1>
      <div
        {...getRootProps()}
        className={`border-2 border-dashed p-6 sm:p-8 text-center rounded-lg cursor-pointer transition-colors duration-200 ${
          isDragActive ? 'border-blue-500 bg-blue-50' : 'border-gray-300 bg-gray-50'
        }`}
      >
        {/* Fix getInputProps typing issue by destructuring */}
        {(() => {
          const inputProps = getInputProps();
          // Remove refKey if present
          const { refKey, ...rest } = inputProps;
          return <input {...rest} />;
        })()}
        <p className="text-base sm:text-lg">
          {isDragActive
            ? t('drop_here', 'Thả file vào đây')
            : t('drag_drop_or_click', 'Kéo và thả file tại đây hoặc nhấp để chọn file')}
        </p>
        <p className="text-xs sm:text-sm text-gray-500 mt-2">
          {t('supported_formats', 'Định dạng hỗ trợ')}: PDF, JPG, PNG, MP4, DOCX (Tối đa 50MB)
        </p>
      </div>
      {selectedFile && (
        <div className="mt-4">
          <p className="text-base sm:text-lg">
            {t('selected_file', 'File đã chọn')}: {selectedFile.name}
          </p>
          <button
            className="mt-4 bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600 disabled:bg-gray-400 w-full"
            onClick={handleUpload}
            disabled={uploadStatus === 'uploading'}
          >
            {uploadStatus === 'uploading' ? (
              <span className="flex items-center justify-center">
                <ClipLoader size={20} color="#fff" />
                {t('uploading', 'Đang tải lên...')}
              </span>
            ) : (
              t('upload_btn', 'Tải lên')
            )}
          </button>
        </div>
      )}
      {uploadStatus === 'uploading' && (
        <div className="mt-4">
          <p>{t('uploading', 'Đang tải lên...')}</p>
          <div className="w-full bg-gray-200 rounded-full h-2.5 mt-2">
            <div
              className="bg-blue-600 h-2.5 rounded-full transition-all duration-200"
              style={{ width: `${uploadProgress}%` }}
            ></div>
          </div>
          <p className="text-sm mt-1">{uploadProgress}%</p>
        </div>
      )}
      {uploadStatus === 'completed' && selectedFile && (
        <div className="mt-6">
          {/* Component chọn định dạng chuyển đổi */}
          <FormatSelector
            fileExtension={fileExtension}
            onFormatChange={setTargetFormat}
            disabled={convertLoading}
          />
          {/* Nút Chuyển đổi */}
          <button
            className="mt-4 bg-blue-500 text-white font-semibold px-4 py-3 rounded-lg w-full hover:bg-blue-600 disabled:bg-gray-300 disabled:text-gray-400 transition flex items-center justify-center"
            onClick={handleConvert}
            disabled={!targetFormat || !fileId || convertLoading}
          >
            {convertLoading ? (
              <span className="flex items-center justify-center">
                <ClipLoader size={20} color="#fff" />
                <span className="ml-2">Đang gửi yêu cầu...</span>
              </span>
            ) : (
              'Chuyển đổi'
            )}
          </button>
          {/* Thông báo trạng thái hoặc lỗi */}
          {formatError && <p className="mt-2 text-sm text-red-600 text-center">{formatError}</p>}
          {jobId && (
            <div className="mt-4 text-blue-700 text-center text-base font-medium">
              Đang xử lý... (jobId: {jobId})
            </div>
          )}
        </div>
      )}
      {uploadStatus === 'error' && errorMessage && (
        <div className="mt-4 text-red-600">
          <p>{errorMessage}</p>
        </div>
      )}
    </div>
  );
};

export default FileConverter; 