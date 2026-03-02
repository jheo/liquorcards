import { describe, it, expect, beforeEach, vi } from 'vitest';
import { screen, fireEvent, waitFor } from '@testing-library/react';
import { LiquorDetailPage } from '../LiquorDetailPage';
import { renderWithProviders } from '../../test/test-utils';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ id: '1' }),
  };
});

import { deleteLiquor, updateStatus } from '../../api/client';
vi.mock('../../api/client', () => ({
  deleteLiquor: vi.fn(() => Promise.resolve()),
  updateStatus: vi.fn(() => Promise.resolve()),
  updateLiquor: vi.fn(() => Promise.resolve({ data: {} })),
  getLiquor: vi.fn(),
}));

const fullLiquor = {
  id: 1,
  name: 'Macallan 18',
  type: 'Single Malt Scotch Whisky',
  category: 'whisky',
  abv: 43,
  age: '18 Years',
  origin: 'Scotland',
  score: 92,
  price: '$350',
  volume: '700ml',
  about: 'A prestigious single malt whisky.',
  heritage: 'Rich distilling heritage since 1824.',
  profile: { sweetness: 80, smokiness: 40 },
  tastingNotes: ['honey', 'vanilla', 'oak'],
  nameKo: '맥캘란 18',
  typeKo: '싱글 몰트 스카치 위스키',
  aboutKo: '유명한 싱글 몰트 위스키입니다.',
  heritageKo: '1824년부터 이어온 풍부한 증류 역사.',
  tastingNotesKo: ['꿀', '바닐라', '오크'],
  status: 'active',
  createdAt: '2024-01-01T00:00:00Z',
};

const liquorWithImage = {
  ...fullLiquor,
  imageUrl: 'https://example.com/macallan.jpg',
};

const liquorWithoutKorean = {
  id: 1,
  name: 'Hendricks Gin',
  type: 'Gin',
  category: 'gin',
  origin: 'Scotland',
  about: 'A Scottish gin infused with cucumber and rose.',
  heritage: 'Made in the Girvan distillery.',
  tastingNotes: ['cucumber', 'rose'],
  status: 'active',
  createdAt: '2024-01-01T00:00:00Z',
};

vi.mock('../../hooks/useLiquor', () => ({
  useLiquor: vi.fn(),
}));

import { useLiquor } from '../../hooks/useLiquor';
const mockUseLiquor = vi.mocked(useLiquor);

