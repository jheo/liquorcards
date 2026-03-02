import { Wine } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import type { Liquor } from '../../types/liquor';
import { useLanguage } from '../../i18n/LanguageContext';
import './LiquorCard.css';

interface LiquorCardProps {
  liquor: Liquor;
}

function getCategoryMeta(liquor: Liquor): string[] {
  const meta: string[] = [];
  const cat = liquor.category?.toLowerCase();

  if (cat === 'whisky' || cat === 'whiskey') {
    if (liquor.age) meta.push(liquor.age);
    if (liquor.region) meta.push(liquor.region);
    if (liquor.abv) meta.push(`${liquor.abv}%`);
  } else if (cat === 'wine') {
    if (liquor.region) meta.push(liquor.region);
    if (liquor.age) meta.push(liquor.age);
    if (liquor.abv) meta.push(`${liquor.abv}%`);
  } else if (cat === 'beer') {
    if (liquor.type) meta.push(liquor.type);
    if (liquor.abv) meta.push(`${liquor.abv}%`);
  } else if (cat === 'sake') {
    if (liquor.type) meta.push(liquor.type);
    if (liquor.region) meta.push(liquor.region);
    if (liquor.abv) meta.push(`${liquor.abv}%`);
  } else {
    if (liquor.abv) meta.push(`${liquor.abv}%`);
    if (liquor.region) meta.push(liquor.region);
  }

  return meta.slice(0, 3);
}

export function LiquorCard({ liquor }: LiquorCardProps) {
  const navigate = useNavigate();
  const { locale } = useLanguage();

  const displayName = locale === 'ko' && liquor.nameKo ? liquor.nameKo : liquor.name;
  const displayType = locale === 'ko' && liquor.typeKo ? liquor.typeKo : liquor.type;
  const metaChips = getCategoryMeta(liquor);

  return (
    <div className="liquor-card" onClick={() => navigate(`/liquor/${liquor.id}`)}>
      <div className="liquor-card-image">
        {liquor.imageUrl ? (
          <img src={liquor.imageUrl} alt={displayName} />
        ) : (
          <div className="liquor-card-placeholder">
            <Wine size={40} />
          </div>
        )}
        {liquor.score != null && (
          <div className="liquor-card-score">{liquor.score}</div>
        )}
      </div>
      <div className="liquor-card-body">
        <div className="liquor-card-name">{displayName}</div>
        <div className="liquor-card-type">{displayType}</div>
        {liquor.origin && <div className="liquor-card-origin">{liquor.origin}</div>}
        {metaChips.length > 0 && (
          <div className="liquor-card-meta-chips">
            {metaChips.map((chip) => (
              <span key={chip} className="liquor-card-meta-chip">{chip}</span>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
