import { describe, it, expect, beforeEach, vi } from 'vitest';
import { screen, fireEvent } from '@testing-library/react';
import { CardExportPage } from '../CardExportPage';
import { renderWithProviders } from '../../test/test-utils';

vi.mock('../../hooks/useLiquors', () => ({
  useLiquors: vi.fn(),
}));

vi.mock('html2canvas', () => ({
  default: vi.fn(() =>
    Promise.resolve({
      toDataURL: () => 'data:image/jpeg;base64,test',
    })
  ),
}));

vi.mock('jspdf', () => ({
  jsPDF: vi.fn().mockImplementation(() => ({
    addPage: vi.fn(),
    addImage: vi.fn(),
    save: vi.fn(),
    internal: { pageSize: { getWidth: () => 297, getHeight: () => 210 } },
  })),
}));

import { useLiquors } from '../../hooks/useLiquors';
const mockUseLiquors = vi.mocked(useLiquors);

const sampleLiquors = [
  {
    id: 1,
    name: 'Macallan 12',
    nameKo: '맥캘란 12년',
    type: 'Whisky',
    typeKo: '위스키',
    score: 88,
    status: 'active',
    profile: { sweetness: 70 },
    tastingNotes: ['honey', 'vanilla'],
    tastingNotesKo: ['꿀', '바닐라'],
  },
  {
    id: 2,
    name: 'Hendricks Gin',
    type: 'Gin',
    score: 85,
    status: 'active',
    profile: {},
    tastingNotes: ['cucumber'],
  },
  {
    id: 3,
    name: 'Archived Wine',
    type: 'Wine',
    status: 'archived',
  },
];

describe('CardExportPage', () => {
  beforeEach(() => {
    localStorage.clear();
    mockUseLiquors.mockReturnValue({
      liquors: sampleLiquors as any,
      loading: false,
      error: null,
    });
  });

  it('should render the export title', () => {
    renderWithProviders(<CardExportPage />);
    expect(screen.getByText('Card Export')).toBeInTheDocument();
  });

  it('should show only active liquors (not archived)', () => {
    renderWithProviders(<CardExportPage />);
    expect(screen.getByText('Macallan 12')).toBeInTheDocument();
    expect(screen.getByText('Hendricks Gin')).toBeInTheDocument();
    expect(screen.queryByText('Archived Wine')).not.toBeInTheDocument();
  });

  it('should show liquor count in select all', () => {
    renderWithProviders(<CardExportPage />);
    expect(screen.getByText(/\(2\)/)).toBeInTheDocument();
  });

  it('should show empty preview when nothing selected', () => {
    renderWithProviders(<CardExportPage />);
    expect(screen.getByText('Select liquors to preview cards')).toBeInTheDocument();
  });

  it('should show selected count as 0 initially', () => {
    renderWithProviders(<CardExportPage />);
    expect(screen.getByText('0 card(s) selected')).toBeInTheDocument();
  });

  it('should toggle item selection when clicked', () => {
    renderWithProviders(<CardExportPage />);
    const item = screen.getByText('Macallan 12');
    fireEvent.click(item);
    expect(screen.getByText('1 card(s) selected')).toBeInTheDocument();
  });

  it('should select all items with the select all label', () => {
    renderWithProviders(<CardExportPage />);
    const selectAllLabel = screen.getByText(/Select all/);
    fireEvent.click(selectAllLabel);
    expect(screen.getByText('2 card(s) selected')).toBeInTheDocument();
  });

  it('should deselect item when clicked again', () => {
    renderWithProviders(<CardExportPage />);
    const item = screen.getByText('Macallan 12');
    fireEvent.click(item);
    fireEvent.click(item);
    expect(screen.getByText('0 card(s) selected')).toBeInTheDocument();
  });

  it('should disable export button when nothing selected', () => {
    renderWithProviders(<CardExportPage />);
    const exportBtn = screen.getByText('Export PDF');
    expect(exportBtn.closest('button')).toBeDisabled();
  });

  it('should filter list based on search input', () => {
    renderWithProviders(<CardExportPage />);
    const searchInput = screen.getByPlaceholderText('Filter liquors...');
    fireEvent.change(searchInput, { target: { value: 'gin' } });
    expect(screen.queryByText('Macallan 12')).not.toBeInTheDocument();
    expect(screen.getByText('Hendricks Gin')).toBeInTheDocument();
  });

  it('should show scores in list items', () => {
    renderWithProviders(<CardExportPage />);
    expect(screen.getByText('88')).toBeInTheDocument();
    expect(screen.getByText('85')).toBeInTheDocument();
  });
});

describe('CardExportPage - Korean locale', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('locale', 'ko');
    mockUseLiquors.mockReturnValue({
      liquors: sampleLiquors as any,
      loading: false,
      error: null,
    });
  });

  it('should show Korean names when locale is ko', () => {
    renderWithProviders(<CardExportPage />);
    expect(screen.getByText('맥캘란 12년')).toBeInTheDocument();
  });

  it('should show Korean export title', () => {
    renderWithProviders(<CardExportPage />);
    expect(screen.getByText('카드 내보내기')).toBeInTheDocument();
  });
});
