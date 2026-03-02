import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LanguageProvider, useLanguage } from '../LanguageContext';

function TestConsumer() {
  const { locale, setLocale, t } = useLanguage();
  return (
    <div>
      <span data-testid="locale">{locale}</span>
      <span data-testid="translated">{t('nav.collection')}</span>
      <span data-testid="missing-key">{t('nonexistent.key')}</span>
      <button data-testid="switch-ko" onClick={() => setLocale('ko')}>
        Switch to Korean
      </button>
      <button data-testid="switch-en" onClick={() => setLocale('en')}>
        Switch to English
      </button>
    </div>
  );
}

describe('LanguageContext', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('should render children inside LanguageProvider', () => {
    render(
      <LanguageProvider>
        <div data-testid="child">Hello</div>
      </LanguageProvider>
    );
    expect(screen.getByTestId('child')).toBeInTheDocument();
    expect(screen.getByTestId('child')).toHaveTextContent('Hello');
  });

  it('should default to locale "en"', () => {
    render(
      <LanguageProvider>
        <TestConsumer />
      </LanguageProvider>
    );
    expect(screen.getByTestId('locale')).toHaveTextContent('en');
  });

  it('should return English strings by default with t()', () => {
    render(
      <LanguageProvider>
        <TestConsumer />
      </LanguageProvider>
    );
    expect(screen.getByTestId('translated')).toHaveTextContent('Collection');
  });

  it('should return Korean strings when locale is "ko"', async () => {
    const user = userEvent.setup();
    render(
      <LanguageProvider>
        <TestConsumer />
      </LanguageProvider>
    );

    await user.click(screen.getByTestId('switch-ko'));
    expect(screen.getByTestId('locale')).toHaveTextContent('ko');
    expect(screen.getByTestId('translated')).toHaveTextContent('컬렉션');
  });

  it('should change locale when setLocale is called', async () => {
    const user = userEvent.setup();
    render(
      <LanguageProvider>
        <TestConsumer />
      </LanguageProvider>
    );

    expect(screen.getByTestId('locale')).toHaveTextContent('en');
    await user.click(screen.getByTestId('switch-ko'));
    expect(screen.getByTestId('locale')).toHaveTextContent('ko');
    await user.click(screen.getByTestId('switch-en'));
    expect(screen.getByTestId('locale')).toHaveTextContent('en');
  });

  it('should return the key itself for missing translation keys', () => {
    render(
      <LanguageProvider>
        <TestConsumer />
      </LanguageProvider>
    );
    expect(screen.getByTestId('missing-key')).toHaveTextContent('nonexistent.key');
  });

  it('should persist locale to localStorage', async () => {
    const user = userEvent.setup();
    render(
      <LanguageProvider>
        <TestConsumer />
      </LanguageProvider>
    );

    await user.click(screen.getByTestId('switch-ko'));
    expect(localStorage.getItem('locale')).toBe('ko');
  });

  it('should read locale from localStorage on mount', () => {
    localStorage.setItem('locale', 'ko');
    render(
      <LanguageProvider>
        <TestConsumer />
      </LanguageProvider>
    );
    expect(screen.getByTestId('locale')).toHaveTextContent('ko');
    expect(screen.getByTestId('translated')).toHaveTextContent('컬렉션');
  });
});
