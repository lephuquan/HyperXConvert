import React from 'react';
import { useTranslation } from 'react-i18next';

interface LoadingProps {
  size?: 'sm' | 'md' | 'lg';
  text?: string;
  className?: string;
}

const Loading: React.FC<LoadingProps> = ({ 
  size = 'md', 
  text, 
  className = '' 
}) => {
  const { t } = useTranslation();
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-8 h-8',
    lg: 'w-12 h-12'
  };

  return (
    <div className={`flex flex-col items-center justify-center space-y-2 ${className}`}>
      <div 
        className={`${sizeClasses[size]} border-4 border-primary-200 border-t-green-400 dark:border-t-green-500  rounded-full animate-spin`}
      />
      {text && (
        <p className="text-gray-600 dark:text-gray-300 text-sm animate-pulse">{text || t('loading')}</p>
      )}
    </div>
  );
};

export default Loading;