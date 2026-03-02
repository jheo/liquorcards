import { useState, useMemo } from 'react';
import { Wine, Loader } from 'lucide-react';
import { SearchBar } from '../components/ui/SearchBar';
import { Chip } from '../components/ui/Chip';
import { LiquorCard } from '../components/ui/LiquorCard';
import { useLiquors } from '../hooks/useLiquors';
import { useLanguage } from '../i18n/LanguageContext';
import './CollectionPage.css';

const CATEGORY_KEYS = ['all', 'whisky', 'wine', 'gin', 'vodka', 'rum', 'tequila', 'brandy'] as const;

export function CollectionPage() {
  const { liquors, loading } = useLiquors();
  const { t } = useLanguage();
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('all');
  const [sort, setSort] = useState('recent');

  const sortOptions = [
    { value: 'name', label: t('collection.sortName') },
    { value: 'score', label: t('collection.sortScore') },
    { value: 'recent', label: t('collection.sortRecent') },
  ];

  const filtered = useMemo(() => {
    let result = liquors.filter((l) => l.status !== 'archived');

    if (search) {
      const q = search.toLowerCase();
      result = result.filter((l) => {
        if (l.name.toLowerCase().includes(q)) return true;
        if (l.type?.toLowerCase().includes(q)) return true;
        if (l.origin?.toLowerCase().includes(q)) return true;
        if (l.nameKo?.toLowerCase().includes(q)) return true;
        if (l.typeKo?.toLowerCase().includes(q)) return true;
        if (l.about?.toLowerCase().includes(q)) return true;
        if (l.aboutKo?.toLowerCase().includes(q)) return true;
        if (l.heritage?.toLowerCase().includes(q)) return true;
        if (l.heritageKo?.toLowerCase().includes(q)) return true;
        if (l.region?.toLowerCase().includes(q)) return true;
        if (l.age?.toLowerCase().includes(q)) return true;
        if (l.price?.toLowerCase().includes(q)) return true;
        if (l.tastingNotes?.some((n) => n.toLowerCase().includes(q))) return true;
        if (l.tastingNotesKo?.some((n) => n.toLowerCase().includes(q))) return true;
        if (l.profile && Object.keys(l.profile).some((k) => k.toLowerCase().includes(q))) return true;
        return false;
      });
    }

    if (category !== 'all') {
      result = result.filter((l) => l.category === category);
    }

    switch (sort) {
      case 'name':
        result = [...result].sort((a, b) => a.name.localeCompare(b.name));
        break;
      case 'score':
        result = [...result].sort((a, b) => (b.score ?? 0) - (a.score ?? 0));
        break;
      case 'recent':
        result = [...result].sort(
          (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        break;
    }

    return result;
  }, [liquors, search, category, sort]);

  return (
    <div>
      <div className="collection-header">
        <h1 className="collection-title">{t('collection.title')}</h1>
        <span className="collection-count">{filtered.length}</span>
      </div>

      <div className="collection-filters">
        <SearchBar
          value={search}
          onChange={setSearch}
          placeholder={t('collection.searchPlaceholder')}
        />
        <div className="collection-chips">
          {CATEGORY_KEYS.map((cat) => (
            <Chip
              key={cat}
              label={t(`collection.${cat}`)}
              active={category === cat}
              onClick={() => setCategory(cat)}
            />
          ))}
        </div>
        <div className="collection-sort">
          <span className="collection-sort-label">{t('collection.sortBy')}</span>
          <select value={sort} onChange={(e) => setSort(e.target.value)}>
            {sortOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {loading ? (
        <div className="collection-loading">
          <Loader size={24} className="spin" />
        </div>
      ) : filtered.length > 0 ? (
        <div className="collection-grid">
          {filtered.map((liquor) => (
            <LiquorCard key={liquor.id} liquor={liquor} />
          ))}
        </div>
      ) : (
        <div className="collection-empty">
          <Wine size={48} className="collection-empty-icon" />
          <div className="collection-empty-title">{t('collection.noResults')}</div>
          <div className="collection-empty-text">
            {search || category !== 'all'
              ? t('collection.noResultsHint')
              : t('collection.emptyHint')}
          </div>
        </div>
      )}
    </div>
  );
}
