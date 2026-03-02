import { describe, it, expect, beforeEach, vi } from 'vitest';
import { screen, fireEvent } from '@testing-library/react';
import { LiquorCard } from '../LiquorCard';
import { renderWithProviders } from '../../../test/test-utils';
import type { Liquor } from '../../../types/liquor';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const baseLiquor: Liquor = {
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
};

const liquorWithoutKorean: Liquor = {
  id: 2,
  name: 'Hendricks Gin',
  type: 'Gin',
  category: 'gin',
  origin: 'Scotland',
  status: 'active',
  createdAt: '2024-01-01T00:00:00Z',
};

const liquorWithImage: Liquor = {
  ...baseLiquor,
  imageUrl: 'https://example.com/macallan.jpg',
};

describe('LiquorCard', () => {
  beforeEach(() => {
    localStorage.clear();
    mockNavigate.mockClear();
  });

  it('should render English name and type by default', () => {
    renderWithProviders(<LiquorCard liquor={baseLiquor} />);
    expect(screen.getByText('Macallan 18')).toBeInTheDocument();
    expect(screen.getByText('Single Malt Scotch Whisky')).toBeInTheDocument();
  });

  it('should render Korean name and type when locale is "ko"', () => {
    localStorage.setItem('locale', 'ko');
    renderWithProviders(<LiquorCard liquor={baseLiquor} />);
    expect(screen.getByText('맥캘란 18')).toBeInTheDocument();
    expect(screen.getByText('싱글 몰트 스카치 위스키')).toBeInTheDocument();
  });

  it('should fall back to English when Korean fields are missing', () => {
    localStorage.setItem('locale', 'ko');
    renderWithProviders(<LiquorCard liquor={liquorWithoutKorean} />);
    expect(screen.getByText('Hendricks Gin')).toBeInTheDocument();
    expect(screen.getByText('Gin')).toBeInTheDocument();
  });

  it('should display the score when present', () => {
    renderWithProviders(<LiquorCard liquor={baseLiquor} />);
    expect(screen.getByText('92')).toBeInTheDocument();
  });

  it('should display the origin when present', () => {
    renderWithProviders(<LiquorCard liquor={baseLiquor} />);
    expect(screen.getByText('Scotland')).toBeInTheDocument();
  });

  it('should not display score when not present', () => {
    renderWithProviders(<LiquorCard liquor={liquorWithoutKorean} />);
    expect(screen.queryByText('92')).not.toBeInTheDocument();
  });

  it('should navigate to detail page when clicked', () => {
    renderWithProviders(<LiquorCard liquor={baseLiquor} />);
    fireEvent.click(screen.getByText('Macallan 18'));
    expect(mockNavigate).toHaveBeenCalledWith('/liquor/1');
  });

  it('should render image when imageUrl is present', () => {
    renderWithProviders(<LiquorCard liquor={liquorWithImage} />);
    const img = screen.getByAltText('Macallan 18');
    expect(img).toHaveAttribute('src', 'https://example.com/macallan.jpg');
  });

  it('should show placeholder when no imageUrl', () => {
    renderWithProviders(<LiquorCard liquor={baseLiquor} />);
    const placeholder = document.querySelector('.liquor-card-placeholder');
    expect(placeholder).toBeInTheDocument();
  });
});
