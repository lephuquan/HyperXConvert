// Định nghĩa các hằng số liên quan đến file
export const SUPPORTED_FORMATS = ['pdf', 'jpg', 'png', 'mp4', 'docx'] as const;
export const MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

// Định nghĩa các status mới từ BE
export const FILE_STATUS = {
  UPLOADED: 'UPLOADED',
  VALIDATING: 'VALIDATING',
  VALIDATION_FAILED: 'VALIDATION_FAILED',
  VALIDATED: 'VALIDATED',
  CONVERTING: 'CONVERTING',
  CONVERSION_FAILED: 'CONVERSION_FAILED',
  CONVERTED: 'CONVERTED',
} as const;

// Mapping status để hiển thị UI
export const STATUS_DISPLAY_MAP = {
  [FILE_STATUS.UPLOADED]: 'uploaded',
  [FILE_STATUS.VALIDATING]: 'validating',
  [FILE_STATUS.VALIDATION_FAILED]: 'validation_failed',
  [FILE_STATUS.VALIDATED]: 'validated',
  [FILE_STATUS.CONVERTING]: 'converting',
  [FILE_STATUS.CONVERSION_FAILED]: 'conversion_failed',
  [FILE_STATUS.CONVERTED]: 'converted',
} as const;

export const ERROR_MESSAGES = {
  invalidResponse: 'invalid_response',
  convertError: 'convert_error',
  fileNotReady: 'file_not_ready',
  unsupportedConvert: 'unsupported_convert',
  unsupportedFormat: 'unsupported_format',
  fileTooLarge: 'file_too_large',
  uploadError: 'upload_error',
  s3Error: 's3_error',
  validationFailed: 'validation_failed',
  conversionFailed: 'conversion_failed',
};

export type FormatOption = { label: string; value: string };

export const formatOptionsMap: Record<string, FormatOption[]> = {
  pdf: [
    { label: 'format_pdf_to_word', value: 'DOCX' },
    { label: 'format_compress_pdf', value: 'COMPRESSED_PDF' },
  ],
  docx: [
    { label: 'format_word_to_pdf', value: 'PDF' },
  ],
  jpg: [
    { label: 'format_jpg_to_png', value: 'PNG' },
  ],
  png: [
    { label: 'format_png_to_jpg', value: 'JPG' },
  ],
  mp4: [
    { label: 'format_mp4_to_mp3', value: 'MP3' },
    { label: 'format_compress_video', value: 'COMPRESSED_VIDEO' },
  ],
}; 