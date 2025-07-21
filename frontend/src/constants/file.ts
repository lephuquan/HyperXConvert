// Định nghĩa các hằng số liên quan đến file
export const SUPPORTED_FORMATS = ['pdf', 'jpg', 'png', 'mp4', 'docx'] as const;
export const MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

export const ERROR_MESSAGES = {
  invalidResponse: 'invalid_response',
  convertError: 'convert_error',
  fileNotReady: 'file_not_ready',
  unsupportedConvert: 'unsupported_convert',
  unsupportedFormat: 'unsupported_format',
  fileTooLarge: 'file_too_large',
  uploadError: 'upload_error',
  s3Error: 's3_error',
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