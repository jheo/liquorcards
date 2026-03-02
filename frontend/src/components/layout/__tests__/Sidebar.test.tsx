import { describe, it, expect, beforeEach, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { Sidebar } from '../Sidebar';
import { renderWithProviders } from '../../../test/test-utils';

vi.mock('../../../hooks/useLiquors', () => ({
  useLiquors: () => ({
    liquors: [
      { id: 1, name: 'Test', category: 'whisky', status: 'active', createdAt: '2024-01-01' },
      { id: 2, name: 'Test2', category: 'gin', status: 'active', createdAt: '2024-01-01' },
    ],
    loading: false,
    error: null,
    refetch: vi.fn(),
  }),
}));

describe('Sidebar', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('should render English nav labels by default', () => {
    renderWithProviders(<Sidebar />);
    expect(screen.getByText('Collection')).toBeInTheDocument();
    expect(screen.getByText('Add Liquor')).toBeInTheDocument();
    expect(screen.getByText('Card Export')).toBeInTheDocument();
  });

  it('should render Korean nav labels when locale is "ko"', () => {
    localStorage.setItem('locale', 'ko');
    renderWithProviders(<Sidebar />);
    expect(screen.getByText('컬렉션')).toBeInTheDocument();
    expect(screen.getByText('주류 추가')).toBeInTheDocument();
    expect(screen.getByText('카드 내보내기')).toBeInTheDocument();
  });

  it('should render the LanguageToggle component', () => {
    renderWithProviders(<Sidebar />);
    expect(screen.getByText('EN')).toBeInTheDocument();
    expect(screen.getByText('한국어')).toBeInTheDocument();
  });

  it('should render English stat labels by default', () => {
    renderWithProviders(<Sidebar />);
    expect(screen.getByText('Total')).toBeInTheDocument();
    expect(screen.getByText('Categories')).toBeInTheDocument();
  });

  it('should render Korean stat labels when locale is "ko"', () => {
    localStorage.setItem('locale', 'ko');
    renderWithProviders(<Sidebar />);
    expect(screen.getByText('전체')).toBeInTheDocument();
    expect(screen.getByText('카테고리')).toBeInTheDocument();
  });

  it('should display the correct total and categories counts', () => {
    renderWithProviders(<Sidebar />);
    // Both Total and Categories show 2 (2 liquors, 2 unique categories)
    const statValues = screen.getAllByText('2');
    expect(statValues).toHaveLength(2);
  });

  it('should render the LIQUOR CARDS logo', () => {
    renderWithProviders(<Sidebar />);
    expect(screen.getByText('LIQUOR CARDS')).toBeInTheDocument();
  });
});
