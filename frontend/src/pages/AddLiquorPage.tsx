import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Loader,
  Sparkles,
  Upload,
  Wine,
  Clock,
  CheckCircle,
  AlertCircle,
  X,
  RotateCcw,
  Trash2,
  ChevronDown,
  ChevronUp,
  Database,
  Bot,
  Search,
  FileText,
  Info,
} from 'lucide-react';
import { Button } from '../components/ui/Button';
import { Chip } from '../components/ui/Chip';
import { useSearchQueue } from '../hooks/SearchQueueContext';
import type { SearchItem } from '../hooks/SearchQueueContext';
import { createLiquor, uploadImage } from '../api/client';
import { useLanguage } from '../i18n/LanguageContext';
import type { LiquorProfile } from '../types/liquor';
import '../components/ui/Input.css';
import './AddLiquorPage.css';

export function AddLiquorPage() {
  const navigate = useNavigate();
  const { items, addSearch, removeItem, clearAll, retryItem } = useSearchQueue();
  const { locale, t } = useLanguage();
  const [query, setQuery] = useState('');
  const [provider, setProvider] = useState('claude');
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [savingId, setSavingId] = useState<string | null>(null);
  const [imageFiles, setImageFiles] = useState<Record<string, File>>({});
  const [imagePreviews, setImagePreviews] = useState<Record<string, string>>({});
  const fileInputRef = useRef<HTMLInputElement>(null);
  const activeUploadId = useRef<string | null>(null);

  const handleSearch = () => {
    if (!query.trim()) return;
    addSearch(query.trim(), provider);
    setQuery('');
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.nativeEvent.isComposing) handleSearch();
  };

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    const id = activeUploadId.current;
    if (!file || !id) return;
    setImageFiles((prev) => ({ ...prev, [id]: file }));
    setImagePreviews((prev) => ({ ...prev, [id]: URL.createObjectURL(file) }));
  };

  const handleUploadClick = (id: string) => {
    activeUploadId.current = id;
    fileInputRef.current?.click();
  };

  const toggleExpand = (id: string) => {
    setExpandedId((prev) => (prev === id ? null : id));
  };

  const handleSave = async (item: SearchItem) => {
    if (!item.result) return;
    setSavingId(item.id);
    try {
      const result = item.result;
      let imageUrl: string | undefined;
      if (imageFiles[item.id]) {
        const res = await uploadImage(imageFiles[item.id]);
        imageUrl = res.data.url;
      } else if (result.imageUrl) {
        imageUrl = result.imageUrl;
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
        tastingDetail: result.tastingDetail,
        tastingDetailKo: result.tastingDetailKo,
        pairing: result.pairing,
        pairingKo: result.pairingKo,
        nameKo: result.nameKo,
        typeKo: result.typeKo,
        aboutKo: result.aboutKo,
        heritageKo: result.heritageKo,
        tastingNotesKo: result.tastingNotesKo,
        dataSource: result.dataSource,
        status: 'active',
      };

      const res = await createLiquor(data);
      removeItem(item.id);
      navigate(`/liquor/${res.data.id}`);
    } catch {
      alert('Failed to save liquor');
    } finally {
      setSavingId(null);
    }
  };

  const handleSuggestionClick = (suggestionName: string) => {
    addSearch(suggestionName, provider);
  };

  const statusIcon = (status: SearchItem['status']) => {
    switch (status) {
      case 'pending':
        return <Clock size={16} className="queue-icon queue-icon-pending" />;
      case 'searching':
        return <Loader size={16} className="spin queue-icon queue-icon-searching" />;
      case 'done':
        return <CheckCircle size={16} className="queue-icon queue-icon-done" />;
      case 'not_found':
        return <AlertCircle size={16} className="queue-icon queue-icon-not-found" />;
      case 'error':
        return <AlertCircle size={16} className="queue-icon queue-icon-error" />;
    }
  };

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
            disabled={!query.trim()}
            icon={<Sparkles size={16} />}
          >
            {t('addLiquor.search')}
          </Button>
        </div>
        <div className="ai-provider-toggle">
          <Chip label="Claude" active={provider === 'claude'} onClick={() => setProvider('claude')} />
          <Chip label="OpenAI" active={provider === 'openai'} onClick={() => setProvider('openai')} />
        </div>
      </div>

      {/* Hidden file input shared across queue items */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        className="image-upload-input"
        onChange={handleImageChange}
      />

      {/* Queue List */}
      <div className="queue-section">
        {items.length > 0 && (
          <div className="queue-header">
            <span className="queue-count">{items.length} item{items.length !== 1 ? 's' : ''}</span>
            <Button variant="ghost" size="sm" icon={<Trash2 size={14} />} onClick={clearAll}>
              {t('addLiquor.clearAll')}
            </Button>
          </div>
        )}

        {items.length === 0 && (
          <div className="queue-empty">
            <Wine size={32} className="queue-empty-icon" />
            <div className="queue-empty-text">{t('addLiquor.queueEmpty')}</div>
          </div>
        )}

        <div className="queue-list">
          {items.map((item) => (
            <QueueItem
              key={item.id}
              item={item}
              expanded={expandedId === item.id}
              locale={locale}
              t={t}
              saving={savingId === item.id}
              imagePreview={imagePreviews[item.id]}
              statusIcon={statusIcon(item.status)}
              onToggle={() => toggleExpand(item.id)}
              onRemove={() => removeItem(item.id)}
              onRetry={() => retryItem(item.id)}
              onSave={() => handleSave(item)}
              onUploadClick={() => handleUploadClick(item.id)}
              onSuggestionClick={handleSuggestionClick}
            />
          ))}
        </div>
      </div>
    </div>
  );
}

