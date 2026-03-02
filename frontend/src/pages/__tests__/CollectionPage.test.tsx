import { describe, it, expect, beforeEach, vi } from 'vitest';
import { screen, fireEvent } from '@testing-library/react';
import { CollectionPage } from '../CollectionPage';
import { renderWithProviders } from '../../test/test-utils';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const mockLiquors = [
  {
    id: 1,
    name: 'Macallan 18',
    type: 'Single Malt Scotch Whisky',
    category: 'whisky',
    origin: 'Scotland',
    score: 92,
    status: 'active',
    createdAt: '2024-01-01T00:00:00Z',
    nameKo: '맥캘란 18',
    typeKo: '싱글 몰트 스카치 위스키',
  },
  {
    id: 2,
    name: 'Hendricks Gin',
    type: 'Premium Gin',
    category: 'gin',
    origin: 'Scotland',
    score: 85,
    status: 'active',
    createdAt: '2024-06-01T00:00:00Z',
    nameKo: '헨드릭스 진',
    typeKo: '프리미엄 진',
  },
  {
    id: 3,
    name: 'Archived Whisky',
    type: 'Blended',
    category: 'whisky',
    origin: 'Japan',
    score: 70,
    status: 'archived',
    createdAt: '2024-03-01T00:00:00Z',
  },
];

let mockLoading = false;

vi.mock('../../hooks/useLiquors', () => ({
  useLiquors: () => ({
    liquors: mockLiquors,
    loading: mockLoading,
    error: null,
    refetch: vi.fn(),
  }),
}));

describe('CollectionPage', () => {
  beforeEach(() => {
    localStorage.clear();
    mockLoading = false;
  });

  it('should render English title by default', () => {
    renderWithProviders(<CollectionPage />);
    expect(screen.getByText('Collection')).toBeInTheDocument();
  });

  it('should render Korean title when locale is "ko"', () => {
    localStorage.setItem('locale', 'ko');
    renderWithProviders(<CollectionPage />);
    expect(screen.getByText('컬렉션')).toBeInTheDocument();
  });

  it('should render English category chips by default', () => {
    renderWithProviders(<CollectionPage />);
    expect(screen.getByText('All')).toBeInTheDocument();
    expect(screen.getByText('Whisky')).toBeInTheDocument();
    expect(screen.getByText('Wine')).toBeInTheDocument();
    expect(screen.getByText('Gin')).toBeInTheDocument();
  });

  it('should render Korean category chips when locale is "ko"', () => {
    localStorage.setItem('locale', 'ko');
    renderWithProviders(<CollectionPage />);
    expect(screen.getByText('전체')).toBeInTheDocument();
    expect(screen.getByText('위스키')).toBeInTheDocument();
    expect(screen.getByText('와인')).toBeInTheDocument();
    expect(screen.getByText('진')).toBeInTheDocument();
  });

  it('should render English sort options by default', () => {
    renderWithProviders(<CollectionPage />);
    expect(screen.getByText('Sort by')).toBeInTheDocument();
  });

  it('should render Korean sort label when locale is "ko"', () => {
    localStorage.setItem('locale', 'ko');
    renderWithProviders(<CollectionPage />);
    expect(screen.getByText('정렬')).toBeInTheDocument();
  });

  it('should render English search placeholder by default', () => {
    renderWithProviders(<CollectionPage />);
    expect(
      screen.getByPlaceholderText('Search by name, type, origin, tasting notes, profile...')
    ).toBeInTheDocument();
  });

  it('should render Korean search placeholder when locale is "ko"', () => {
    localStorage.setItem('locale', 'ko');
    renderWithProviders(<CollectionPage />);
    expect(
      screen.getByPlaceholderText('이름, 종류, 원산지, 테이스팅 노트, 프로필로 검색...')
    ).toBeInTheDocument();
  });

  it('should render liquor cards with English names by default', () => {
    renderWithProviders(<CollectionPage />);
    expect(screen.getByText('Macallan 18')).toBeInTheDocument();
  });

  it('should render liquor cards with Korean names when locale is "ko"', () => {
    localStorage.setItem('locale', 'ko');
    renderWithProviders(<CollectionPage />);
    expect(screen.getByText('맥캘란 18')).toBeInTheDocument();
  });

  it('should filter out archived liquors', () => {
    renderWithProviders(<CollectionPage />);
    expect(screen.queryByText('Archived Whisky')).not.toBeInTheDocument();
    // Should show 2 active items
    expect(screen.getByText('2')).toBeInTheDocument(); // count
  });

  it('should filter by search query', () => {
    renderWithProviders(<CollectionPage />);
    const searchInput = screen.getByPlaceholderText('Search by name, type, origin, tasting notes, profile...');
    fireEvent.change(searchInput, { target: { value: 'Hendricks' } });
    expect(screen.getByText('Hendricks Gin')).toBeInTheDocument();
    expect(screen.queryByText('Macallan 18')).not.toBeInTheDocument();
  });

  it('should filter by Korean name when searching in Korean locale', () => {
    localStorage.setItem('locale', 'ko');
    renderWithProviders(<CollectionPage />);
    const searchInput = screen.getByPlaceholderText('이름, 종류, 원산지, 테이스팅 노트, 프로필로 검색...');
    fireEvent.change(searchInput, { target: { value: '헨드릭스' } });
    expect(screen.getByText('헨드릭스 진')).toBeInTheDocument();
    expect(screen.queryByText('맥캘란 18')).not.toBeInTheDocument();
  });

  it('should filter by category when clicking a chip', () => {
    renderWithProviders(<CollectionPage />);
    fireEvent.click(screen.getByText('Gin'));
    expect(screen.getByText('Hendricks Gin')).toBeInTheDocument();
    expect(screen.queryByText('Macallan 18')).not.toBeInTheDocument();
  });

  it('should sort by name when sort option changes', () => {
    renderWithProviders(<CollectionPage />);
    const select = screen.getByRole('combobox');
    fireEvent.change(select, { target: { value: 'name' } });
    const cards = screen.getAllByText(/Macallan|Hendricks/);
    // Hendricks comes before Macallan alphabetically
    expect(cards[0].textContent).toBe('Hendricks Gin');
  });

  it('should sort by score when sort option changes', () => {
    renderWithProviders(<CollectionPage />);
    const select = screen.getByRole('combobox');
    fireEvent.change(select, { target: { value: 'score' } });
    const cards = screen.getAllByText(/Macallan|Hendricks/);
    // Macallan (92) comes before Hendricks (85) by score desc
    expect(cards[0].textContent).toBe('Macallan 18');
  });

  it('should show empty state when no results match filter', () => {
    renderWithProviders(<CollectionPage />);
    const searchInput = screen.getByPlaceholderText('Search by name, type, origin, tasting notes, profile...');
    fireEvent.change(searchInput, { target: { value: 'xyznonexistent' } });
    expect(screen.getByText('No liquors found')).toBeInTheDocument();
    expect(screen.getByText('Try adjusting your search or filters')).toBeInTheDocument();
  });

  it('should display count of filtered results', () => {
    renderWithProviders(<CollectionPage />);
    // 2 active liquors shown
    expect(screen.getByText('2')).toBeInTheDocument();
  });
});
