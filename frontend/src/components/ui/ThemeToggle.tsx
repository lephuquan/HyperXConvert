import useDarkMode from '../../hooks/useDarkMode';
import { useTranslation } from 'react-i18next';

export default function ThemeToggle() {
  const [isDark, setIsDark] = useDarkMode();
  const { t } = useTranslation();

  return (
    <div className="flex gap-2 h-[25px] md:h-[30px]">
      {!isDark && (
        <button
          type="button"
          className="transition-all duration-300 ease-in-out bg-cyan-300 hover:bg-cyan-200 text-gray-900 flex items-center gap-x-2 py-2 px-3 rounded-full text-sm font-medium font-sans shadow hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-primary-500 animate-fade-in"
          aria-pressed={!isDark}
          onClick={() => setIsDark(true)}
        >
          <svg className="shrink-0 size-4" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M21 12.79A9 9 0 1 1 11.21 3a7 7 0 0 0 9.79 9.79z" />
          </svg>
          {t('dark')}
        </button>
      )}
      {isDark && (
        <button
          type="button"
          className="transition-all duration-300 ease-in-out bg-white/10 text-white flex items-center gap-x-2 py-2 px-3 rounded-full text-sm font-medium font-sans shadow hover:bg-white/20 focus:outline-none focus:ring-2 focus:ring-primary-500 animate-fade-in"
          aria-pressed={isDark}
          onClick={() => setIsDark(false)}
        >
          <svg className="shrink-0 size-4" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="5" />
            <path d="M12 1v2" />
            <path d="M12 21v2" />
            <path d="M4.22 4.22l1.42 1.42" />
            <path d="M18.36 18.36l1.42 1.42" />
            <path d="M1 12h2" />
            <path d="M21 12h2" />
            <path d="M4.22 19.78l1.42-1.42" />
            <path d="M18.36 5.64l1.42-1.42" />
          </svg>
          {t('light')}
        </button>
      )}
    </div>
  );
}
