import { Wine, Plus, Download } from 'lucide-react';
import { NavItem } from '../ui/NavItem';
import { LanguageToggle } from '../ui/LanguageToggle';
import { useLiquors } from '../../hooks/useLiquors';
import { useLanguage } from '../../i18n/LanguageContext';
import './Sidebar.css';

export function Sidebar() {
  const { liquors } = useLiquors();
  const { t } = useLanguage();

  const categories = new Set(liquors.map((l) => l.category).filter(Boolean));

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">LIQUOR CARDS</div>
      <nav className="sidebar-nav">
        <NavItem to="/" icon={<Wine size={18} />} label={t('nav.collection')} />
        <NavItem to="/add" icon={<Plus size={18} />} label={t('nav.addLiquor')} />
        <NavItem to="/export" icon={<Download size={18} />} label={t('nav.cardExport')} />
      </nav>
      <LanguageToggle />
      <div className="sidebar-stats">
        <div className="sidebar-stat">
          <span className="sidebar-stat-label">{t('common.total')}</span>
          <span className="sidebar-stat-value">{liquors.length}</span>
        </div>
        <div className="sidebar-stat">
          <span className="sidebar-stat-label">{t('common.categories')}</span>
          <span className="sidebar-stat-value">{categories.size}</span>
        </div>
      </div>
    </aside>
  );
}
