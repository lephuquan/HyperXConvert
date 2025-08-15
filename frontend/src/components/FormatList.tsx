import React from 'react';
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
}

const FormatList: React.FC<FormatListProps> = ({ onFormatSelect, selectedFormat }) => {
  const { t } = useTranslation();
  // Flatten all conversion options into a single array
  const allOptions = SUPPORTED_FORMATS.flatMap((from) =>
    (formatOptionsMap[from] || []).map((option) => ({ from, ...option }))
  );

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
          >
            <div className="grid grid-rows-2 col-span-9 justify-items-center items-center">
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
              <div className="text-xs font-semibold dark:text-gray-300 text-gray-800">{t(item.label)}</div>
            </div>
            <button
              className="btn btn-square btn-ghost col-span-3 justify-items-center border-l border-gray-800 dark:border-gray-200 rounded-md hover:bg-gray-300 dark:hover:bg-gray-700"
              onClick={() => onFormatSelect?.(item.from, item.value)}
              aria-label={`Select ${item.label}`}
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
          </li>
        ))}
      </ul>
    </div>
  );
}

export default FormatList;
