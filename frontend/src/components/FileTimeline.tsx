import React from 'react';
import { FileStatus } from '../types/file';
import { useTranslation } from 'react-i18next';
import axios from '../api/api';
import { useCustomToast } from './toast';

export interface FileTimelineProps {
  status: FileStatus | null;
  hasFile: boolean;
  isUploading: boolean;
  fileId?: string | null; // Allow null for fileId
}

const FileTimeline: React.FC<FileTimelineProps> = ({
  status,
  hasFile,
  isUploading,
  fileId, // Destructure fileId prop
}) => {
  const { t } = useTranslation();
  const toast = useCustomToast(); // Move useCustomToast here
  const [convertedFilename, setConvertedFilename] = React.useState<
    string | null
  >(null); // State to store converted filename

  const steps = [
    { key: '', label: '' }, //Đánh dấu dot đầu tiên không có label - chưa tối ưu
    { key: 'uploaded', label: t('uploaded_file_label') }, // push to S3
    { key: 'validate', label: t('validated_file_label') },
    { key: 'convert', label: t('converted_file_label') },
  ];

  // Skeleton loading effect
  const skeletonSteps = [0, 1, 2];

  const hiddenStatuses: string[] = [
    'UPLOADED',
    'VALIDATING',
    'VALIDATION_FAILED',
    'VALIDATED',
    'CONVERTING',
    'CONVERSION_FAILED',
    'CONVERTED',
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
      if (baseStep.key === 'uploaded') displayLabel = t('uploading_file_label');
      if (baseStep.key === 'validate')
        displayLabel = t('validating_file_label');
      if (baseStep.key === 'convert') displayLabel = t('converting_file_label');
    }

    switch (idx) {
      case 0:
        return {
          label: hasFile ? t('file_is_ready') : t('no_files_yet'),
          labelClass: hasFile
            ? 'text-green-500'
            : 'text-gray-700 dark:text-gray-300',
          dotClass: hasFile
            ? 'border-green-500'
            : 'dark:border-green-300 border-green-600 bg-cyber-gradient',
          icon: (
            <svg
              className={hasFile ? 'text-green-500' : 'text-gray-800'}
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
            ? 'text-green-500'
            : isActive
            ? 'text-green-300 dark:text-green-300'
            : 'text-gray-400',
          dotClass: isDone
            ? 'border-green-500'
            : isActive
            ? 'border-gray-300'
            : 'border-gray-300',
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

  const handleDownloadClick = async (fileId: string) => {
    try {
      const res = await axios.get(`/file/download/${fileId}`);

      const preSignedUrl = res.data?.preSignedUrl || res.data?.downloadUrl;

      if (!preSignedUrl) {
        toast.error(t('no_download_link'));
        return;
      }

      const link = document.createElement('a');
      link.href = preSignedUrl;
      link.setAttribute('download', '');
      link.setAttribute('target', '_blank');
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (err: any) {
      if (err.response?.status === 404) {
        toast.error(t('file_not_found'));
      } else {
        toast.error(t('download_error'));
      }
    }
  };

  // Fetch converted filename when status is CONVERTED and fileId is available
  React.useEffect(() => {
    const fetchConvertedFilename = async () => {
      if (status === 'CONVERTED' && fileId) {
        try {
          const res = await axios.get(`/file/status/${fileId}`);
          setConvertedFilename(res.data?.convertedFilename || null);
        } catch (err) {
          setConvertedFilename(null);
        }
      } else {
        setConvertedFilename(null);
      }
    };
    fetchConvertedFilename();
    // Only run when status or fileId changes
  }, [status, fileId]);

  return (
    <div className="flex flex-col w-full mt-4">
      {/* Skeleton loading effect */}

      {steps.map((_, idx) => {
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
              <div className={`flex-shrink-0 flex items-center justify-center`}>
                <span
                  className={`text-sm font-bold dark:font-normal  ${labelClass}`}
                >
                  {label}
                </span>
              </div>
            </div>
          </div>
        );
      })}
      {!hiddenStatuses.includes(status || '') && (
        <>
          <div className="flex w-full">
            {/* Cột line + dot */}
            <div className="flex flex-col w-6">
              {/* Line xuất hiện trước */}
              <div className="flex w-full animate-pulse">
                <div className="flex flex-col items-center mr-4">
                  <div className="w-6 flex items-center justify-center">
                    <div className="w-[1px] h-6 bg-gray-200"></div>
                  </div>
                </div>
              </div>
              {skeletonSteps.map((idx) => (
                <>
                  <div
                    key={`skeleton-${idx}`}
                    className={`flex w-full animate-pulse
              ${idx > 0 ? 'min-h-[48px]' : ''}`}
                  >
                    <div className="flex flex-col items-center mr-4">
                      {idx > 0 && (
                        <div className="w-[1px] flex-1 bg-gray-200"></div>
                      )}
                      <div className="w-6 h-6 rounded-full border border-gray-400"></div>
                    </div>
                    {/* Skeleton block next to each dot */}
                    {/*<div className="flex-1 h-6 bg-gray-200 rounded ml-4"></div>*/}
                  </div>
                </>
              ))}
            </div>
            {/* Line ảo và line Skeleton loading */}
            <div className="flex flex-1 flex-col">
              {/* Line ảo xuất hiện trước */}
              <div className="flex w-full animate-pulse">
                <div className="flex flex-col items-center mr-4">
                  <div className="w-6 flex items-center justify-center">
                    <div className="w-[1px] h-6"></div>
                  </div>
                </div>
              </div>
              {skeletonSteps.map((idx) => (
                <>
                  <div
                    key={`skeleton-${idx}`}
                    className={`flex w-full animate-pulse
              ${idx > 0 ? 'min-h-[48px]' : ''}`}
                  >
                    <div className="flex items-start flex-col justify-center i w-full mr-4">
                      {idx > 0 && <div className="w-[10px] flex-1"></div>}
                      <div className="h-6 w-2/5 flex flex-col items-center justify-center">
                        <div className="h-[2px] w-full bg-gray-300 opacity-10 rounded ml-4"></div>
                      </div>
                    </div>
                  </div>
                </>
              ))}
            </div>
          </div>
          <div className="mt-4 flex items-stretch gap-2 w-full">
            <button className="flex items-center justify-center h-8 py-3 min-w-[6rem] border-2 border-gray-400  rounded-full animate-pulse opacity-90">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
                className="lucide lucide-download-icon lucide-download text-gray-400"
              >
                <path d="M12 15V3" />
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <path d="m7 10 5 5 5-5" />
              </svg>
            </button>
            <div className="w-2/5 animate-pulse gap-y-3 flex flex-col items-center justify-center">
              <div className="h-[1px] w-full bg-gray-400 opacity-90 rounded ml-4"></div>
              <div className="h-[1px] w-full bg-gray-400 opacity-90 rounded ml-4"></div>
            </div>
          </div>
        </>
      )}
      {status === 'CONVERTED' && (
        <div className="mt-4 flex items-center gap-2 w-full">
          <button
            className="px-2 h-8 py-3 min-w-[6rem] font-medium text-sm text-white bg-gradient-to-r from-green-400 via-blue-500 to-purple-600 rounded-full shadow-lg shadow-green-500/50 hover:scale-105 transform transition-transform duration-300 ease-in-out hover:shadow-purple-500/50 flex items-center justify-center"
            onClick={() => handleDownloadClick(fileId || '')} // Use fileId prop
          >
            <span className="text-sm break-words"> {t('download_file')}</span>
          </button>
          <span className="text-sm break-words text-transparent bg-clip-text bg-gradient-to-r from-green-400 via-blue-500 to-purple-600 max-w-full overflow-hidden text-ellipsis">
            {convertedFilename || t('no_download_link')}
          </span>
        </div>
      )}
    </div>
  );
};

export default FileTimeline;
