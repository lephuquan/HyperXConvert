// File utility functions
export const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };
  
  export const getFileExtension = (filename: string): string => {
    return filename.slice((filename.lastIndexOf('.') - 1 >>> 0) + 2);
  };
  
  export const isValidFileType = (file: File, allowedTypes: string[]): boolean => {
    const extension = getFileExtension(file.name).toLowerCase();
    return allowedTypes.includes(extension);
  };
  
  export const isValidFileSize = (file: File, maxSizeInMB: number): boolean => {
    const maxSizeInBytes = maxSizeInMB * 1024 * 1024;
    return file.size <= maxSizeInBytes;
  };
  
  export const getFileIcon = (filename: string): string => {
    const extension = getFileExtension(filename).toLowerCase();
    
    const iconMap: Record<string, string> = {
      // Documents
      pdf: '📄',
      doc: '📝',
      docx: '📝',
      txt: '📄',
      rtf: '📄',
      
      // Images
      jpg: '🖼️',
      jpeg: '🖼️',
      png: '🖼️',
      gif: '🖼️',
      bmp: '🖼️',
      webp: '🖼️',
      
      // Videos
      mp4: '🎥',
      avi: '🎥',
      mov: '🎥',
      wmv: '🎥',
      flv: '🎥',
      webm: '🎥',
      
      // Audio
      mp3: '🎵',
      wav: '🎵',
      flac: '🎵',
      aac: '🎵',
      ogg: '🎵',
      
      // Archives
      zip: '📦',
      rar: '📦',
      '7z': '📦',
      tar: '📦',
      
      // Default
      default: '📁'
    };
    
    return iconMap[extension] || iconMap.default;
  };
  
  export const downloadFile = (blob: Blob, filename: string): void => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  };
  
  export const validateFile = (
    file: File, 
    options: {
      maxSizeInMB?: number;
      allowedTypes?: string[];
    } = {}
  ): { isValid: boolean; error?: string } => {
    const { maxSizeInMB = 100, allowedTypes = [] } = options;
    
    // Check file size
    if (!isValidFileSize(file, maxSizeInMB)) {
      return {
        isValid: false,
        error: `File size must be less than ${maxSizeInMB}MB`
      };
    }
    
    // Check file type if specified
    if (allowedTypes.length > 0 && !isValidFileType(file, allowedTypes)) {
      return {
        isValid: false,
        error: `File type not supported. Allowed types: ${allowedTypes.join(', ')}`
      };
    }
    
    return { isValid: true };
  };