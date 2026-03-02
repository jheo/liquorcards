import { describe, it, expect, beforeEach, vi } from 'vitest';
import { screen, fireEvent } from '@testing-library/react';
import { renderWithProviders } from '../../test/test-utils';

const mockNavigate = vi.fn();
const mockAddSearch = vi.fn();
const mockRemoveItem = vi.fn();
const mockClearAll = vi.fn();
const mockRetryItem = vi.fn();
const mockCancelItem = vi.fn();
const mockSelectDisambiguation = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('../../hooks/SearchQueueContext', () => ({
  useSearchQueue: () => ({
    items: mockItems,
    addSearch: mockAddSearch,
    removeItem: mockRemoveItem,
    clearAll: mockClearAll,
    retryItem: mockRetryItem,
    cancelItem: mockCancelItem,
    selectDisambiguation: mockSelectDisambiguation,
  }),
  SearchQueueProvider: ({ children }: { children: React.ReactNode }) => children,
}));

vi.mock('../../api/client', () => ({
  createLiquor: vi.fn(() => Promise.resolve({ data: { id: 1 } })),
  uploadImage: vi.fn(() => Promise.resolve({ data: { url: '/images/test.jpg' } })),
}));

let mockItems: any[] = [];

import { AddLiquorPage } from '../AddLiquorPage';

describe('AddLiquorPage - Search submission', () => {
  beforeEach(() => {
    localStorage.clear();
    mockNavigate.mockClear();
    mockAddSearch.mockClear();
    mockCancelItem.mockClear();
    mockSelectDisambiguation.mockClear();
    mockItems = [];
  });

  it('should render the search input', () => {
    renderWithProviders(<AddLiquorPage />);
    expect(screen.getByPlaceholderText(/Macallan 18/)).toBeInTheDocument();
  });

  it('should call addSearch when search button is clicked', () => {
    renderWithProviders(<AddLiquorPage />);
    const input = screen.getByPlaceholderText(/Macallan 18/);
    fireEvent.change(input, { target: { value: 'Macallan 12' } });
    const searchBtn = screen.getByText('Search');
    fireEvent.click(searchBtn);
    expect(mockAddSearch).toHaveBeenCalledWith('Macallan 12');
  });

  it('should not call addSearch with empty query', () => {
    renderWithProviders(<AddLiquorPage />);
    const searchBtn = screen.getByText('Search');
    fireEvent.click(searchBtn);
    expect(mockAddSearch).not.toHaveBeenCalled();
  });

  it('should clear input after search', () => {
    renderWithProviders(<AddLiquorPage />);
    const input = screen.getByPlaceholderText(/Macallan 18/) as HTMLInputElement;
    fireEvent.change(input, { target: { value: 'Macallan' } });
    const searchBtn = screen.getByText('Search');
    fireEvent.click(searchBtn);
    expect(input.value).toBe('');
  });
});

describe('AddLiquorPage - Queue items display', () => {
  beforeEach(() => {
    localStorage.clear();
    mockCancelItem.mockClear();
    mockRetryItem.mockClear();
  });

  it('should show pending item', () => {
    mockItems = [
      { id: '1', query: 'Macallan 12', status: 'pending', progressSteps: [] },
    ];
    renderWithProviders(<AddLiquorPage />);
    expect(screen.getByText('Macallan 12')).toBeInTheDocument();
  });

  it('should show searching status', () => {
    mockItems = [
      { id: '1', query: 'Macallan 12', status: 'searching', progressSteps: [] },
    ];
    renderWithProviders(<AddLiquorPage />);
    expect(screen.getByText('Macallan 12')).toBeInTheDocument();
  });

  it('should show error status', () => {
    mockItems = [
      { id: '1', query: 'Macallan 12', status: 'error', error: 'Network error', progressSteps: [] },
    ];
    renderWithProviders(<AddLiquorPage />);
    expect(screen.getByText('Macallan 12')).toBeInTheDocument();
  });

  it('should show cancelled status', () => {
    mockItems = [
      { id: '1', query: 'Macallan 12', status: 'cancelled', progressSteps: [] },
    ];
    renderWithProviders(<AddLiquorPage />);
    expect(screen.getByText('Macallan 12')).toBeInTheDocument();
  });

  it('should show disambiguation candidates', () => {
    mockItems = [
      {
        id: '1',
        query: '발베니',
        status: 'disambiguation',
        progressSteps: [],
        disambiguationCandidates: [
          { name: 'The Balvenie 12 DoubleWood', nameKo: '발베니 12년', description: '12 year double matured' },
          { name: 'The Balvenie 14 Caribbean', nameKo: '발베니 14년', description: '14 year rum cask' },
        ],
      },
    ];
    renderWithProviders(<AddLiquorPage />);
    expect(screen.getByText('The Balvenie 12 DoubleWood')).toBeInTheDocument();
    expect(screen.getByText('The Balvenie 14 Caribbean')).toBeInTheDocument();
  });

  it('should call selectDisambiguation when candidate is clicked', () => {
    mockItems = [
      {
        id: '1',
        query: '발베니',
        status: 'disambiguation',
        progressSteps: [],
        disambiguationCandidates: [
          { name: 'The Balvenie 12 DoubleWood', nameKo: '발베니 12년', description: '12 year' },
        ],
      },
    ];
    renderWithProviders(<AddLiquorPage />);
    fireEvent.click(screen.getByText('The Balvenie 12 DoubleWood'));
    expect(mockSelectDisambiguation).toHaveBeenCalledWith('1', 'The Balvenie 12 DoubleWood');
  });

  it('should show done item with result data', () => {
    mockItems = [
      {
        id: '1',
        query: 'Macallan 12',
        status: 'done',
        progressSteps: [],
        result: {
          name: 'Macallan 12 Year Single Malt',
          type: 'Scotch Whisky',
          category: 'whisky',
          origin: 'Scotland',
        },
      },
    ];
    renderWithProviders(<AddLiquorPage />);
    // The query text should be visible
    expect(screen.getByText(/Macallan 12/)).toBeInTheDocument();
  });
});

describe('AddLiquorPage - Korean locale', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('locale', 'ko');
    mockItems = [];
  });

  it('should render Korean UI labels', () => {
    renderWithProviders(<AddLiquorPage />);
    expect(screen.getByText('주류 추가')).toBeInTheDocument();
  });
});
