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
  Edit3,
  Save,
  XCircle,
} from 'lucide-react';
import { Button } from '../components/ui/Button';
import { StatBadge } from '../components/ui/StatBadge';
import { useLiquor } from '../hooks/useLiquor';
import { deleteLiquor, updateStatus, updateLiquor } from '../api/client';
import { useLanguage } from '../i18n/LanguageContext';
import type { LiquorProfile } from '../types/liquor';
import './LiquorDetailPage.css';

export function LiquorDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { liquor, loading, refetch } = useLiquor(id ? Number(id) : undefined);
  const { locale, t } = useLanguage();
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [editData, setEditData] = useState<Record<string, unknown>>({});
  const [saving, setSaving] = useState(false);

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
  const displayTastingDetail = locale === 'ko' && liquor.tastingDetailKo ? liquor.tastingDetailKo : liquor.tastingDetail;
  const displayPairing = locale === 'ko' && liquor.pairingKo ? liquor.pairingKo : (liquor.pairing ?? []);
  const displayVolume = liquor.volumeMl ? `${liquor.volumeMl}ml` : liquor.volume;
  const displayPrice = locale === 'ko' && liquor.priceKrw
    ? `₩${liquor.priceKrw.toLocaleString()}`
    : liquor.priceUsd
      ? `$${liquor.priceUsd.toFixed(0)}`
      : liquor.price;

  const profile: LiquorProfile = liquor.profile ?? {};

  const handleArchive = async () => {
    await updateStatus(liquor.id, 'archived');
    navigate('/');
  };

  const handleDelete = async () => {
    await deleteLiquor(liquor.id);
    navigate('/');
  };

  const startEdit = () => {
    setEditData({
      name: liquor.name,
      nameKo: liquor.nameKo || '',
      type: liquor.type || '',
      typeKo: liquor.typeKo || '',
      category: liquor.category || '',
      abv: liquor.abv ?? '',
      age: liquor.age || '',
      score: liquor.score ?? '',
      price: liquor.price || '',
      priceUsd: liquor.priceUsd ?? '',
      priceKrw: liquor.priceKrw ?? '',
      origin: liquor.origin || '',
      region: liquor.region || '',
      volume: liquor.volume || '',
      volumeMl: liquor.volumeMl ?? '',
      about: liquor.about || '',
      aboutKo: liquor.aboutKo || '',
      heritage: liquor.heritage || '',
      heritageKo: liquor.heritageKo || '',
      tastingDetail: liquor.tastingDetail || '',
      tastingDetailKo: liquor.tastingDetailKo || '',
    });
    setEditMode(true);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const data: Record<string, unknown> = {};
      for (const [key, value] of Object.entries(editData)) {
        if (value === '') continue;
        if (['abv', 'priceUsd'].includes(key)) {
          data[key] = Number(value);
        } else if (['score', 'priceKrw', 'volumeMl'].includes(key)) {
          data[key] = Math.round(Number(value));
        } else {
          data[key] = value;
        }
      }
      await updateLiquor(liquor.id, data);
      setEditMode(false);
      refetch();
    } catch {
      alert('Failed to save');
    } finally {
      setSaving(false);
    }
  };

  const ed = (field: string) => editData[field] as string ?? '';
  const setEd = (field: string, value: string) => setEditData((prev) => ({ ...prev, [field]: value }));

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
          {editMode ? (
            <>
              <input className="detail-edit-input detail-edit-name" value={ed('name')} onChange={(e) => setEd('name', e.target.value)} placeholder="Name" />
              <input className="detail-edit-input" value={ed('nameKo')} onChange={(e) => setEd('nameKo', e.target.value)} placeholder="한국어 이름" />
            </>
          ) : (
            <>
              <h1 className="detail-hero-name">{displayName}</h1>
              <div className="detail-hero-type">{displayType}</div>
            </>
          )}
        </div>
      </div>

      {/* Edit toolbar */}
      <div className="detail-edit-toolbar">
        {editMode ? (
          <>
            <Button size="sm" icon={<Save size={14} />} onClick={handleSave} disabled={saving}>
              {saving ? '...' : t('detail.save')}
            </Button>
            <Button variant="ghost" size="sm" icon={<XCircle size={14} />} onClick={() => setEditMode(false)}>
              {t('detail.cancelEdit')}
            </Button>
          </>
        ) : (
          <Button variant="secondary" size="sm" icon={<Edit3 size={14} />} onClick={startEdit}>
            {t('detail.edit')}
          </Button>
        )}
      </div>

      {editMode ? (
        <div className="detail-edit-grid">
          <EditField label={t('addLiquor.type')} value={ed('type')} onChange={(v) => setEd('type', v)} />
          <EditField label={t('addLiquor.type') + ' (KO)'} value={ed('typeKo')} onChange={(v) => setEd('typeKo', v)} />
          <EditField label={t('addLiquor.category')} value={ed('category')} onChange={(v) => setEd('category', v)} />
          <EditField label={t('addLiquor.abv')} value={String(ed('abv'))} onChange={(v) => setEd('abv', v)} type="number" />
          <EditField label={t('addLiquor.age')} value={ed('age')} onChange={(v) => setEd('age', v)} />
          <EditField label={t('addLiquor.score')} value={String(ed('score'))} onChange={(v) => setEd('score', v)} type="number" />
          <EditField label={t('addLiquor.price')} value={ed('price')} onChange={(v) => setEd('price', v)} />
          <EditField label="Price (USD)" value={String(ed('priceUsd'))} onChange={(v) => setEd('priceUsd', v)} type="number" />
          <EditField label="Price (KRW)" value={String(ed('priceKrw'))} onChange={(v) => setEd('priceKrw', v)} type="number" />
          <EditField label={t('addLiquor.origin')} value={ed('origin')} onChange={(v) => setEd('origin', v)} />
          <EditField label={t('addLiquor.region')} value={ed('region')} onChange={(v) => setEd('region', v)} />
          <EditField label={t('addLiquor.volume')} value={ed('volume')} onChange={(v) => setEd('volume', v)} />
          <EditField label="Volume (ml)" value={String(ed('volumeMl'))} onChange={(v) => setEd('volumeMl', v)} type="number" />
          <EditTextarea label={t('detail.about')} value={ed('about')} onChange={(v) => setEd('about', v)} />
          <EditTextarea label={t('detail.about') + ' (KO)'} value={ed('aboutKo')} onChange={(v) => setEd('aboutKo', v)} />
          <EditTextarea label={t('detail.heritage')} value={ed('heritage')} onChange={(v) => setEd('heritage', v)} />
          <EditTextarea label={t('detail.heritage') + ' (KO)'} value={ed('heritageKo')} onChange={(v) => setEd('heritageKo', v)} />
          <EditTextarea label={t('detail.tastingDetail')} value={ed('tastingDetail')} onChange={(v) => setEd('tastingDetail', v)} />
          <EditTextarea label={t('detail.tastingDetail') + ' (KO)'} value={ed('tastingDetailKo')} onChange={(v) => setEd('tastingDetailKo', v)} />
        </div>
      ) : (
        <>
          <div className="detail-meta">
            {liquor.abv != null && (
              <StatBadge icon={<Percent size={14} />} label={t('addLiquor.abv')} value={`${liquor.abv}%`} />
            )}
            {liquor.age && (
              <StatBadge icon={<Clock size={14} />} label={t('addLiquor.age')} value={liquor.age} />
            )}
            {displayVolume && (
              <StatBadge icon={<Beaker size={14} />} label={t('addLiquor.volume')} value={displayVolume} />
            )}
            {displayPrice && (
              <StatBadge icon={<DollarSign size={14} />} label={t('addLiquor.price')} value={displayPrice} />
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

          {(displayTastingDetail || displayTastingNotes.length > 0) && (
            <div className="detail-card">
              <h3 className="detail-card-title">{t('detail.tastingNotes')}</h3>
              {displayTastingDetail && (
                <p className="detail-card-text">{displayTastingDetail}</p>
              )}
              {displayTastingNotes.length > 0 && (
                <div className="detail-notes">
                  {displayTastingNotes.map((note) => (
                    <span key={note} className="detail-note">{note}</span>
                  ))}
                </div>
              )}
            </div>
          )}

          {displayPairing.length > 0 && (
            <div className="detail-card">
              <h3 className="detail-card-title">{t('detail.pairing')}</h3>
              <div className="detail-notes">
                {displayPairing.map((item) => (
                  <span key={item} className="detail-note">{item}</span>
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
        </>
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

function EditField({ label, value, onChange, type = 'text' }: { label: string; value: string; onChange: (v: string) => void; type?: string }) {
  return (
    <div className="detail-edit-field">
      <label className="detail-edit-label">{label}</label>
      <input className="detail-edit-input" type={type} value={value} onChange={(e) => onChange(e.target.value)} />
    </div>
  );
}

function EditTextarea({ label, value, onChange }: { label: string; value: string; onChange: (v: string) => void }) {
  return (
    <div className="detail-edit-field detail-edit-field-full">
      <label className="detail-edit-label">{label}</label>
      <textarea className="detail-edit-textarea" value={value} onChange={(e) => onChange(e.target.value)} rows={3} />
    </div>
  );
}
