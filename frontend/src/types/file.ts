// Định nghĩa type cho SUPPORTED_FORMATS
export type SupportedFormat = 'pdf' | 'jpg' | 'png' | 'mp4' | 'docx';

// Định nghĩa các status mới từ BE
export type FileStatus = 
  | 'UPLOADED'
  | 'VALIDATING'
  | 'VALIDATION_FAILED'
  | 'VALIDATED'
  | 'CONVERTING'
  | 'CONVERSION_FAILED'
  | 'CONVERTED';

// Response từ API upload-url
export interface UploadUrlResponse {
  uploadUrl: string;
  fileId: string;
}

// Response từ API convert
export interface ConvertResponse {
  jobId: string;
  status: FileStatus;
}

// Response từ API status
export interface StatusResponse {
  status: FileStatus;
  downloadUrl?: string;
  message?: string;
} 