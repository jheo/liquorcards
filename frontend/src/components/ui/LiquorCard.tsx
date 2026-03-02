import { Wine } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import type { Liquor } from '../../types/liquor';
import { useLanguage } from '../../i18n/LanguageContext';
import './LiquorCard.css';

interface LiquorCardProps {
  liquor: Liquor;
}

export function LiquorCard({ liquor }: LiquorCardProps) {
  const navigate = useNavigate();
  const { locale } = useLanguage();

  const displayName = locale === 'ko' && liquor.nameKo ? liquor.nameKo : liquor.name;
  const displayType = locale === 'ko' && liquor.typeKo ? liquor.typeKo : liquor.type;

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
      </div>
    </div>
  );
}
