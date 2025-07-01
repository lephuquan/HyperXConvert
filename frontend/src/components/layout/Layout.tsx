import React from 'react';

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center">
              <h1 className="text-2xl font-bold text-primary-600">
                HyperX Convert
              </h1>
              <span className="ml-2 text-sm text-gray-500 bg-gray-100 px-2 py-1 rounded">
                MVP
              </span>
            </div>
            
            <nav className="hidden md:flex space-x-8">
              <a href="#" className="text-gray-500 hover:text-gray-700 px-3 py-2 rounded-md text-sm font-medium">
                Converter
              </a>
              <a href="#" className="text-gray-500 hover:text-gray-700 px-3 py-2 rounded-md text-sm font-medium">
                History
              </a>
              <a href="#" className="text-gray-500 hover:text-gray-700 px-3 py-2 rounded-md text-sm font-medium">
                Support
              </a>
            </nav>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {children}
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-gray-200 mt-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="text-center text-gray-500 text-sm">
            <p>&copy; 2025 HyperX Convert MVP. File conversion made simple.</p>
            <p className="mt-1">Files are automatically deleted after 24 hours.</p>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default Layout;