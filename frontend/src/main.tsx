import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './styles/globals.css'
import './styles/print.css'
import { LanguageProvider } from './i18n/LanguageContext'
import { SearchQueueProvider } from './hooks/SearchQueueContext'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <LanguageProvider>
      <SearchQueueProvider>
        <App />
      </SearchQueueProvider>
    </LanguageProvider>
  </StrictMode>,
)
