import { Wine } from 'lucide-react';
import type { Liquor, LiquorProfile } from '../../types/liquor';
import { useLanguage } from '../../i18n/LanguageContext';
import './PrintCard.css';

interface PrintCardProps {
  liquor: Liquor;
}

export function PrintCard({ liquor }: PrintCardProps) {
  const { locale } = useLanguage();
  const profile: LiquorProfile = liquor.profile ?? {};
  const tastingNotes = locale === 'ko' && liquor.tastingNotesKo ? liquor.tastingNotesKo : (liquor.tastingNotes ?? []);

  const displayName = locale === 'ko' && liquor.nameKo ? liquor.nameKo : liquor.name;
  const displayType = locale === 'ko' && liquor.typeKo ? liquor.typeKo : liquor.type;

  const profileEntries = Object.entries(profile).slice(0, 5);

  return (
    <div className="print-card">
      <div className="print-card-image">
        {liquor.imageUrl ? (
          <img src={liquor.imageUrl} alt={displayName} />
        ) : (
          <div className="print-card-image-placeholder">
            <Wine size={28} />
          </div>
        )}
      </div>
      <div className="print-card-body">
        <div className="print-card-header">
          <div className="print-card-name">{displayName}</div>
          {liquor.score != null && (
            <div className="print-card-score">{liquor.score}</div>
          )}
        </div>
        <div className="print-card-meta">
          <span>{displayType}</span>
          {liquor.origin && <span>{liquor.origin}</span>}
        </div>
        {profileEntries.length > 0 && (
          <div className="print-card-bars">
            {profileEntries.map(([key, value]) => (
              <div key={key} className="print-card-bar">
                <span className="print-card-bar-label">{key}</span>
                <div className="print-card-bar-track">
                  <div
                    className="print-card-bar-fill"
                    style={{ width: `${value}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        )}
        {tastingNotes.length > 0 && (
          <div className="print-card-notes">
            {tastingNotes.slice(0, 6).map((note) => (
              <span key={note} className="print-card-note-tag">{note}</span>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
