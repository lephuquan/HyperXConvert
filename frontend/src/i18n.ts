import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import viTranslation from './locales/vi/translation.json';

// English translations (you can create a separate file later)
const enTranslation = {
  home_page: 'Home',
  conversion_page: 'Converter',
  language: 'Language',
  footer_slogan: 'File conversion made simple. Files are automatically deleted after 24 hours.',
  // Add more English translations as needed
};

i18n
  .use(initReactI18next)
  .init({
    resources: {
      vi: {
        translation: viTranslation,
      },
      en: {
        translation: enTranslation,
      },
    },
    lng: 'vi',
    fallbackLng: 'vi',
    interpolation: {
      escapeValue: false,
    },
  });

export default i18n; 