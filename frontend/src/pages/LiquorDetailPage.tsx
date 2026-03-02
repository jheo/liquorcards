import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Loader,
  Wine,
  Percent,
  Clock,
  Beaker,
  DollarSign,
  Star,
  Archive,
  Trash2,
} from 'lucide-react';
import { Button } from '../components/ui/Button';
import { StatBadge } from '../components/ui/StatBadge';
import { useLiquor } from '../hooks/useLiquor';
import { deleteLiquor, updateStatus } from '../api/client';
import { useLanguage } from '../i18n/LanguageContext';
import type { LiquorProfile } from '../types/liquor';
import './LiquorDetailPage.css';

export function LiquorDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { liquor, loading } = useLiquor(id ? Number(id) : undefined);
  const { locale, t } = useLanguage();
  const [confirmDelete, setConfirmDelete] = useState(false);

  if (loading) {
    return (
      <div className="detail-loading">
        <Loader size={24} className="spin" />
      </div>
    );
  }

  if (!liquor) {
    return (
      <div className="detail-loading">
        <span>{t('detail.notFound')}</span>
      </div>
    );
  }

  const displayName = locale === 'ko' && liquor.nameKo ? liquor.nameKo : liquor.name;
  const displayType = locale === 'ko' && liquor.typeKo ? liquor.typeKo : liquor.type;
  const displayAbout = locale === 'ko' && liquor.aboutKo ? liquor.aboutKo : liquor.about;
  const displayHeritage = locale === 'ko' && liquor.heritageKo ? liquor.heritageKo : liquor.heritage;
  const displayTastingNotes = locale === 'ko' && liquor.tastingNotesKo ? liquor.tastingNotesKo : (liquor.tastingNotes ?? []);

  const profile: LiquorProfile = liquor.profile ?? {};

  const handleArchive = async () => {
    await updateStatus(liquor.id, 'archived');
    navigate('/');
  };

  const handleDelete = async () => {
    await deleteLiquor(liquor.id);
    navigate('/');
  };

  return (
    <div className="detail-page">
      <button className="detail-back" onClick={() => navigate('/')}>
        <ArrowLeft size={18} />
        {t('detail.backToCollection')}
      </button>

      <div className="detail-hero">
        {liquor.imageUrl ? (
          <img src={liquor.imageUrl} alt={displayName} />
        ) : (
          <div className="detail-hero-placeholder">
            <Wine size={64} />
          </div>
        )}
        <div className="detail-hero-overlay">
          <h1 className="detail-hero-name">{displayName}</h1>
          <div className="detail-hero-type">{displayType}</div>
        </div>
      </div>

      <div className="detail-meta">
        {liquor.abv != null && (
          <StatBadge icon={<Percent size={14} />} label={t('addLiquor.abv')} value={`${liquor.abv}%`} />
        )}
        {liquor.age && (
          <StatBadge icon={<Clock size={14} />} label={t('addLiquor.age')} value={liquor.age} />
        )}
        {liquor.volume && (
          <StatBadge icon={<Beaker size={14} />} label={t('addLiquor.volume')} value={liquor.volume} />
        )}
        {liquor.price && (
          <StatBadge icon={<DollarSign size={14} />} label={t('addLiquor.price')} value={liquor.price} />
        )}
        {liquor.origin && (
          <StatBadge icon={<Wine size={14} />} label={t('addLiquor.origin')} value={liquor.origin} />
        )}
      </div>

      {liquor.score != null && (
        <div className="detail-score">
          <div>
            <div className="detail-score-number">{liquor.score}</div>
            <div className="detail-score-label">{t('detail.score')}</div>
          </div>
          <div className="detail-score-bar">
            <div
              className="detail-score-bar-fill"
              style={{ width: `${liquor.score}%` }}
            />
          </div>
          <Star size={20} color="var(--gold)" />
        </div>
      )}

      {Object.keys(profile).length > 0 && (
        <div className="detail-card">
          <h3 className="detail-card-title">{t('detail.categoryProfile')}</h3>
          <div className="detail-profile-list">
            {Object.entries(profile).map(([key, value]) => (
              <div key={key} className="detail-profile-row">
                <span className="detail-profile-label">{key}</span>
                <div className="detail-profile-track">
                  <div
                    className="detail-profile-fill"
                    style={{ width: `${value}%` }}
                  />
                </div>
                <span className="detail-profile-value">{value}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {displayTastingNotes.length > 0 && (
        <div className="detail-card">
          <h3 className="detail-card-title">{t('detail.tastingNotes')}</h3>
          <div className="detail-notes">
            {displayTastingNotes.map((note) => (
              <span key={note} className="detail-note">{note}</span>
            ))}
          </div>
        </div>
      )}

      {displayAbout && (
        <div className="detail-card">
          <h3 className="detail-card-title">{t('detail.about')}</h3>
          <p className="detail-card-text">{displayAbout}</p>
        </div>
      )}

      {displayHeritage && (
        <div className="detail-card">
          <h3 className="detail-card-title">{t('detail.heritage')}</h3>
          <p className="detail-card-text">{displayHeritage}</p>
        </div>
      )}

      <div className="detail-actions">
        <Button
          variant="secondary"
          icon={<Archive size={16} />}
          onClick={handleArchive}
        >
          {t('detail.archive')}
        </Button>
        {!confirmDelete ? (
          <Button
            variant="destructive"
            icon={<Trash2 size={16} />}
            onClick={() => setConfirmDelete(true)}
          >
            {t('detail.delete')}
          </Button>
        ) : (
          <div className="delete-confirm">
            <span className="delete-confirm-text">{t('detail.confirmDelete')}</span>
            <Button variant="destructive" size="sm" onClick={handleDelete}>
              {t('detail.yesDelete')}
            </Button>
            <Button variant="ghost" size="sm" onClick={() => setConfirmDelete(false)}>
              {t('detail.cancel')}
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
