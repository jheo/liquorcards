import { describe, it, expect, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LanguageToggle } from '../LanguageToggle';
import { renderWithProviders } from '../../../test/test-utils';

describe('LanguageToggle', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('should render EN and 한국어 buttons', () => {
    renderWithProviders(<LanguageToggle />);
    expect(screen.getByText('EN')).toBeInTheDocument();
    expect(screen.getByText('한국어')).toBeInTheDocument();
  });

  it('should have EN active by default', () => {
    renderWithProviders(<LanguageToggle />);
    const enButton = screen.getByText('EN');
    const koButton = screen.getByText('한국어');
    expect(enButton.className).toContain('active');
    expect(koButton.className).not.toContain('active');
  });

  it('should switch active state when clicking 한국어', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LanguageToggle />);

    const enButton = screen.getByText('EN');
    const koButton = screen.getByText('한국어');

    await user.click(koButton);

    expect(koButton.className).toContain('active');
    expect(enButton.className).not.toContain('active');
  });

  it('should switch back to EN when clicking EN after switching to Korean', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LanguageToggle />);

    const enButton = screen.getByText('EN');
    const koButton = screen.getByText('한국어');

    await user.click(koButton);
    expect(koButton.className).toContain('active');

    await user.click(enButton);
    expect(enButton.className).toContain('active');
    expect(koButton.className).not.toContain('active');
  });

  it('should persist locale to localStorage when toggling', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LanguageToggle />);

    await user.click(screen.getByText('한국어'));
    expect(localStorage.getItem('locale')).toBe('ko');

    await user.click(screen.getByText('EN'));
    expect(localStorage.getItem('locale')).toBe('en');
  });
});
