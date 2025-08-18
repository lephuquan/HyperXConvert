import React, { useRef, useEffect } from 'react';
import ReactDOM from 'react-dom';
import { useTranslation } from 'react-i18next';
import { SUPPORTED_FORMATS, formatOptionsMap } from '../constants/file';
import { FaRegFileWord } from 'react-icons/fa';
import { BsFiletypePdf, BsFiletypeJpg } from 'react-icons/bs';
import {
  FaRegFileZipper,
  FaRegFileImage,
  FaRegFileVideo,
} from 'react-icons/fa6';
import FiletypeMp3Icon from './FiletypeMp3Icon';

interface ChooseFormatDropdownProps {
  open: boolean;
  anchorRef: React.RefObject<HTMLElement>;
  onClose: () => void;
  onFormatSelect?: (from: string, to: string) => void;
  selectedFormat?: { from: string; to: string } | null;
  fileStatus?: string | null; // Thêm prop này để nhận trạng thái file
}

const ChooseFormatDropdown: React.FC<ChooseFormatDropdownProps> = ({
  open,
  anchorRef,
  onClose,
  selectedFormat,
  onFormatSelect,
  fileStatus, // nhận prop này
}) => {
  const { t } = useTranslation();
  const dropdownRef = useRef<HTMLDivElement>(null);
  const [pos, setPos] = React.useState<{ top: number; left: number } | null>(
    null
  );

  // Flatten all conversion options into a single array
  const allOptions = SUPPORTED_FORMATS.flatMap((from) =>
    (formatOptionsMap[from] || []).map((option) => ({ from, ...option }))
  );

  useEffect(() => {
    if (!open) return;
    const handleClick = (e: MouseEvent) => {
      // Nếu click vào dropdown thì không đóng
      if (dropdownRef.current && dropdownRef.current.contains(e.target as Node))
        return;
      // Nếu click vào anchorRef (nút hoặc header) thì KHÔNG làm gì cả (KHÔNG gọi onClose ở đây)
      if (anchorRef.current && anchorRef.current.contains(e.target as Node))
        return;
      // Ngược lại, đóng dropdown
      onClose();
    };
    document.addEventListener('mousedown', handleClick, true);
    return () => document.removeEventListener('mousedown', handleClick, true);
  }, [open, onClose, anchorRef]);

  if (!open) return null;

  // Lấy vị trí và chiều rộng của header để đặt style cho dropdown
  let dropdownStyle: React.CSSProperties = {};
  if (anchorRef && anchorRef.current) {
    const rect = anchorRef.current.getBoundingClientRect();
    dropdownStyle = {
      position: 'fixed',
      left: rect.left,
      top: rect.bottom,
      width: rect.width,
      zIndex: 9999,
    };
  }

  // Icon mapping giống FormatList
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

  // Các status không cho chọn (trừ CONVERTED)
  const blockStatuses = [
    'UPLOADED',
    'VALIDATING',
    'VALIDATION_FAILED',
    'VALIDATED',
    'CONVERTING',
    'CONVERSION_FAILED',
  ];

  const dropdownContent = (
    <div
      ref={dropdownRef}
      style={dropdownStyle}
      className="max-h-[70vh] mt-14 sm:mt-15 lg:mt-2 bg-white dark:bg-gray-600 border-1 border-emerald-300 items-center justify-center border rounded-2xl shadow-xl animate-fade-in-down"
    >
      <ul className="grid grid-cols-[repeat(auto-fit,minmax(150px,0.5fr))] justify-items-center sm:justify-items-stretch sm:grid-cols-[repeat(auto-fit,minmax(160px,1fr))] gap-1 gap-y-3 sm:gap-3 p-1 my-2 sm:p-3 bg-transparent border-0">
        {allOptions.map((item) => {
          const isBlocked = fileStatus && blockStatuses.includes(fileStatus);
          return (
            <li
              key={`${item.from}-${item.value}`}
              className={`flex flex-col items-center justify-between min-w-[150px] max-w-[160px] sm:min-w-[160px] sm:max-w-[220px] border border-gray-800 dark:border-gray-100 rounded-md shadow-md py-1 bg-white dark:bg-gray-800 ${
                selectedFormat &&
                selectedFormat.from === item.from &&
                selectedFormat.to === item.value
                  ? 'ring-2 ring-blue-400'
                  : ''
              } ${
                isBlocked ? 'cursor-not-allowed opacity-60' : 'cursor-pointer'
              } hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors`}
              onClick={() => {
                if (isBlocked) return;
                onFormatSelect?.(item.from, item.value);
                onClose();
              }}
              onMouseEnter={(e) => {
                if (isBlocked) {
                  const rect = (
                    e.currentTarget as HTMLElement
                  ).getBoundingClientRect();
                  setPos({
                    top: rect.top - 30,
                    left: rect.left + rect.width / 2,
                  });
                }
              }}
              onMouseLeave={() => {
                setPos(null);
              }}
            >
              <div className="flex items-center gap-2 mb-1">
                {getIcon(item.from)}
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
                {getIcon(item.value)}
              </div>
              <div className="text-xs text-center font-semibold dark:text-gray-300 text-gray-800">
                {t(item.label)}
              </div>
            </li>
          );
        })}
      </ul>

      {/* Tooltip dùng portal - moved outside the map loop */}
      {pos &&
        fileStatus &&
        blockStatuses.includes(fileStatus) &&
        ReactDOM.createPortal(
          <div
            style={{
              position: 'fixed',
              top: pos.top,
              left: pos.left,
              transform: 'translateX(-50%)',
              zIndex: 9999,
            }}
            className="px-2 py-1 text-xs text-gray-800 bg-yellow-200 rounded shadow-lg whitespace-nowrap"
          >
            {t('disabled_cause_processing_conversion')}
          </div>,
          document.body
        )}
    </div>
  );

  return ReactDOM.createPortal(dropdownContent, document.body);
};

export default ChooseFormatDropdown;
