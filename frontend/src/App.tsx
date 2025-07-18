import Layout from '@/components/layout/Layout';
import './App.css';
import ErrorBoundary from './components/ui/ErrorBoundary';
import FileConverter from './components/FileConverter';


function App() {
  return (
    <ErrorBoundary>
      <Layout>
        <FileConverter />
      </Layout>
    </ErrorBoundary>
  );
}

export default App;