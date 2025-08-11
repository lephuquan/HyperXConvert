import React, { useState, useRef, useEffect } from 'react';
import { useTranslation } from 'react-i18next';

interface LanguageOption {
  value: string;
  label: string;
  flag: string;
  flagUrl: string;
}

const LanguageSelector: React.FC = () => {
  const { i18n } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const languages: LanguageOption[] = [
    { value: 'vi', label: 'Tiếng Việt', flag: '🇻🇳', flagUrl: 'https://flagcdn.com/w40/vn.png' },
    { value: 'en', label: 'English', flag: '🇺🇸', flagUrl: 'https://flagcdn.com/w40/us.png' },
  ];

  const currentLanguage = languages.find(lang => lang.value === i18n.language) || languages[0];

  const handleLanguageChange = (language: string) => {
    i18n.changeLanguage(language);
    setIsOpen(false);
  };

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  return (
    <div className="relative" ref={dropdownRef}>
             {/* Selected Language Button */}
       <button
         onClick={() => setIsOpen(!isOpen)}
         className="flex items-center h-[25px] md:h-[30px] bg-[linear-gradient(135deg,#7FFFE0_0%,#B27FFF_100%)] hover:bg-[linear-gradient(135deg,#A4FFF0_0%,#D0B2FF_100%)] transition duration-300 text-black transition-all duration-300 ease-in-out space-x-2 dark:bg-gray-800 border-1 border-gray-600 rounded-3xl px-3 py-1.5 text-sm font-medium dark:text-gray-200 dark:hover:bg-gray-700 dark:bg-none focus:outline-none focus:ring-2 focus:ring-cyan-400 focus:border-cyan-400 transition-colors"
       >
        <img 
          src={currentLanguage.flagUrl} 
          alt={`${currentLanguage.label} flag`}
          className="w-5 h-3 object-cover rounded-sm"
        />
        <span>{currentLanguage.label}</span>
        <svg 
          className={`h-4 w-4  text-black dark:text-gray-400 transition-transform ${isOpen ? 'rotate-180' : ''}`}
          fill="none" 
          stroke="currentColor" 
          viewBox="0 0 24 24"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {/* Dropdown Menu */}
      {isOpen && (
        <div className="absolute right-0 mt-1 w-[9rem] bg-gray-100 border-1 hover:bg-neutral-200 dark:bg-gray-800  border dark:border-gray-600 rounded-xl shadow-lg z-50 overflow-hidden">
          {languages.map((language) => (
            <button
              key={language.value}
              onClick={() => handleLanguageChange(language.value)}
              className={`w-full flex items-center px-1 py-1 text-sm font-medium transition-colors ${
                language.value === i18n.language
                  ? 'bg-cyan-100 text-cyan-700 border-l-4 border-cyan-400 dark:bg-cyan-900/30 dark:text-cyan-300 dark:border-cyan-400'
                  : 'text-gray-700 hover:bg-gray-200 border-l-4 border-gray-300 dark:text-gray-200 dark:hover:bg-gray-700 dark:border-gray-800'

              }`}
            >
              <img 
                src={language.flagUrl} 
                alt={`${language.label} flag`}
                className="w-5 h-3 object-cover rounded-sm mr-2"
              />
              <span  className="text-sm mr-1">{language.label}</span>
              {language.value === i18n.language && (
                <svg className="h-4 w-4 ml-auto mr-1 text-cyan-400" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                </svg>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};

export default LanguageSelector; 