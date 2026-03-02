import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loader, Sparkles, Upload, Wine } from 'lucide-react';
import { Button } from '../components/ui/Button';
import { Chip } from '../components/ui/Chip';
import { useAiSearch } from '../hooks/useAiSearch';
import { createLiquor, uploadImage } from '../api/client';
import { useLanguage } from '../i18n/LanguageContext';
import type { LiquorProfile } from '../types/liquor';
import '../components/ui/Input.css';
import './AddLiquorPage.css';

export function AddLiquorPage() {
  const navigate = useNavigate();
  const { result, loading, error, search, reset } = useAiSearch();
  const { locale, t } = useLanguage();
  const [query, setQuery] = useState('');
  const [provider, setProvider] = useState('claude');
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [imagePreviewUrl, setImagePreviewUrl] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleSearch = () => {
    if (!query.trim()) return;
    search(query.trim(), provider);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSearch();
  };

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setImageFile(file);
    setImagePreviewUrl(URL.createObjectURL(file));
  };

  const handleSave = async () => {
    if (!result) return;
    setSaving(true);
    try {
      let imageUrl: string | undefined;
      if (imageFile) {
        const res = await uploadImage(imageFile);
        imageUrl = res.data.url;
      }

      const data = {
        name: result.name,
        type: result.type,
        category: result.category,
        abv: result.abv,
        age: result.age,
        score: result.score,
        price: result.price,
        origin: result.origin,
        region: result.region,
        volume: result.volume,
        imageUrl,
        about: result.about,
        heritage: result.heritage,
        profile: result.profile,
        tastingNotes: result.tastingNotes,
        nameKo: result.nameKo,
        typeKo: result.typeKo,
        aboutKo: result.aboutKo,
        heritageKo: result.heritageKo,
        tastingNotesKo: result.tastingNotesKo,
        status: 'active',
      };

      const res = await createLiquor(data);
      navigate(`/liquor/${res.data.id}`);
    } catch {
      alert('Failed to save liquor');
    } finally {
      setSaving(false);
    }
  };

  const profile: LiquorProfile | undefined = result?.profile;

  const displayName = result ? (locale === 'ko' && result.nameKo ? result.nameKo : result.name) : '';
  const displayType = result ? (locale === 'ko' && result.typeKo ? result.typeKo : result.type) : undefined;
  const displayAbout = result ? (locale === 'ko' && result.aboutKo ? result.aboutKo : result.about) : undefined;
  const displayHeritage = result ? (locale === 'ko' && result.heritageKo ? result.heritageKo : result.heritage) : undefined;
  const displayTastingNotes = result ? (locale === 'ko' && result.tastingNotesKo ? result.tastingNotesKo : result.tastingNotes) : undefined;

  return (
    <div className="add-page">
      <h1 className="add-title">{t('addLiquor.title')}</h1>

      <div className="ai-search-section">
        <div className="ai-search-label">{t('addLiquor.searchWithAi')}</div>
        <div className="ai-search-row">
          <input
            className="input"
            type="text"
            placeholder={t('addLiquor.inputPlaceholder')}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
          />
          <Button
            onClick={handleSearch}
            disabled={!query.trim() || loading}
            icon={loading ? <Loader size={16} className="spin" /> : <Sparkles size={16} />}
          >
            {t('addLiquor.search')}
          </Button>
        </div>
        <div className="ai-provider-toggle">
          <Chip label="Claude" active={provider === 'claude'} onClick={() => setProvider('claude')} />
          <Chip label="OpenAI" active={provider === 'openai'} onClick={() => setProvider('openai')} />
        </div>
      </div>

      {error && <div className="ai-error">{error}</div>}

      {loading && (
        <div className="ai-loading">
          <Loader size={32} className="spin" />
          <div className="ai-loading-text">{t('addLiquor.searching')}</div>
        </div>
      )}

      {result && !loading && (
        <div className="ai-preview">
          <h2 className="ai-preview-title">{displayName}</h2>

          <div className="preview-grid">
            <PreviewField label={t('addLiquor.type')} value={displayType} />
            <PreviewField label={t('addLiquor.category')} value={result.category} />
            <PreviewField label={t('addLiquor.abv')} value={result.abv ? `${result.abv}%` : undefined} />
            <PreviewField label={t('addLiquor.age')} value={result.age} />
            <PreviewField label={t('addLiquor.score')} value={result.score?.toString()} />
            <PreviewField label={t('addLiquor.price')} value={result.price} />
            <PreviewField label={t('addLiquor.origin')} value={result.origin} />
            <PreviewField label={t('addLiquor.region')} value={result.region} />
            <PreviewField label={t('addLiquor.volume')} value={result.volume} />
          </div>

          {displayAbout && (
            <div className="preview-section">
              <div className="preview-section-title">{t('addLiquor.about')}</div>
              <div className="preview-section-text">{displayAbout}</div>
            </div>
          )}

          {displayHeritage && (
            <div className="preview-section">
              <div className="preview-section-title">{t('addLiquor.heritage')}</div>
              <div className="preview-section-text">{displayHeritage}</div>
            </div>
          )}

          {profile && Object.keys(profile).length > 0 && (
            <div className="preview-section">
              <div className="preview-section-title">{t('addLiquor.profile')}</div>
              <div className="profile-bars">
                {Object.entries(profile).map(([key, value]) => (
                  <div key={key} className="profile-bar">
                    <span className="profile-bar-label">{key}</span>
                    <div className="profile-bar-track">
                      <div className="profile-bar-fill" style={{ width: `${value}%` }} />
                    </div>
                    <span className="profile-bar-value">{value}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {displayTastingNotes && displayTastingNotes.length > 0 && (
            <div className="preview-section">
              <div className="preview-section-title">{t('addLiquor.tastingNotes')}</div>
              <div className="tasting-notes">
                {displayTastingNotes.map((note) => (
                  <span key={note} className="tasting-note">{note}</span>
                ))}
              </div>
            </div>
          )}

          <div className="image-upload-section">
            <div className="preview-section-title">{t('addLiquor.image')}</div>
            <div className="image-upload-area">
              <div className="image-preview">
                {imagePreviewUrl ? (
                  <img src={imagePreviewUrl} alt="Preview" />
                ) : (
                  <Wine size={28} />
                )}
              </div>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                className="image-upload-input"
                onChange={handleImageChange}
              />
              <Button
                variant="secondary"
                size="sm"
                icon={<Upload size={14} />}
                onClick={() => fileInputRef.current?.click()}
              >
                {t('addLiquor.uploadImage')}
              </Button>
            </div>
          </div>

          <div className="add-actions">
            <Button onClick={handleSave} disabled={saving}>
              {saving ? t('addLiquor.saving') : t('addLiquor.addToCollection')}
            </Button>
            <Button variant="ghost" onClick={reset}>
              {t('addLiquor.reset')}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

function PreviewField({ label, value }: { label: string; value?: string }) {
  if (!value) return null;
  return (
    <div className="preview-field">
      <span className="preview-field-label">{label}</span>
      <span className="preview-field-value">{value}</span>
    </div>
  );
}
