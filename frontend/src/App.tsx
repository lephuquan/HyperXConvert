import Layout from '@/components/layout/Layout';
import './App.css';
import ErrorBoundary from './components/ui/ErrorBoundary';
import FileConverter from './components/FileConverter';


function App() {
  return (
    <div className="min-h-screen w-full bg-gradient-to-b from-slate-200 via-slate-100 to-white dark:bg-[#64748b]/20 dark:bg-none">
      <ErrorBoundary>
        <Layout>
          <FileConverter />
        </Layout>
      </ErrorBoundary>
    </div>
  );
}

export default App;