describe('LiquorDetailPage - English (full data)', () => {
  beforeEach(() => {
    localStorage.clear();
    mockNavigate.mockClear();
    mockUseLiquor.mockReturnValue({
      liquor: fullLiquor as any,
      loading: false,
      error: null,
      refetch: vi.fn(),
    });
  });

  it('should render English name and type', () => {
    renderWithProviders(<LiquorDetailPage />);
    expect(screen.getByText('Macallan 18')).toBeInTheDocument();
    expect(screen.getByText('Single Malt Scotch Whisky')).toBeInTheDocument();
  });

  it('should render English about and heritage text', () => {
    renderWithProviders(<LiquorDetailPage />);
    expect(screen.getByText('A prestigious single malt whisky.')).toBeInTheDocument();
    expect(screen.getByText('Rich distilling heritage since 1824.')).toBeInTheDocument();
  });

  it('should render English tasting notes', () => {
    renderWithProviders(<LiquorDetailPage />);
    expect(screen.getByText('honey')).toBeInTheDocument();
    expect(screen.getByText('vanilla')).toBeInTheDocument();
    expect(screen.getByText('oak')).toBeInTheDocument();
  });

  it('should render English UI labels', () => {
    renderWithProviders(<LiquorDetailPage />);
    expect(screen.getByText('Back to Collection')).toBeInTheDocument();
    expect(screen.getByText('About')).toBeInTheDocument();
    expect(screen.getByText('Heritage')).toBeInTheDocument();
    expect(screen.getByText('Tasting Notes')).toBeInTheDocument();
    expect(screen.getByText('Archive')).toBeInTheDocument();
    expect(screen.getByText('Delete')).toBeInTheDocument();
  });

  it('should render the score', () => {
    renderWithProviders(<LiquorDetailPage />);
    expect(screen.getByText('92')).toBeInTheDocument();
    expect(screen.getByText('Score')).toBeInTheDocument();
  });

  it('should render profile bars', () => {
    renderWithProviders(<LiquorDetailPage />);
    expect(screen.getByText('sweetness')).toBeInTheDocument();
    expect(screen.getByText('80')).toBeInTheDocument();
    expect(screen.getByText('smokiness')).toBeInTheDocument();
    expect(screen.getByText('40')).toBeInTheDocument();
  });

  it('should render stat badges for abv, age, volume, price, origin', () => {
    renderWithProviders(<LiquorDetailPage />);
    expect(screen.getByText('43%')).toBeInTheDocument();
    expect(screen.getByText('18 Years')).toBeInTheDocument();
    expect(screen.getByText('700ml')).toBeInTheDocument();
    expect(screen.getByText('$350')).toBeInTheDocument();
    expect(screen.getByText('Scotland')).toBeInTheDocument();
  });

  it('should navigate back when back button is clicked', () => {
    renderWithProviders(<LiquorDetailPage />);
    fireEvent.click(screen.getByText('Back to Collection'));
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('should call updateStatus and navigate when Archive is clicked', async () => {
    renderWithProviders(<LiquorDetailPage />);
    fireEvent.click(screen.getByText('Archive'));
    await waitFor(() => {
      expect(updateStatus).toHaveBeenCalledWith(1, 'archived');
      expect(mockNavigate).toHaveBeenCalledWith('/');
    });
  });

  it('should show confirm delete dialog when Delete is clicked', () => {
    renderWithProviders(<LiquorDetailPage />);
    fireEvent.click(screen.getByText('Delete'));
    expect(screen.getByText('Are you sure?')).toBeInTheDocument();
    expect(screen.getByText('Yes, Delete')).toBeInTheDocument();
    expect(screen.getByText('Cancel')).toBeInTheDocument();
  });

  it('should call deleteLiquor and navigate when confirm delete', async () => {
    renderWithProviders(<LiquorDetailPage />);
    fireEvent.click(screen.getByText('Delete'));
    fireEvent.click(screen.getByText('Yes, Delete'));
    await waitFor(() => {
      expect(deleteLiquor).toHaveBeenCalledWith(1);
      expect(mockNavigate).toHaveBeenCalledWith('/');
    });
  });

  it('should hide confirm dialog when Cancel is clicked', () => {
    renderWithProviders(<LiquorDetailPage />);
    fireEvent.click(screen.getByText('Delete'));
    expect(screen.getByText('Are you sure?')).toBeInTheDocument();
    fireEvent.click(screen.getByText('Cancel'));
    expect(screen.queryByText('Are you sure?')).not.toBeInTheDocument();
    expect(screen.getByText('Delete')).toBeInTheDocument();
  });

  it('should show placeholder when no image', () => {
    renderWithProviders(<LiquorDetailPage />);
    expect(screen.getByText('Macallan 18')).toBeInTheDocument();
    // Wine icon placeholder should be present (SVG)
    const placeholder = document.querySelector('.detail-hero-placeholder');
    expect(placeholder).toBeInTheDocument();
  });
});

describe('LiquorDetailPage - with image', () => {
  beforeEach(() => {
    localStorage.clear();
    mockUseLiquor.mockReturnValue({
      liquor: liquorWithImage as any,
      loading: false,
      error: null,
      refetch: vi.fn(),
    });
  });

  it('should render image when imageUrl is present', () => {
    renderWithProviders(<LiquorDetailPage />);
    const img = screen.getByAltText('Macallan 18');
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', 'https://example.com/macallan.jpg');
  });
});

describe('LiquorDetailPage - Loading', () => {
  beforeEach(() => {
    localStorage.clear();
    mockUseLiquor.mockReturnValue({
      liquor: null,
      loading: true,
      error: null,
      refetch: vi.fn(),
    });
  });

  it('should show loading spinner', () => {
    renderWithProviders(<LiquorDetailPage />);
    const spinner = document.querySelector('.spin');
    expect(spinner).toBeInTheDocument();
  });
});

describe('LiquorDetailPage - Korean (full data)', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('locale', 'ko');
    mockUseLiquor.mockReturnValue({
      liquor: fullLiquor as any,
      loading: false,
      error: null,
      refetch: vi.fn(),
    });
  });

  it('should render Korean name and type', () => {
    renderWithProviders(<LiquorDetailPage />);
    expect(screen.getByText('맥캘란 18')).toBeInTheDocument();
    expect(screen.getByText('싱글 몰트 스카치 위스키')).toBeInTheDocument();
  });

  it('should render Korean about and heritage text', () => {
    renderWithProviders(<LiquorDetailPage />);
    expect(screen.getByText('유명한 싱글 몰트 위스키입니다.')).toBeInTheDocument();
    expect(screen.getByText('1824년부터 이어온 풍부한 증류 역사.')).toBeInTheDocument();
  });

  it('should render Korean tasting notes', () => {
    renderWithProviders(<LiquorDetailPage />);
    expect(screen.getByText('꿀')).toBeInTheDocument();
    expect(screen.getByText('바닐라')).toBeInTheDocument();
    expect(screen.getByText('오크')).toBeInTheDocument();
  });

  it('should render Korean UI labels', () => {
    renderWithProviders(<LiquorDetailPage />);
    expect(screen.getByText('컬렉션으로 돌아가기')).toBeInTheDocument();
    expect(screen.getByText('소개')).toBeInTheDocument();
    expect(screen.getByText('역사')).toBeInTheDocument();
    expect(screen.getByText('테이스팅 노트')).toBeInTheDocument();
    expect(screen.getByText('보관')).toBeInTheDocument();
    expect(screen.getByText('삭제')).toBeInTheDocument();
  });

  it('should render Korean score label', () => {
    renderWithProviders(<LiquorDetailPage />);
    expect(screen.getByText('점수')).toBeInTheDocument();
  });
});

