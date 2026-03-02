import { useLanguage } from '../../i18n/LanguageContext';
import './LanguageToggle.css';

export function LanguageToggle() {
  const { locale, setLocale } = useLanguage();

  return (
    <div className="language-toggle">
      <button
        className={`language-toggle-btn ${locale === 'en' ? 'active' : ''}`}
        onClick={() => setLocale('en')}
      >
        EN
      </button>
      <button
        className={`language-toggle-btn ${locale === 'ko' ? 'active' : ''}`}
        onClick={() => setLocale('ko')}
      >
        한국어
      </button>
    </div>
  );
}
