import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import LanguageSelector from '../ui/LanguageSelector';

const Header: React.FC = () => {
  const { t } = useTranslation();
  const [menuOpen, setMenuOpen] = useState(false);

  const NAV_LINKS = [
    { 
      name: t('home_page'), 
      href: '#',
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth="1.5" stroke="currentColor" className="w-5 h-5">
          <path strokeLinecap="round" strokeLinejoin="round" d="m2.25 12 8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25" />
        </svg>
      )
    },
    { 
      name: t('conversion_page'), 
      href: '#',
      icon: (
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-5 h-5">
          <path d="M16.466 7.5C15.643 4.237 13.952 2 12 2 9.239 2 7 6.477 7 12s2.239 10 5 10c.342 0 .677-.069 1-.2"/>
          <path d="m15.194 13.707 3.814 1.86-1.86 3.814"/>
          <path d="M19 15.57c-1.804.885-4.274 1.43-7 1.43-5.523 0-10-2.239-10-5s4.477-5 10-5c4.838 0 8.873 1.718 9.8 4"/>
        </svg>
      )
    },
  ];

  return (
    <header className="bg-gray-900/95 backdrop-blur shadow-lg border-b border border-gray-700 sticky top-1 z-30 rounded-xl lg:rounded-[50px] 2xl:w-[1200px] 2xl:mx-auto">
      <div className="mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-[53px] w-full">
          {/* Logo - Left side */}
          <div className="flex items-center">
            <h1 className="text-2xl sm:text-2xl font-orbitron font-black tracking-wider select-none">
              <span className="text-logo-light">Hyper</span>
              <span className="bg-cyber-gradient bg-clip-text text-transparent">X</span>
              <span className="text-logo-light">Convert</span>
            </h1>
          </div>

                     {/* Desktop Nav - Right side */}
           <nav className="hidden lg:flex space-x-6 items-center">
             {NAV_LINKS.map(link => (
               <a
                 key={link.name}
                 href={link.href}
                 className="text-gray-300 hover:text-white px-3 py-2 rounded-md text-base font-medium transition-colors flex items-center gap-2"
               >
                 {link.icon}
                 {link.name}
               </a>
             ))}

            {/* Language Selector */}
            <LanguageSelector />
          </nav>
          {/* Mobile Hamburger */}
          <button
            className="lg:hidden flex items-center justify-center p-2 rounded focus:outline-none focus:ring-2 focus:ring-primary-500"
            aria-label={t('open_menu')}
            onClick={() => setMenuOpen(!menuOpen)}
          >
            <svg className="h-7 w-7 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
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
         <div className="lg:hidden absolute top-full left-0 right-0 bg-white border-t border-b rounded-2xl lg:rounded-b-[50px] border-gray-200 shadow-sm animate-fade-in-down z-40">
                      <nav className="flex flex-col px-4 py-2 space-y-1">
              {NAV_LINKS.map(link => (
                <a
                  key={link.name}
                  href={link.href}
                  className="text-gray-700 hover:text-primary-600 px-3 py-2 rounded-md text-base font-medium transition-colors flex items-center gap-3"
                  onClick={() => setMenuOpen(false)}
                >
                  {link.icon}
                  {link.name}
                </a>
              ))}

                          {/* Mobile Language Selector */}
              <div className="px-3 py-2">
                <div className="flex items-center space-x-4">
                  <div className="flex items-center gap-2">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth="1.5" stroke="currentColor" className="w-5 h-5 text-gray-700">
                      <path strokeLinecap="round" strokeLinejoin="round" d="m10.5 21 5.25-11.25L21 21m-9-3h7.5M3 5.621a48.474 48.474 0 0 1 6-.371m0 0c1.12 0 2.233.038 3.334.114M9 5.25V3m3.334 2.364C11.176 10.658 7.69 15.08 3 17.502m9.334-12.138c.896.061 1.785.147 2.666.257m-4.589 8.495a18.023 18.023 0 0 1-3.827-5.802" />
                    </svg>
                  </div>
                  <LanguageSelector />
                </div>
              </div>
           </nav>
         </div>
       )}
    </header>
  );
};

export default Header; 