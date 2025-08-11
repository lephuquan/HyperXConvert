import Layout from '@/components/layout/Layout';
import './App.css';
import ErrorBoundary from './components/ui/ErrorBoundary';
import FileConverter from './components/FileConverter';


function App() {
  return (
    <div className="min-h-screen w-full bg-gradient-to-b from-slate-200 via-slate-100 to-white dark:bg-gradient-to-b dark:from-[#0d1b2a] dark:to-[#1e293b] dark:bg-none">
      <ErrorBoundary>
        <Layout>
          <FileConverter />
        </Layout>
      </ErrorBoundary>
    </div>
  );
}

export default App;