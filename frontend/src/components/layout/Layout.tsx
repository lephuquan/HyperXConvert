import React, { useState } from 'react';

interface LayoutProps {
  children: React.ReactNode;
}

const NAV_LINKS = [
  { name: 'Converter', href: '#' },
  { name: 'History', href: '#' },
  { name: 'Support', href: '#' },
];

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <div className="min-h-screen flex flex-col bg-gradient-to-b from-white to-gray-100">
      {/* Header */}
      <header className="bg-white/90 backdrop-blur shadow-sm border-b border-gray-200 sticky top-0 z-30">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            {/* Logo */}
            <div className="flex items-center">
              <span className="inline-block rounded-lg bg-primary-600/10 p-2 mr-2">
                <svg width="28" height="28" viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="14" cy="14" r="14" fill="#2563eb"/>
                  <path d="M8 14h12M14 8v12" stroke="#fff" strokeWidth="2" strokeLinecap="round"/>
                </svg>
              </span>
              <span className="text-xl sm:text-2xl font-extrabold tracking-tight text-primary-700 select-none">
                HyperXConvert
              </span>
            </div>
            {/* Desktop Nav */}
            <nav className="hidden md:flex space-x-6">
              {NAV_LINKS.map(link => (
                <a
                  key={link.name}
                  href={link.href}
                  className="text-gray-700 hover:text-primary-600 px-3 py-2 rounded-md text-base font-medium transition-colors"
                >
                  {link.name}
                </a>
              ))}
            </nav>
            {/* Mobile Hamburger */}
            <button
              className="md:hidden flex items-center justify-center p-2 rounded focus:outline-none focus:ring-2 focus:ring-primary-500"
              aria-label="Open menu"
              onClick={() => setMenuOpen(!menuOpen)}
            >
              <svg className="h-7 w-7 text-primary-700" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                {menuOpen ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8h16M4 16h16" />
                )}
              </svg>
            </button>
          </div>
        </div>
        {/* Mobile Nav Drawer */}
        {menuOpen && (
          <div className="md:hidden bg-white border-t border-b border-gray-200 shadow-sm animate-fade-in-down">
            <nav className="flex flex-col px-4 py-2 space-y-1">
              {NAV_LINKS.map(link => (
                <a
                  key={link.name}
                  href={link.href}
                  className="text-gray-700 hover:text-primary-600 px-3 py-2 rounded-md text-base font-medium transition-colors"
                  onClick={() => setMenuOpen(false)}
                >
                  {link.name}
                </a>
              ))}
            </nav>
          </div>
        )}
      </header>

      {/* Main Content */}
      <main className="flex-1 w-full max-w-4xl mx-auto px-2 sm:px-6 lg:px-8 py-6 sm:py-10">
        {children}
      </main>

      {/* Footer */}
      <footer className="bg-white/90 border-t border-gray-200 mt-8">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-6 flex flex-col items-center">
          <p className="text-gray-700 text-sm font-medium">&copy; 2025 HyperX Convert MVP</p>
          <p className="text-gray-400 text-xs mt-1">File conversion made simple. Files are automatically deleted after 24 hours.</p>
        </div>
      </footer>
    </div>
  );
};

export default Layout;