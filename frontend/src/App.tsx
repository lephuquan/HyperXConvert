import Layout from '@/components/layout/Layout';
import './App.css';
import ErrorBoundary from './components/ui/ErrorBoundary';
import FileConverter from './components/FileConverter';
import { ToastProvider } from './components/toast';


function App() {
  return (
    <div className="min-h-screen w-full bg-gradient-to-b from-slate-200 via-slate-100 to-white dark:bg-gradient-to-b dark:from-[#0d1b2a] dark:to-[#1e293b]">
      <ErrorBoundary>
        <ToastProvider>
          <Layout>
            <FileConverter />
          </Layout>
        </ToastProvider>
      </ErrorBoundary>
    </div>
  );
}

export default App;