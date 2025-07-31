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