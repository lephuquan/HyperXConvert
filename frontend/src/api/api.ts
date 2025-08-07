import axios from 'axios';
import i18n from '../i18n';

axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL; // or your .env value
console.log('Axios baseURL:', axios.defaults.baseURL);

const setAcceptLanguageHeader = (lng: string) => {
  let lang = 'en';
  if (lng === 'vi') lang = 'vi';
  else if (lng === 'en') lang = 'en';
  axios.defaults.headers.common['Accept-Language'] = lang;
};

setAcceptLanguageHeader(i18n.language);
i18n.on('languageChanged', (lng: string) => {
  setAcceptLanguageHeader(lng);
});

export default axios;
