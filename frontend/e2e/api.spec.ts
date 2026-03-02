import { test, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8080/api';

test.describe('API i18n Tests', () => {
  test('GET /api/liquors returns Korean fields', async ({ request }) => {
    const response = await request.get(`${API_BASE}/liquors`);
    expect(response.ok()).toBeTruthy();
    const liquors = await response.json();
    expect(liquors.length).toBeGreaterThan(0);

    const macallan = liquors.find((l: any) => l.name.includes('Macallan'));
    expect(macallan).toBeDefined();
    expect(macallan.nameKo).toBe('맥캘란 12년 셰리 오크');
    expect(macallan.typeKo).toBe('싱글 몰트 스카치 위스키');
    expect(macallan.aboutKo).toBeTruthy();
    expect(macallan.heritageKo).toBeTruthy();
    expect(macallan.tastingNotesKo).toBeInstanceOf(Array);
    expect(macallan.tastingNotesKo.length).toBeGreaterThan(0);
  });

  test('GET /api/liquors/:id returns Korean fields', async ({ request }) => {
    const response = await request.get(`${API_BASE}/liquors/1`);
    expect(response.ok()).toBeTruthy();
    const liquor = await response.json();
    expect(liquor.nameKo).toBeTruthy();
    expect(liquor.typeKo).toBeTruthy();
    expect(liquor.aboutKo).toBeTruthy();
    expect(liquor.heritageKo).toBeTruthy();
    expect(liquor.tastingNotesKo).toBeInstanceOf(Array);
  });

  test('POST /api/liquors with Korean fields', async ({ request }) => {
    const newLiquor = {
      name: 'Test Whisky',
      type: 'Single Malt',
      category: 'whisky',
      nameKo: '테스트 위스키',
      typeKo: '싱글 몰트',
      aboutKo: '테스트용 위스키입니다.',
      heritageKo: '테스트 역사입니다.',
      tastingNotesKo: ['바닐라', '카라멜', '오크'],
      about: 'A test whisky.',
      heritage: 'Test heritage.',
      tastingNotes: ['vanilla', 'caramel', 'oak'],
      status: 'active',
    };

    const response = await request.post(`${API_BASE}/liquors`, { data: newLiquor });
    expect(response.ok()).toBeTruthy();
    const created = await response.json();
    expect(created.nameKo).toBe('테스트 위스키');
    expect(created.typeKo).toBe('싱글 몰트');
    expect(created.aboutKo).toBe('테스트용 위스키입니다.');
    expect(created.tastingNotesKo).toEqual(['바닐라', '카라멜', '오크']);

    // Clean up
    await request.delete(`${API_BASE}/liquors/${created.id}`);
  });

  test('PUT /api/liquors/:id updates Korean fields', async ({ request }) => {
    // Create first
    const createRes = await request.post(`${API_BASE}/liquors`, {
      data: { name: 'Update Test', category: 'whisky', status: 'active' }
    });
    const created = await createRes.json();

    // Update with Korean fields
    const updateRes = await request.put(`${API_BASE}/liquors/${created.id}`, {
      data: {
        nameKo: '업데이트 테스트',
        typeKo: '위스키',
        aboutKo: '업데이트된 소개입니다.',
      }
    });
    expect(updateRes.ok()).toBeTruthy();
    const updated = await updateRes.json();
    expect(updated.nameKo).toBe('업데이트 테스트');
    expect(updated.typeKo).toBe('위스키');
    expect(updated.aboutKo).toBe('업데이트된 소개입니다.');

    // Clean up
    await request.delete(`${API_BASE}/liquors/${created.id}`);
  });

  test('all sample liquors have Korean data', async ({ request }) => {
    const response = await request.get(`${API_BASE}/liquors`);
    const liquors = await response.json();

    for (const liquor of liquors) {
      if (liquor.status === 'active') {
        expect(liquor.nameKo).toBeTruthy();
        expect(liquor.typeKo).toBeTruthy();
      }
    }
  });
});
