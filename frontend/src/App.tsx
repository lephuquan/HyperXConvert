import Layout from '@/components/layout/Layout';
// import Home from '@/pages/Home';
import './App.css';
import ErrorBoundary from './components/ui/ErrorBoundary';
import FileConverter from './components/FileConverter';
// import ReactGA from 'react-ga4';
// import { ToastContainer } from 'react-toastify';
// import 'react-toastify/dist/ReactToastify.css';


function App() {
  // ReactGA.initialize('G-XXXXXXXXXX');
  return (
    <ErrorBoundary>
      <Layout>
        {/* <Home /> */}
        <FileConverter />
        {/* <ToastContainer position="top-right" autoClose={5000} /> */}
      </Layout>
    </ErrorBoundary>
  );
}

export default App;