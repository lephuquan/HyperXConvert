// Định nghĩa type cho SUPPORTED_FORMATS
export type SupportedFormat = 'pdf' | 'jpg' | 'png' | 'mp4' | 'docx';

// Response từ API upload-url
export interface UploadUrlResponse {
  uploadUrl: string;
  fileId: string;
}

// Response từ API convert
export interface ConvertResponse {
  jobId: string;
  status: string; // Có thể refine thêm nếu có enum cụ thể
} 