describe('LiquorDetailPage - Korean fallback (no Korean fields)', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem('locale', 'ko');
    mockUseLiquor.mockReturnValue({
      liquor: liquorWithoutKorean as any,
      loading: false,
      error: null,
      refetch: vi.fn(),
    });
  });

  it('should fall back to English name and type when Korean is missing', () => {
    renderWithProviders(<LiquorDetailPage />);
    expect(screen.getByText('Hendricks Gin')).toBeInTheDocument();
    expect(screen.getByText('Gin')).toBeInTheDocument();
  });

  it('should fall back to English about and heritage when Korean is missing', () => {
    renderWithProviders(<LiquorDetailPage />);
    expect(screen.getByText('A Scottish gin infused with cucumber and rose.')).toBeInTheDocument();
    expect(screen.getByText('Made in the Girvan distillery.')).toBeInTheDocument();
  });

  it('should fall back to English tasting notes when Korean is missing', () => {
    renderWithProviders(<LiquorDetailPage />);
    expect(screen.getByText('cucumber')).toBeInTheDocument();
    expect(screen.getByText('rose')).toBeInTheDocument();
  });
});

describe('LiquorDetailPage - Not found', () => {
  beforeEach(() => {
    localStorage.clear();
    mockUseLiquor.mockReturnValue({
      liquor: null,
      loading: false,
      error: null,
      refetch: vi.fn(),
    });
  });

  it('should show English not found message by default', () => {
    renderWithProviders(<LiquorDetailPage />);
    expect(screen.getByText('Liquor not found')).toBeInTheDocument();
  });

  it('should show Korean not found message when locale is "ko"', () => {
    localStorage.setItem('locale', 'ko');
    renderWithProviders(<LiquorDetailPage />);
    expect(screen.getByText('주류를 찾을 수 없습니다')).toBeInTheDocument();
  });
});
