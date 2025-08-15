import React, { useState } from 'react';
import { createPortal } from 'react-dom';
import { FaRegFileWord } from 'react-icons/fa';
import { BsFiletypePdf } from 'react-icons/bs';
import { FaRegFileZipper } from 'react-icons/fa6';
import { formatOptionsMap, SUPPORTED_FORMATS } from '../constants/file';
import { BsFiletypeJpg } from 'react-icons/bs';
import { FaRegFileImage, FaRegFileVideo} from 'react-icons/fa';
import { useTranslation } from 'react-i18next';
import FiletypeMp3Icon from './FiletypeMp3Icon';

const formatIcons: Record<string, JSX.Element> = {
  pdf: <BsFiletypePdf size={25} className="text-red-400" />,
  docx: <FaRegFileWord size={25} className="text-blue-400" />,
  jpg: <BsFiletypeJpg size={25} className="text-yellow-400" />,
  png: <FaRegFileImage size={25} className="text-green-400" />,
  mp4: <FaRegFileVideo size={25} className="text-purple-400" />,
  mp3: <FiletypeMp3Icon size={25} className="text-indigo-400" />,
  compressed_pdf: <FaRegFileZipper size={25} className="text-red-400" />,
  compressed_video: <FaRegFileZipper size={25} className="text-purple-400" />,
};

const getIcon = (format: string) => {
  const key = format.toLowerCase();
  return formatIcons[key] || <span className="text-xs">{format}</span>;
};

interface FormatListProps {
  onFormatSelect?: (from: string, to: string) => void;
  selectedFormat?: { from: string; to: string } | null;
  fileStatus?: string | null; // Thêm prop này để nhận trạng thái file
  onForceReloadAndSelect?: (from: string, to: string) => void; // callback khi cần reload rồi mới chọn format
}

const FormatList: React.FC<FormatListProps> = ({ onFormatSelect, selectedFormat, fileStatus, onForceReloadAndSelect }) => {
  const { t } = useTranslation();
  // Flatten all conversion options into a single array
  const allOptions = SUPPORTED_FORMATS.flatMap((from) =>
    (formatOptionsMap[from] || []).map((option) => ({ from, ...option }))
  );

  // Các status không cho chọn (trừ CONVERTED)
  const blockStatuses = [
    'UPLOADED',
    'VALIDATING',
    'VALIDATION_FAILED',
    'VALIDATED',
    'CONVERTING',
    'CONVERSION_FAILED',
  ];

  const [pos, setPos] = useState<{ top: number; left: number } | null>(null);

  return (
    <div className="mt-4">
      <div className="text-sm font-extrabold p-2 text-gray-800 tracking-wide dark:text-gray-300">
        List of conversion formats
      </div>
      <ul className="rounded-box shadow-lg max-h-80 overflow-y-auto custom-scrollbar border border-gray-300 dark:border-gray-600 p-1 rounded-md dark:bg-gray-800 opacity-85">
        {allOptions.map((item) => (
          <li
            key={`${item.from}-${item.value}`}
            className={`grid grid-cols-12 gap-2 border border-gray-800 dark:border-gray-100 rounded-md shadow-md mb-2 ${selectedFormat && selectedFormat.from === item.from && selectedFormat.to === item.value ? 'ring-2 ring-blue-400' : ''}`}
            onMouseEnter={(e) => {
              if (fileStatus && blockStatuses.includes(fileStatus)) {
                const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
                setPos({
                  top: rect.top - 30,
                  left: rect.left + rect.width / 2,
                });
              }
            }}
            onMouseLeave={() => setPos(null)}
          >
            <div className="grid p-1 grid-rows-2 col-span-9 justify-items-center items-center">
              <div className="grid grid-cols-3 gap-x-2 pt-1">
                <div className="col-span-1">{getIcon(item.from)}</div>
                <div className="col-span-1">
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="24"
                    height="24"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    className="lucide lucide-move-right-icon lucide-move-right text-gray-800 dark:text-gray-300"
                  >
                    <path d="M18 8L22 12L18 16" />
                    <path d="M2 12H22" />
                  </svg>
                </div>
                <div className="col-span-1">{getIcon(item.value)}</div>
              </div>
              <div className="text-xs text-center font-semibold dark:text-gray-300 text-gray-800">{t(item.label)}</div>
            </div>
            <button
              className={
                `relative group btn btn-square btn-ghost col-span-3 justify-items-center border-l border-gray-800 dark:border-gray-200 rounded-md hover:bg-gray-300 dark:hover:bg-gray-700 disabled:cursor-not-allowed`
              }
              onClick={() => {
                if (fileStatus === 'CONVERTED' && onForceReloadAndSelect) {
                    onForceReloadAndSelect(item.from, item.value);
                  return;
                }
                if (fileStatus && blockStatuses.includes(fileStatus)) {
                  return;
                }
                onFormatSelect?.(item.from, item.value);
              }}
              aria-label={`Select ${item.label}`}
              disabled={!!fileStatus && blockStatuses.includes(fileStatus)}
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                className="lucide lucide-circle-chevron-right-icon lucide-circle-chevron-right text-gray-800 dark:text-gray-200"
              >
                <circle cx="12" cy="12" r="10" />
                <path d="m10 8 4 4-4 4" />
              </svg>
            </button>
            {/* Tooltip dùng portal */}
            {pos && fileStatus && blockStatuses.includes(fileStatus) &&
              createPortal(
                <div
                  style={{
                    position: "fixed",
                    top: pos.top,
                    left: pos.left,
                    transform: "translateX(-50%)",
                    zIndex: 9999,
                  }}
                  className="px-2 py-1 text-xs text-gray-800 bg-yellow-200 rounded shadow-lg whitespace-nowrap"
                >
                  {t('disabled_cause_processing_conversion')}
                </div>,
                document.body
              )}
          </li>
        ))}
      </ul>
    </div>
  );
}

export default FormatList;