interface QueueItemProps {
  item: SearchItem;
  expanded: boolean;
  locale: string;
  t: (key: string) => string;
  saving: boolean;
  imagePreview?: string;
  statusIcon: React.ReactNode;
  onToggle: () => void;
  onRemove: () => void;
  onRetry: () => void;
  onSave: () => void;
  onUploadClick: () => void;
  onSuggestionClick: (name: string) => void;
}

function QueueItem({
  item,
  expanded,
  locale,
  t,
  saving,
  imagePreview,
  statusIcon,
  onToggle,
  onRemove,
  onRetry,
  onSave,
  onUploadClick,
  onSuggestionClick,
}: QueueItemProps) {
  const result = item.result;

  const displayName = result
    ? locale === 'ko' && result.nameKo
      ? result.nameKo
      : result.name
    : item.query;

  const displayType = result
    ? locale === 'ko' && result.typeKo
      ? result.typeKo
      : result.type
    : undefined;

  const displayAbout = result
    ? locale === 'ko' && result.aboutKo
      ? result.aboutKo
      : result.about
    : undefined;

  const displayHeritage = result
    ? locale === 'ko' && result.heritageKo
      ? result.heritageKo
      : result.heritage
    : undefined;

  const displayTastingNotes = result
    ? locale === 'ko' && result.tastingNotesKo
      ? result.tastingNotesKo
      : result.tastingNotes
    : undefined;

  const displayTastingDetail = result
    ? locale === 'ko' && result.tastingDetailKo
      ? result.tastingDetailKo
      : result.tastingDetail
    : undefined;

  const displayPairing = result
    ? locale === 'ko' && result.pairingKo
      ? result.pairingKo
      : result.pairing
    : undefined;

  const profile: LiquorProfile | undefined = result?.profile;

  return (
    <div className={`queue-item queue-item-${item.status}`}>
      <div className="queue-item-header" onClick={item.status === 'done' || item.status === 'not_found' ? onToggle : undefined}>
        <div className="queue-item-left">
          {statusIcon}
          <div className="queue-item-info">
            <span className="queue-item-name">{displayName}</span>
            {item.status === 'pending' && (
              <span className="queue-item-status">{t('addLiquor.pending')}</span>
            )}
            {item.status === 'searching' && (
              <span className="queue-item-status">
                {item.progressSteps.length > 0
                  ? (locale === 'ko'
                    ? item.progressSteps[item.progressSteps.length - 1].messageKo
                    : item.progressSteps[item.progressSteps.length - 1].message)
                  : t('addLiquor.searching')}
              </span>
            )}
            {item.status === 'error' && (
              <span className="queue-item-status queue-item-status-error">{item.error}</span>
            )}
            {item.status === 'done' && displayType && (
              <span className="queue-item-status">{displayType}</span>
            )}
            {item.status === 'not_found' && (
              <span className="queue-item-status queue-item-status-not-found">
                {locale === 'ko'
                  ? (item.suggestions?.messageKo || '검색 결과를 찾을 수 없습니다')
                  : (item.suggestions?.message || 'No results found')}
              </span>
            )}
          </div>
        </div>
        <div className="queue-item-actions">
          {item.status === 'error' && (
            <button className="queue-action-btn" onClick={onRetry} title={t('addLiquor.retry')}>
              <RotateCcw size={14} />
            </button>
          )}
          {(item.status === 'done' || item.status === 'not_found') && (
            expanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />
          )}
          {item.status !== 'searching' && (
            <button className="queue-action-btn" onClick={onRemove} title={t('addLiquor.remove')}>
              <X size={14} />
            </button>
          )}
        </div>
      </div>

      {/* Progress steps for searching items */}
      {item.status === 'searching' && item.progressSteps.length > 0 && (
        <div className="queue-item-progress">
          {item.progressSteps.map((step, i) => (
            <div
              key={i}
              className={`progress-step ${i === item.progressSteps.length - 1 ? 'progress-step-active' : 'progress-step-done'}`}
            >
              {i === item.progressSteps.length - 1 ? (
                <Loader size={12} className="spin" />
              ) : (
                <CheckCircle size={12} />
              )}
              <span>{locale === 'ko' ? step.messageKo : step.message}</span>
            </div>
          ))}
        </div>
      )}

      {/* Suggestions UI for not_found */}
      {expanded && item.status === 'not_found' && item.suggestions && (
        <div className="queue-item-detail">
          <div className="suggestions-section">
            <div className="suggestions-header">
              <Search size={16} />
              <span>{locale === 'ko' ? '이런 주류를 찾으셨나요?' : 'Did you mean one of these?'}</span>
            </div>
            <div className="suggestions-list">
              {item.suggestions.suggestions.map((suggestion) => (
                <button
                  key={suggestion.name}
                  className="suggestion-card"
                  onClick={() => onSuggestionClick(suggestion.name)}
                >
                  <div className="suggestion-name">
                    {locale === 'ko' && suggestion.nameKo ? suggestion.nameKo : suggestion.name}
                  </div>
                  <div className="suggestion-reason">
                    {locale === 'ko' && suggestion.reasonKo ? suggestion.reasonKo : suggestion.reason}
                  </div>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {expanded && result && (
        <div className="queue-item-detail">
          <div className="data-source-badge-container">
            {result.dataSource === 'database' ? (
              <span className="data-source-badge data-source-db">
                <Database size={12} />
                {locale === 'ko' ? '외부 DB 기반' : 'Database-verified'}
                {result.dataSources && result.dataSources.length > 0 && (
                  <span className="data-source-detail">({result.dataSources.join(', ')})</span>
                )}
              </span>
            ) : (
              <span className="data-source-badge data-source-ai">
                <Bot size={12} />
                {locale === 'ko' ? 'AI 생성 데이터' : 'AI-generated'}
              </span>
            )}
          </div>

          {/* Source Transparency */}
          {result.collectedSources && result.collectedSources.length > 0 && (
            <div className="sources-section">
              <div className="sources-header">
                <FileText size={14} />
                <span>{locale === 'ko' ? '수집된 원문 소스' : 'Collected Sources'}</span>
                <span className="sources-count">{result.collectedSources.length}</span>
              </div>
              <div className="sources-list">
                {result.collectedSources.map((src, i) => (
                  <div key={i} className="source-card">
                    <div className="source-card-header">
                      <span className="source-name">{src.source}</span>
                      <span className="source-fields">{src.fieldsFound.length} fields</span>
                    </div>
                    {src.name && <div className="source-match-name">{src.name}</div>}
                    <div className="source-highlights">
                      {Object.entries(src.highlights).filter(([, v]) => v).map(([key, value]) => (
                        <div key={key} className="source-highlight">
                          <span className="source-highlight-key">{key}</span>
                          <span className="source-highlight-value">{value}</span>
                        </div>
                      ))}
                    </div>
                    {src.originalTexts && Object.keys(src.originalTexts).length > 0 && (
                      <div className="source-original-texts">
                        {Object.entries(src.originalTexts).map(([key, text]) => (
                          <div key={key} className="source-original-text">
                            <span className="source-original-label">
                              {key === 'description' ? (locale === 'ko' ? '원문 설명' : 'Description')
                                : key === 'nose' ? 'Nose'
                                : key === 'palate' ? 'Palate'
                                : key === 'finish' ? 'Finish'
                                : key === 'flavor_notes' ? (locale === 'ko' ? '풍미 노트' : 'Flavor Notes')
                                : key === 'review' ? (locale === 'ko' ? '리뷰' : 'Review')
                                : key === 'tasting_note' ? (locale === 'ko' ? '테이스팅 노트' : 'Tasting Note')
                                : key}
                            </span>
                            <span className="source-original-value">{text}</span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
              {result.synthesisReasoning && (
                <div className="synthesis-reasoning">
                  <div className="synthesis-reasoning-header">
                    <Info size={14} />
                    <span>{locale === 'ko' ? 'AI 종합 근거' : 'AI Synthesis Reasoning'}</span>
                  </div>
                  <div className="synthesis-reasoning-text">{result.synthesisReasoning}</div>
                </div>
              )}
            </div>
          )}

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

          {(displayTastingDetail || (displayTastingNotes && displayTastingNotes.length > 0)) && (
            <div className="preview-section">
              <div className="preview-section-title">{t('addLiquor.tastingNotes')}</div>
              {displayTastingDetail && (
                <div className="preview-section-text">{displayTastingDetail}</div>
              )}
              {displayTastingNotes && displayTastingNotes.length > 0 && (
                <div className="tasting-notes">
                  {displayTastingNotes.map((note) => (
                    <span key={note} className="tasting-note">{note}</span>
                  ))}
                </div>
              )}
            </div>
          )}

          {displayPairing && displayPairing.length > 0 && (
            <div className="preview-section">
              <div className="preview-section-title">{t('addLiquor.pairing')}</div>
              <div className="tasting-notes">
                {displayPairing.map((item) => (
                  <span key={item} className="tasting-note">{item}</span>
                ))}
              </div>
            </div>
          )}

          <div className="image-upload-section">
            <div className="preview-section-title">{t('addLiquor.image')}</div>
            <div className="image-upload-area">
              <div className="image-preview">
                {imagePreview ? (
                  <img src={imagePreview} alt="Preview" />
                ) : result.imageUrl ? (
                  <img src={result.imageUrl} alt="AI Generated" />
                ) : (
                  <Wine size={28} />
                )}
              </div>
              <Button
                variant="secondary"
                size="sm"
                icon={<Upload size={14} />}
                onClick={onUploadClick}
              >
                {t('addLiquor.uploadImage')}
              </Button>
            </div>
          </div>

          <div className="add-actions">
            <Button onClick={onSave} disabled={saving}>
              {saving ? t('addLiquor.saving') : t('addLiquor.addToCollection')}
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
