import { useState, useMemo, useRef } from 'react';
import { Download, ChevronLeft, ChevronRight, FileDown } from 'lucide-react';
import html2canvas from 'html2canvas';
import { jsPDF } from 'jspdf';
import { SearchBar } from '../components/ui/SearchBar';
import { Button } from '../components/ui/Button';
import { PrintCard } from '../components/export/PrintCard';
import { useLiquors } from '../hooks/useLiquors';
import { useLanguage } from '../i18n/LanguageContext';
import type { Liquor } from '../types/liquor';
import './CardExportPage.css';

const CARDS_PER_PAGE = 2;

export function CardExportPage() {
  const { liquors } = useLiquors();
  const { locale, t } = useLanguage();
  const [search, setSearch] = useState('');
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [page, setPage] = useState(0);
  const [exporting, setExporting] = useState(false);
  const previewRef = useRef<HTMLDivElement>(null);

  const activeLiquors = liquors.filter((l) => l.status !== 'archived');

  const filteredList = useMemo(() => {
    if (!search) return activeLiquors;
    const q = search.toLowerCase();
    return activeLiquors.filter(
      (l) => l.name.toLowerCase().includes(q) || l.type?.toLowerCase().includes(q)
    );
  }, [activeLiquors, search]);

  const selectedLiquors = useMemo(
    () => activeLiquors.filter((l) => selectedIds.has(l.id)),
    [activeLiquors, selectedIds]
  );

  const totalPages = Math.max(1, Math.ceil(selectedLiquors.length / CARDS_PER_PAGE));
  const pageCards = selectedLiquors.slice(
    page * CARDS_PER_PAGE,
    (page + 1) * CARDS_PER_PAGE
  );

  const toggleItem = (id: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleAll = () => {
    if (selectedIds.size === filteredList.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(filteredList.map((l) => l.id)));
    }
  };

  const handleExport = async () => {
    if (selectedLiquors.length === 0 || !previewRef.current) return;
    setExporting(true);

    try {
      const pdf = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
      const pages = Math.ceil(selectedLiquors.length / CARDS_PER_PAGE);

      for (let p = 0; p < pages; p++) {
        if (p > 0) pdf.addPage();

        // Temporarily set page to render correct cards
        setPage(p);
        await new Promise((r) => setTimeout(r, 100));

        const canvas = await html2canvas(previewRef.current, {
          scale: 2,
          backgroundColor: '#FFFFFF',
          useCORS: true,
        });

        const imgData = canvas.toDataURL('image/jpeg', 0.95);
        const pdfWidth = pdf.internal.pageSize.getWidth();
        const pdfHeight = pdf.internal.pageSize.getHeight();
        pdf.addImage(imgData, 'JPEG', 0, 0, pdfWidth, pdfHeight);
      }

      pdf.save('liquor-cards.pdf');
    } catch {
      alert('Failed to export PDF');
    } finally {
      setExporting(false);
    }
  };

  const getDisplayName = (liquor: Liquor) =>
    locale === 'ko' && liquor.nameKo ? liquor.nameKo : liquor.name;
  const getDisplayType = (liquor: Liquor) =>
    locale === 'ko' && liquor.typeKo ? liquor.typeKo : liquor.type;

  return (
    <div className="export-page">
      <div className="export-left">
        <h1 className="export-title">{t('export.title')}</h1>
        <SearchBar
          value={search}
          onChange={setSearch}
          placeholder={t('export.filterPlaceholder')}
        />
        <label className="export-select-all">
          <input
            type="checkbox"
            checked={filteredList.length > 0 && selectedIds.size === filteredList.length}
            onChange={toggleAll}
          />
          {t('export.selectAll')} ({filteredList.length})
        </label>
        <div className="export-list">
          {filteredList.map((liquor) => (
            <ExportListItem
              key={liquor.id}
              liquor={liquor}
              displayName={getDisplayName(liquor)}
              displayType={getDisplayType(liquor)}
              selected={selectedIds.has(liquor.id)}
              onToggle={() => toggleItem(liquor.id)}
            />
          ))}
        </div>
        <div className="export-actions">
          <div className="export-selected-count">
            {selectedIds.size} {t('export.cardsSelected')}
          </div>
          <Button
            onClick={handleExport}
            disabled={selectedLiquors.length === 0 || exporting}
            icon={<Download size={16} />}
          >
            {exporting ? t('export.exporting') : t('export.exportPdf')}
          </Button>
        </div>
      </div>

      <div className="export-right">
        {selectedLiquors.length > 0 ? (
          <>
            <div className="a4-preview" ref={previewRef}>
              {pageCards.map((liquor) => (
                <PrintCard key={liquor.id} liquor={liquor} />
              ))}
            </div>
            {totalPages > 1 && (
              <div className="export-page-nav">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  icon={<ChevronLeft size={16} />}
                >
                  {t('export.prev')}
                </Button>
                <span className="export-page-indicator">
                  {t('export.page')} {page + 1} {t('export.of')} {totalPages}
                </span>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page === totalPages - 1}
                  icon={<ChevronRight size={16} />}
                >
                  {t('export.next')}
                </Button>
              </div>
            )}
          </>
        ) : (
          <div className="export-empty-preview">
            <FileDown size={48} />
            <div className="export-empty-preview-text">
              {t('export.selectToPreview')}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function ExportListItem({
  liquor,
  displayName,
  displayType,
  selected,
  onToggle,
}: {
  liquor: Liquor;
  displayName: string;
  displayType?: string;
  selected: boolean;
  onToggle: () => void;
}) {
  return (
    <div
      className={`export-list-item ${selected ? 'export-list-item-selected' : ''}`}
      onClick={onToggle}
    >
      <input type="checkbox" checked={selected} readOnly />
      <div className="export-list-item-info">
        <div className="export-list-item-name">{displayName}</div>
        <div className="export-list-item-type">{displayType}</div>
      </div>
      {liquor.score != null && (
        <span className="export-list-item-score">{liquor.score}</span>
      )}
    </div>
  );
}
