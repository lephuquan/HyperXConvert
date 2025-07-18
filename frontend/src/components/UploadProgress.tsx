import React from 'react';

interface UploadProgressProps {
  progress: number;
}

const UploadProgress: React.FC<UploadProgressProps> = ({ progress }) => (
  <div className="mt-4">
    <p>Đang tải lên...</p>
    <div className="w-full bg-gray-200 rounded-full h-2.5 mt-2">
      <div
        className="bg-blue-600 h-2.5 rounded-full transition-all duration-200"
        style={{ width: `${progress}%` }}
      ></div>
    </div>
    <p className="text-sm mt-1">{progress}%</p>
  </div>
);

export default UploadProgress; 