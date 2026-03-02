import { describe, it, expect, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import { PrintCard } from '../PrintCard';
import { renderWithProviders } from '../../../test/test-utils';
import type { Liquor } from '../../../types/liquor';

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
  tastingNotes: ['honey', 'vanilla', 'oak'],
  tastingNotesKo: ['꿀', '바닐라', '오크'],
  profile: { sweetness: 80, smokiness: 40 },
};

const liquorWithoutKorean: Liquor = {
  id: 2,
  name: 'Hendricks Gin',
  type: 'Gin',
  category: 'gin',
  origin: 'Scotland',
  status: 'active',
  createdAt: '2024-01-01T00:00:00Z',
  tastingNotes: ['cucumber', 'rose'],
};

describe('PrintCard', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('should render English name and type by default', () => {
    renderWithProviders(<PrintCard liquor={baseLiquor} />);
    expect(screen.getByText('Macallan 18')).toBeInTheDocument();
    expect(screen.getByText('Single Malt Scotch Whisky')).toBeInTheDocument();
  });

  it('should render Korean name and type when locale is "ko"', () => {
    localStorage.setItem('locale', 'ko');
    renderWithProviders(<PrintCard liquor={baseLiquor} />);
    expect(screen.getByText('맥캘란 18')).toBeInTheDocument();
    expect(screen.getByText('싱글 몰트 스카치 위스키')).toBeInTheDocument();
  });

  it('should render English tasting notes by default', () => {
    renderWithProviders(<PrintCard liquor={baseLiquor} />);
    expect(screen.getByText('honey')).toBeInTheDocument();
    expect(screen.getByText('vanilla')).toBeInTheDocument();
    expect(screen.getByText('oak')).toBeInTheDocument();
  });

  it('should render Korean tasting notes when locale is "ko"', () => {
    localStorage.setItem('locale', 'ko');
    renderWithProviders(<PrintCard liquor={baseLiquor} />);
    expect(screen.getByText('꿀')).toBeInTheDocument();
    expect(screen.getByText('바닐라')).toBeInTheDocument();
    expect(screen.getByText('오크')).toBeInTheDocument();
  });

  it('should fall back to English when Korean fields are missing', () => {
    localStorage.setItem('locale', 'ko');
    renderWithProviders(<PrintCard liquor={liquorWithoutKorean} />);
    expect(screen.getByText('Hendricks Gin')).toBeInTheDocument();
    expect(screen.getByText('Gin')).toBeInTheDocument();
    expect(screen.getByText('cucumber')).toBeInTheDocument();
    expect(screen.getByText('rose')).toBeInTheDocument();
  });

  it('should display the score when present', () => {
    renderWithProviders(<PrintCard liquor={baseLiquor} />);
    expect(screen.getByText('92')).toBeInTheDocument();
  });

  it('should display origin when present', () => {
    renderWithProviders(<PrintCard liquor={baseLiquor} />);
    expect(screen.getByText('Scotland')).toBeInTheDocument();
  });

  it('should render profile bars when profile data exists', () => {
    renderWithProviders(<PrintCard liquor={baseLiquor} />);
    expect(screen.getByText('sweetness')).toBeInTheDocument();
    expect(screen.getByText('smokiness')).toBeInTheDocument();
  });
});
