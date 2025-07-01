import Layout from '@/components/layout/Layout';
import Home from '@/pages/Home';
import './App.css';
import ErrorBoundary from './components/ui/ErrorBoundary';


function App() {
  return (
    <ErrorBoundary>
      <Layout>
        <Home />
      </Layout>
    </ErrorBoundary>
  );
}

export default App;