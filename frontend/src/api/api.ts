import axios from 'axios';
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL; // or your .env value
console.log('Axios baseURL:', axios.defaults.baseURL);
export default axios;
