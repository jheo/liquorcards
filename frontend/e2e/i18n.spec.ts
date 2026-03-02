import { test, expect } from '@playwright/test';

test.describe('i18n Language Switching', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());
    await page.reload();
  });

  test('should display English by default', async ({ page }) => {
    await page.goto('/');
    // Check sidebar nav items (use role-based selectors to avoid ambiguity)
    await expect(page.getByRole('link', { name: 'Collection' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Add Liquor' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Card Export' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/01-default-english.png' });
  });

  test('should switch to Korean when clicking 한국어', async ({ page }) => {
    await page.goto('/');
    await page.click('button:has-text("한국어")');
    // Check sidebar nav items in Korean
    await expect(page.getByRole('link', { name: '컬렉션' })).toBeVisible();
    await expect(page.getByRole('link', { name: '주류 추가' })).toBeVisible();
    await expect(page.getByRole('link', { name: '카드 내보내기' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/02-korean-sidebar.png' });
  });

  test('should persist language after refresh', async ({ page }) => {
    await page.goto('/');
    await page.click('button:has-text("한국어")');
    await page.reload();
    // Korean should still be active after reload
    await expect(page.getByRole('link', { name: '컬렉션' })).toBeVisible();
    await expect(page.getByRole('heading', { name: '컬렉션' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/03-korean-persisted.png' });
  });

  test('should switch back to English', async ({ page }) => {
    await page.goto('/');
    await page.click('button:has-text("한국어")');
    await expect(page.getByRole('link', { name: '컬렉션' })).toBeVisible();
    await page.click('button:has-text("EN")');
    await expect(page.getByRole('link', { name: 'Collection' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Collection' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/04-back-to-english.png' });
  });
});

test.describe('Collection Page i18n', () => {
  test('should display Korean collection page', async ({ page }) => {
    await page.goto('/');
    await page.click('button:has-text("한국어")');
    // Check collection title (h1)
    await expect(page.getByRole('heading', { name: '컬렉션' })).toBeVisible();
    // Check sort label
    await expect(page.locator('.collection-sort-label')).toHaveText('정렬');
    // Check category chips - use button role to avoid sidebar stat label ambiguity
    await expect(page.getByRole('button', { name: '전체' })).toBeVisible();
    await expect(page.getByRole('button', { name: '위스키' })).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/05-korean-collection.png' });
  });

  test('should display Korean liquor names in cards', async ({ page }) => {
    await page.goto('/');
    await page.click('button:has-text("한국어")');
    await expect(page.locator('text=맥캘란 12년 셰리 오크')).toBeVisible();
    await expect(page.locator('text=헨드릭스 진')).toBeVisible();
    await expect(page.locator('text=오퍼스 원 2019')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/06-korean-cards.png' });
  });
});

test.describe('Liquor Detail Page i18n', () => {
  test('should show Korean detail content', async ({ page }) => {
    await page.goto('/');
    await page.click('button:has-text("한국어")');
    await page.click('text=맥캘란 12년 셰리 오크');
    await expect(page.locator('text=싱글 몰트 스카치 위스키')).toBeVisible();
    await expect(page.locator('h3:has-text("소개")')).toBeVisible();
    await expect(page.locator('h3:has-text("역사")')).toBeVisible();
    await expect(page.locator('text=컬렉션으로 돌아가기')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/07-korean-detail.png' });
  });

  test('should show English detail content by default', async ({ page }) => {
    await page.goto('/');
    await page.click('text=Macallan 12 Year Old Sherry Oak');
    await expect(page.locator('text=Single Malt Scotch Whisky')).toBeVisible();
    await expect(page.locator('h3:has-text("About")')).toBeVisible();
    await expect(page.locator('h3:has-text("Heritage")')).toBeVisible();
    await expect(page.locator('text=Back to Collection')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/08-english-detail.png' });
  });
});

test.describe('Add Liquor Page i18n', () => {
  test('should display Korean add page', async ({ page }) => {
    await page.goto('/');
    await page.click('button:has-text("한국어")');
    await page.getByRole('link', { name: '주류 추가' }).click();
    await expect(page.getByRole('heading', { name: '주류 추가' })).toBeVisible();
    await expect(page.locator('text=AI로 검색')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/09-korean-add.png' });
  });
});

test.describe('Card Export Page i18n', () => {
  test('should display Korean export page', async ({ page }) => {
    await page.goto('/');
    await page.click('button:has-text("한국어")');
    await page.getByRole('link', { name: '카드 내보내기' }).click();
    await expect(page.getByRole('heading', { name: '카드 내보내기' })).toBeVisible();
    await expect(page.locator('text=전체 선택')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/10-korean-export.png' });
  });

  test('should show Korean names in export list', async ({ page }) => {
    await page.goto('/');
    await page.click('button:has-text("한국어")');
    await page.getByRole('link', { name: '카드 내보내기' }).click();
    await expect(page.locator('text=맥캘란 12년 셰리 오크')).toBeVisible();
    await page.screenshot({ path: 'e2e/screenshots/11-korean-export-list.png' });
  });
});
