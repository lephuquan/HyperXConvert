import React from 'react';
import axios from '../api/api';
import { useTranslation } from 'react-i18next';

interface ConversionInfoTableProps {
  beforeExt?: string;
  beforeSize?: string;
  afterExt?: string;
  status?: string;
  fileId?: string | null;
  completedTime?: string; // new prop for mm:ss
}

const ConversionInfoTable: React.FC<ConversionInfoTableProps> = ({
  beforeExt,
  beforeSize,
  afterExt,
  status,
  fileId,
  completedTime, // new prop
}) => {
  const [convertedFileSize, setConvertedFileSize] = React.useState<
    string | null
  >(null);
  const [expiresAt, setExpiresAt] = React.useState<string | null>(null);
  const { t } = useTranslation();

  React.useEffect(() => {
    const fetchConvertedInfo = async () => {
      if (status === 'CONVERTED' && fileId) {
        try {
          const res = await axios.get(`/file/converted-info/${fileId}`);
          setConvertedFileSize(res.data?.convertedFileSize || null);
          setExpiresAt(res.data?.expiresAt || null);
        } catch {
          setConvertedFileSize(null);
          setExpiresAt(null);
        }
      } else {
        setConvertedFileSize(null);
        setExpiresAt(null);
      }
    };
    fetchConvertedInfo();
  }, [status, fileId]);

  // Helper to format file size
  const formatFileSize = (size: string | number | null | undefined) => {
    if (!size) return '--';
    const num = typeof size === 'number' ? size : parseFloat(size);
    if (isNaN(num)) return size.toString();
    if (num >= 1024 * 1024) return (num / (1024 * 1024)).toFixed(2) + ' MB';
    if (num >= 1024) return (num / 1024).toFixed(2) + ' KB';
    return num + ' B';
  };

  return (
    <div className="overflow-x-auto w-full">
      <table className="min-w-full border border-0 rounded-xl text-xs text-white  shadow-lg">
        <thead>
          <tr className="">
            <th className="border-b border-gray-700 px-3 py-2 text-left font-semibold text-cyan-900 dark:text-cyan-300 whitespace-nowrap rounded-tl-xl">
              {t('info')}
            </th>
            <th className="border-b border-gray-700 px-3 py-2 text-left font-semibold text-cyan-900 dark:text-cyan-300 whitespace-nowrap">
              {t('before')}
            </th>
            <th className="border-b border-gray-700 px-3 py-2 text-left font-semibold text-cyan-900 dark:text-cyan-300 whitespace-nowrap rounded-tr-xl">
              {t('after')}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr className="hover:bg-[#2d3748]/60 transition">
            <td className="px-3 py-2 font-medium text-cyan-900 dark:text-cyan-200 border-b border-gray-700">
              {t('format')}
            </td>
            <td className="px-3 py-2 border-b border-gray-700 text-cyan-600 dark:text-cyan-100 font-mono">
              {beforeExt || '--'}
            </td>
            <td className="px-3 py-2 border-b border-gray-700 text-green-600 dark:text-green-300 font-mono font-semibold">
              {afterExt || '--'}
            </td>
          </tr>
          <tr className="hover:bg-[#2d3748]/60 transition">
            <td className="px-3 py-2 font-medium text-cyan-900 dark:text-cyan-200 border-b border-gray-700">
              {t('size')}
            </td>
            <td className="px-3 py-2 border-b border-gray-700 text-cyan-600 dark:text-cyan-100 font-mono">
              {beforeSize || '--'}
            </td>
            <td className="px-3 py-2 border-b border-gray-700 text-green-600 dark:text-green-300 font-mono font-semibold">
              {formatFileSize(convertedFileSize)}
            </td>
          </tr>
          <tr className="hover:bg-[#2d3748]/60 transition">
            <td className="px-3 py-2 font-medium text-cyan-900 dark:text-cyan-200 border-b border-gray-700">
              {t('completion_time')}
            </td>
            <td
              className="px-3 py-2 border-b border-gray-700 text-cyan-900 dark:text-cyan-100 font-mono"
              colSpan={2}
            >
              <span className="text-yellow-600 dark:text-yellow-300 font-semibold">
                {completedTime || '--'}
              </span>
            </td>
          </tr>
          <tr className="hover:bg-[#2d3748]/60 transition">
            <td className="px-3 py-2 font-medium text-cyan-900 dark:text-cyan-200">
              {t('file_download_exp_time')}
            </td>
            <td className="px-3 py-2 text-cyan-100 font-mono" colSpan={2}>
              <span className="text-pink-500 dark:text-pink-300 font-semibold">
                {expiresAt || '--'}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  );
};

export default ConversionInfoTable;
