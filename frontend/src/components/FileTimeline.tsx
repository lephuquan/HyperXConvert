import React from 'react';
import { FileStatus } from '../types/file';
import { useTranslation } from 'react-i18next';

export interface FileTimelineProps {
  status: FileStatus | null;
  hasFile: boolean;
  isUploading: boolean;
}

const FileTimeline: React.FC<FileTimelineProps> = ({
  status,
  hasFile,
  isUploading,
}) => {

  const { t } = useTranslation();

  const steps = [
    { key: '', label: '' }, //Đánh dấu dot đầu tiên không có label - chưa tối ưu
    { key: 'uploaded', label: t('uploaded_file_label') }, // push to S3
    { key: 'validate', label: t('validated_file_label') },
    { key: 'convert', label: t('converted_file_label') },
  ];

  // Xác định bước hiện tại
  let currentStep = 0;
  if (!hasFile) currentStep = 0;
  else if (isUploading) currentStep = 1;
  else if (status === 'UPLOADED') currentStep = 2;
  else if (
    status === 'VALIDATING' ||
    status === 'VALIDATED' ||
    status === 'VALIDATION_FAILED'
  )
    currentStep = 3;
  else if (
    status === 'CONVERTING' ||
    status === 'CONVERTED' ||
    status === 'CONVERSION_FAILED'
  )
    currentStep = 4;

  const getStepProps = (idx: number) => {
    const baseStep = steps[idx];
    const isActive = currentStep === idx;
    const isDone = currentStep > idx;

    // Mặc định giữ label gốc
    let displayLabel = baseStep.label;

    // Thêm "..." khi loading và không phải dot đầu tiên
    if (isActive && idx > 0) {
      if (baseStep.key === "uploaded") displayLabel = t("uploading_file_label");
      if (baseStep.key === "validate") displayLabel = t("validating_file_label");
      if (baseStep.key === "convert") displayLabel = t("converting_file_label");
    }


    switch (idx) {
      case 0:
        return {
          label: hasFile ? t("file_is_ready") : t("no_files_yet"),
          labelClass: hasFile ? "text-green-500" : "text-gray-700 dark:text-gray-300",
          dotClass: hasFile ? "border-green-500" : "dark:border-green-300 border-green-600 bg-cyber-gradient",
          icon: (
            <svg
              className={hasFile ? "text-green-500" : "text-gray-800"}
              width="12"
              height="12"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M12 2v10" />
              <path d="M18.4 6.6a9 9 0 1 1-12.77.04" />
            </svg>
          ),
        };
      default:
        return {
          label: displayLabel,
          labelClass: isDone
            ? "text-green-500"
            : isActive
              ? "text-green-300 dark:text-green-300"
              : "text-gray-400",
          dotClass: isDone
            ? "border-green-500"
            : isActive
              ? "border-gray-300"
              : "border-gray-300",
          icon: isDone ? (
            <svg
              className="text-green-500"
              width="12"
              height="12"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M20 6 9 17l-5-5" />
            </svg>
          ) : isActive ? (
            <svg
              className="animate-spin text-gray-800"
              width="12"
              height="12"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M12 2v4" />
              <path d="m16.2 7.8 2.9-2.9" />
              <path d="M18 12h4" />
              <path d="m16.2 16.2 2.9 2.9" />
              <path d="M12 18v4" />
              <path d="m4.9 19.1 2.9-2.9" />
              <path d="M2 12h4" />
              <path d="m4.9 4.9 2.9 2.9" />
            </svg>
          ) : null,
        };
    }
  };

  return (
    <div className="flex flex-col w-full mt-4">
      {steps.map((step, idx) => {
        if (idx > currentStep) return null;

        const { label, labelClass, dotClass, icon } = getStepProps(idx);

        // màu line
        let lineColor = 'bg-gray-300';
        if (labelClass.includes('red')) lineColor = 'bg-red-500';
        else if (labelClass.includes('green-500')) lineColor = 'bg-green-500';
        else if (labelClass.includes('green-300')) lineColor = 'bg-green-300';

        return (
          <div
            key={idx}
            className={`flex w-full  ${idx > 0 ? 'min-h-[48px]' : ''}`}
          >
            {/* Cột line + dot */}
            <div className="flex flex-col items-center mr-4">
              {/* Line trên dot (lấy từ step trước) */}
              {idx > 0 && (
                <div
                  className={`w-0.5 flex-1 ${
                    currentStep >= idx ? lineColor : 'bg-gray-300'
                  }`}
                ></div>
              )}

              {/* Dot */}
              <div
                className={`w-6 h-6 rounded-full border-2 flex-shrink-0 bg-white flex items-center justify-center ${dotClass}`}
              >
                {icon}
              </div>
            </div>

            {/*-------Label-------*/}
            <div className="flex flex-col justify-center items-center text-center mr-4">
              {/* Line ảo */}
              {idx > 0 && <div className={`w-0.5 flex-1`}></div>}
              {/* Label */}
              <div
                className={`flex-shrink-0 flex items-center justify-center`}
              >
                <span className={`text-sm font-bold dark:font-normal  ${labelClass}`}>{label}</span>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export default FileTimeline;
