// File types cho dự án HyperX Convert
export interface FileConversion {
  fileId: string;
  userIp: string;
  originalPath: string;
  convertedPath?: string;
  formatFrom: string;
  formatTo: string;
  status: ConversionStatus;
  uploadTime: string;
  expiryTime: string;
  createdAt: string;
}

export enum ConversionStatus {
  UPLOADING = 'UPLOADING',
  UPLOADED = 'UPLOADED',
  CONVERTING = 'CONVERTING',
  CONVERTED = 'CONVERTED',
  FAILED = 'FAILED',
  EXPIRED = 'EXPIRED'
}

export interface SupportedFormat {
  extension: string;
  mimeType: string;
  category: FormatCategory;
  displayName: string;
}

export enum FormatCategory {
  DOCUMENT = 'DOCUMENT',
  IMAGE = 'IMAGE', 
  VIDEO = 'VIDEO',
  AUDIO = 'AUDIO'
}

export interface ConversionRequest {
  file: File;
  formatTo: string;
}

export interface ConversionResponse {
  fileId: string;
  message: string;
  estimatedTime?: number;
}

export interface ApiResponse<T> {
  data: T;
  message: string;
  success: boolean;
}