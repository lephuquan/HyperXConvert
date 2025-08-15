import React, { useState } from 'react';
import Header from './Header';
import Footer from './Footer';
import { useCustomToast, ToastContainer } from '../toast';
import FileConverter from '../FileConverter';
import { useTranslation } from 'react-i18next';
import FileTimeline from '../FileTimeline';
import FormatList from '../FormatList';

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = () => {
  const { t } = useTranslation();
  // Add key state to force FileConverter remount
  const [fileConverterKey, setFileConverterKey] = useState(0);
  // Track if FileConverter is already reset
  const [isFileConverterReset, setIsFileConverterReset] = useState(true);
  const toast = useCustomToast();

  // --- Timeline state ---
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadStatus, setUploadStatus] = useState<string>('idle');
  const [fileStatus, setFileStatus] = useState<string | null>(null);

  // --- Format selection state ---
  const [selectedFormat, setSelectedFormat] = useState<{ from: string; to: string } | null>(null);

  const handleFormatSelect = (from: string, to: string) => {
    setSelectedFormat({ from, to });
  };

  // Handler to reload FileConverter
  const handleReloadFileConverter = () => {
    if (isFileConverterReset) {
      toast.info(t('nothingToRefresh'));
      return;
    }
    setFileConverterKey((prev) => prev + 1);
    setIsFileConverterReset(true);
    toast.success(t('refreshed'));
    // Reset timeline state
    setSelectedFile(null);
    setUploadStatus('idle');
    setFileStatus(null);
  };

  return (
    <div className="container mx-auto px-4 min-h-screen flex flex-col">
      {/* Header */}
      <Header onReloadFileConverter={handleReloadFileConverter} />

      {/* Toast Notifications */}
      <div className="2xl:w-[1200px] 2xl:mx-auto sticky mt-10 z-30">
        <div className="w-full absolute flex justify-end">
          <ToastContainer className="top-and-bottom-none"/>
        </div>
      </div>

      {/* Main Content: Flex row for FileConverter + Timeline */}
      <main className="flex-1 2xl:w-[1200px] 2xl:mx-auto px-2 sm:px-6 lg:px-8 py-6 sm:py-10">
        <div className="flex flex-col lg:flex-row gap-8 items-stretch">
          {/* SongList section - only on desktop */}
          <div className="hidden lg:block w-full lg:w-[260px] xl:w-[300px] 2xl:w-[320px]">
            <FormatList onFormatSelect={handleFormatSelect} selectedFormat={selectedFormat} />
          </div>
          <div>
            <FileConverter
              key={fileConverterKey}
              onUserInteract={() => setIsFileConverterReset(false)}
              onTimelineStateChange={(state: { selectedFile: File | null; uploadStatus: string; fileStatus: string | null }) => {
                setSelectedFile(state.selectedFile);
                setUploadStatus(state.uploadStatus);
                setFileStatus(state.fileStatus);
              }}
              selectedFormat={selectedFormat}
            />
          </div>
          <div className="w-full lg:w-[340px] xl:w-[380px] 2xl:w-[400px]">
            {/* Timeline section */}
            {/* Only render on desktop (lg+) */}
            <div className="hidden lg:block">
              <FileTimeline
                status={fileStatus as any}
                hasFile={!!selectedFile}
                isUploading={uploadStatus === 'uploading'}
              />
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <Footer />
    </div>
  );
};

export default Layout;
