// Định nghĩa các hằng số liên quan đến file
export const SUPPORTED_FORMATS = ['pdf', 'jpg', 'png', 'mp4', 'docx'] as const;
export const MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

export const ERROR_MESSAGES = {
  unsupportedFormat: 'Định dạng file không được hỗ trợ. Vui lòng chọn PDF, JPG, PNG, MP4 hoặc DOCX',
  fileTooLarge: 'File quá lớn, tối đa 50MB',
  uploadError: 'Lỗi upload, vui lòng thử lại',
  s3Error: 'Lỗi upload lên S3, vui lòng thử lại',
  convertError: 'Lỗi chuyển đổi. Vui lòng thử lại.',
  fileNotReady: 'File chưa sẵn sàng để chuyển đổi. Vui lòng thử lại.',
  unsupportedConvert: 'Định dạng chuyển đổi không được hỗ trợ.',
  invalidResponse: 'Không nhận được phản hồi hợp lệ từ hệ thống.',
};

export type FormatOption = { label: string; value: string };

export const formatOptionsMap: Record<string, FormatOption[]> = {
  pdf: [
    { label: 'PDF sang Word', value: 'DOCX' },
    { label: 'Nén PDF', value: 'COMPRESSED_PDF' },
  ],
  docx: [
    { label: 'Word sang PDF', value: 'PDF' },
  ],
  jpg: [
    { label: 'JPG sang PNG', value: 'PNG' },
  ],
  png: [
    { label: 'PNG sang JPG', value: 'JPG' },
  ],
  mp4: [
    { label: 'MP4 sang MP3', value: 'MP3' },
    { label: 'Nén video', value: 'COMPRESSED_VIDEO' },
  ],
}; 