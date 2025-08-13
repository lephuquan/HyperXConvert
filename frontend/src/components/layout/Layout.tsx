import React, { useState } from 'react';
import Header from './Header';
import Footer from './Footer';
import { useCustomToast, ToastContainer } from '../toast';
import FileConverter from '../FileConverter';
import { useTranslation } from 'react-i18next';

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const { t } = useTranslation();
  // Add key state to force FileConverter remount
  const [fileConverterKey, setFileConverterKey] = useState(0);
  // Track if FileConverter is already reset
  const [isFileConverterReset, setIsFileConverterReset] = useState(true);
  const toast = useCustomToast();

  // Handler to reload FileConverter
  const handleReloadFileConverter = () => {
    if (isFileConverterReset) {
      toast.info(t('nothingToRefresh'));
      return;
    }
    setFileConverterKey((prev) => prev + 1);
    setIsFileConverterReset(true);
    toast.success(t('refreshed'));
  };

  // Detect if FileConverter is not reset (i.e., user interacted)
  const childrenWithReset = React.Children.map(children, (child) => {
    if (
      React.isValidElement(child) &&
      (child.type === FileConverter || (child.type as any).displayName === 'FileConverter')
    ) {
      // Only pass onUserInteract if child is FileConverter
      return React.cloneElement(child as React.ReactElement<any>, {
        key: fileConverterKey,
        onUserInteract: () => setIsFileConverterReset(false),
      });
    }
    return child;
  });

  return (
    <div className="container mx-auto px-4 min-h-screen flex flex-col">
      {/* Header */}
      <Header onReloadFileConverter={handleReloadFileConverter} />

      {/* Toast Notifications */}
      <div className="2xl:w-[1200px] 2xl:mx-auto sticky mt-3 z-30">
        <div className="w-full absolute flex justify-end">
          <ToastContainer className="top-and-bottom-none"/>
        </div>
      </div>

      {/* Main Content */}
      <main className="flex-1 w-full max-w-4xl mx-auto px-2 sm:px-6 lg:px-8 py-6 sm:py-10">
        {childrenWithReset}
      </main>

      {/* Footer */}
      <Footer />
    </div>
  );
};

export default Layout;
