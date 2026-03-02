import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AppLayout } from './components/layout/AppLayout';
import { CollectionPage } from './pages/CollectionPage';
import { AddLiquorPage } from './pages/AddLiquorPage';
import { LiquorDetailPage } from './pages/LiquorDetailPage';
import { CardExportPage } from './pages/CardExportPage';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/" element={<CollectionPage />} />
          <Route path="/add" element={<AddLiquorPage />} />
          <Route path="/liquor/:id" element={<LiquorDetailPage />} />
          <Route path="/export" element={<CardExportPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
