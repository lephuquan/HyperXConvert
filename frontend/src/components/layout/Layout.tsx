import React, { useState } from 'react';
import Header from './Header';
import Footer from './Footer';

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  // Add key state to force FileConverter remount
  const [fileConverterKey, setFileConverterKey] = useState(0);

  // Handler to reload FileConverter
  const handleReloadFileConverter = () => {
    setFileConverterKey((prev) => prev + 1);
  };

  return (
    <div className="container mx-auto px-4 min-h-screen flex flex-col">
      {/* Header */}
      <Header onReloadFileConverter={handleReloadFileConverter} />

      {/* Main Content */}
      <main className="flex-1 w-full max-w-4xl mx-auto px-2 sm:px-6 lg:px-8 py-6 sm:py-10">
        {/* Pass key to FileConverter if present, else render children */}
        {React.Children.map(children, (child) => {
          if (
            React.isValidElement(child) &&
            child.type &&
            (child.type as any).name === 'FileConverter'
          ) {
            return React.cloneElement(child, { key: fileConverterKey });
          }
          return child;
        })}
      </main>

      {/* Footer */}
      <Footer />
    </div>
  );
};

export default Layout;