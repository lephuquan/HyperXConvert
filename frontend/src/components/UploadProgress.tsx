import React from 'react';
import { useTranslation } from 'react-i18next';

interface UploadProgressProps {
  progress: number;
}

const UploadProgress: React.FC<UploadProgressProps> = ({ progress }) => {
  const { t } = useTranslation();
  
  return (
    <div className="mt-4">
      <p>{t('uploading')}</p>
      <div className="w-full bg-gray-200 rounded-full h-2.5 mt-2">
        <div
          className="bg-blue-600 h-2.5 rounded-full transition-all duration-200"
          style={{ width: `${progress}%` }}
        ></div>
      </div>
      <p className="text-sm mt-1">{progress}%</p>
    </div>
  );
};

export default UploadProgress; 