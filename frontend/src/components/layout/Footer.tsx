import React from 'react';
import { useTranslation } from 'react-i18next';

const Footer: React.FC = () => {
  const { t } = useTranslation();

  return (
    <footer className="bg-gray-900/90 border-t border-gray-700 mt-8 rounded-xl mb-2  2xl:w-[1200px] 2xl:mx-auto">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Main Footer Content */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 mb-8">
          {/* Brand Section */}
          <div className="lg:col-span-2">
            <div className="mb-4">
              <h2 className="text-2xl sm:text-3xl font-orbitron font-black tracking-wider select-none">
                <span className="text-logo-light">Hyper</span>
                <span className="bg-cyber-gradient bg-clip-text text-transparent">X</span>
                <span className="text-logo-light">Convert</span>
              </h2>
            </div>
            <p className="text-gray-300 text-sm leading-relaxed max-w-md">
              {t('platform_description')}
            </p>
          </div>

          {/* Contact Section */}
          <div>
            <h3 className="text-white font-semibold text-lg mb-4">{t('contact')}</h3>
            <div className="space-y-3">
              <div className="flex items-center gap-3">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth="1.5" stroke="currentColor" className="w-5 h-5 text-gray-400">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M21.75 6.75v10.5a2.25 2.25 0 0 1-2.25 2.25h-15a2.25 2.25 0 0 1-2.25-2.25V6.75m19.5 0A2.25 2.25 0 0 0 19.5 4.5h-15a2.25 2.25 0 0 0-2.25 2.25m19.5 0v.243a2.25 2.25 0 0 1-1.07 1.916l-7.5 4.615a2.25 2.25 0 0 1-2.36 0L3.32 8.91a2.25 2.25 0 0 1-1.07-1.916V6.75" />
                </svg>
                <a 
                  href="mailto:support@hyperxconvert.com" 
                  className="text-gray-300 hover:text-white transition-colors text-sm"
                >
                  {t('support_email')}
                </a>
              </div>
            </div>
          </div>

          {/* Language Support Section */}
          <div>
            <h3 className="text-white font-semibold text-lg mb-4">{t('language_support')}</h3>
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                <span className="text-sm font-medium text-gray-300">VN</span>
                <div className="w-6 h-4 bg-red-600 rounded-sm flex items-center justify-center">
                  <span className="text-white text-xs font-bold">VN</span>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-sm font-medium text-gray-300">EN</span>
                <div className="w-6 h-4 bg-blue-600 rounded-sm flex items-center justify-center">
                  <span className="text-white text-xs font-bold">EN</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Slogan Section */}
        <div className="border-t border-gray-700 pt-6 mb-6">
          <div className="text-center">
            <p className="text-cyan-400 font-medium text-lg">
              {t('convenient_fast_safe')}
            </p>
          </div>
        </div>

        {/* Copyright Section */}
        <div className="border-t border-gray-700 pt-6">
          <div className="text-center">
            <p className="text-gray-400 text-sm">
              {t('copyright')}
            </p>
          </div>
        </div>
      </div>
    </footer>
  );
};

export default Footer; 