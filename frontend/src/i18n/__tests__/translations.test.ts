import { describe, it, expect } from 'vitest';
import { translations } from '../translations';

function getKeys(obj: Record<string, unknown>, prefix = ''): string[] {
  const keys: string[] = [];
  for (const key of Object.keys(obj)) {
    const fullKey = prefix ? `${prefix}.${key}` : key;
    const value = obj[key];
    if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
      keys.push(...getKeys(value as Record<string, unknown>, fullKey));
    } else {
      keys.push(fullKey);
    }
  }
  return keys.sort();
}

describe('translations', () => {
  it('should have en and ko at the top level', () => {
    expect(translations).toHaveProperty('en');
    expect(translations).toHaveProperty('ko');
  });

  it('should have the same keys in en and ko at every nesting level', () => {
    const enKeys = getKeys(translations.en as unknown as Record<string, unknown>);
    const koKeys = getKeys(translations.ko as unknown as Record<string, unknown>);
    expect(enKeys).toEqual(koKeys);
  });

  it('should have nav keys in both locales', () => {
    expect(translations.en.nav).toHaveProperty('collection');
    expect(translations.en.nav).toHaveProperty('addLiquor');
    expect(translations.en.nav).toHaveProperty('cardExport');
    expect(translations.ko.nav).toHaveProperty('collection');
    expect(translations.ko.nav).toHaveProperty('addLiquor');
    expect(translations.ko.nav).toHaveProperty('cardExport');
  });

  it('should have correct English nav values', () => {
    expect(translations.en.nav.collection).toBe('Collection');
    expect(translations.en.nav.addLiquor).toBe('Add Liquor');
    expect(translations.en.nav.cardExport).toBe('Card Export');
  });

  it('should have correct Korean nav values', () => {
    expect(translations.ko.nav.collection).toBe('컬렉션');
    expect(translations.ko.nav.addLiquor).toBe('주류 추가');
    expect(translations.ko.nav.cardExport).toBe('카드 내보내기');
  });

  it('should have correct English collection title', () => {
    expect(translations.en.collection.title).toBe('Collection');
  });

  it('should have correct Korean collection title', () => {
    expect(translations.ko.collection.title).toBe('컬렉션');
  });

  it('should have correct English detail strings', () => {
    expect(translations.en.detail.backToCollection).toBe('Back to Collection');
    expect(translations.en.detail.tastingNotes).toBe('Tasting Notes');
    expect(translations.en.detail.delete).toBe('Delete');
  });

  it('should have correct Korean detail strings', () => {
    expect(translations.ko.detail.backToCollection).toBe('컬렉션으로 돌아가기');
    expect(translations.ko.detail.tastingNotes).toBe('테이스팅 노트');
    expect(translations.ko.detail.delete).toBe('삭제');
  });

  it('should have all category keys in the collection section', () => {
    const categoryKeys = ['all', 'whisky', 'wine', 'gin', 'vodka', 'rum', 'tequila', 'brandy'];
    for (const key of categoryKeys) {
      expect(translations.en.collection).toHaveProperty(key);
      expect(translations.ko.collection).toHaveProperty(key);
    }
  });

  it('should not have empty string values in en', () => {
    const enKeys = getKeys(translations.en as unknown as Record<string, unknown>);
    for (const key of enKeys) {
      const parts = key.split('.');
      let value: unknown = translations.en;
      for (const p of parts) {
        value = (value as Record<string, unknown>)[p];
      }
      expect(value).not.toBe('');
    }
  });

  it('should not have empty string values in ko', () => {
    const koKeys = getKeys(translations.ko as unknown as Record<string, unknown>);
    for (const key of koKeys) {
      const parts = key.split('.');
      let value: unknown = translations.ko;
      for (const p of parts) {
        value = (value as Record<string, unknown>)[p];
      }
      expect(value).not.toBe('');
    }
  });
});
