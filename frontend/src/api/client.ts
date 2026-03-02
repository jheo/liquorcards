import axios from 'axios';
import type { Liquor, AiSearchResult } from '../types/liquor';

const api = axios.create({ baseURL: '/api' });

export const getLiquors = (params?: Record<string, string>) =>
  api.get<Liquor[]>('/liquors', { params });

export const getLiquor = (id: number) =>
  api.get<Liquor>(`/liquors/${id}`);

export const createLiquor = (data: Partial<Liquor>) =>
  api.post<Liquor>('/liquors', data);

export const updateLiquor = (id: number, data: Partial<Liquor>) =>
  api.put<Liquor>(`/liquors/${id}`, data);

export const deleteLiquor = (id: number) =>
  api.delete(`/liquors/${id}`);

export const updateStatus = (id: number, status: string) =>
  api.patch(`/liquors/${id}/status`, { status });

export const aiSearch = (name: string, provider?: string) =>
  api.post<AiSearchResult>('/liquors/ai-lookup', { name, provider });

export interface SseProgressEvent {
  step: string;
  message: string;
  messageKo: string;
}

export interface SseCallbacks {
  onProgress: (event: SseProgressEvent) => void;
  onDone: (result: AiSearchResult) => void;
  onNotFound: (suggestions: unknown) => void;
  onError: (error: string) => void;
}

export function aiSearchStream(name: string, provider: string, callbacks: SseCallbacks): () => void {
  const controller = new AbortController();

  fetch('/api/liquors/ai-lookup-stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, provider }),
    signal: controller.signal,
  })
    .then(async (response) => {
      if (!response.ok) {
        callbacks.onError(`HTTP ${response.status}`);
        return;
      }

      const reader = response.body?.getReader();
      if (!reader) {
        callbacks.onError('No response body');
        return;
      }

      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        let currentEvent = '';
        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim();
          } else if (line.startsWith('data:')) {
            const data = line.slice(5).trim();
            if (!data) continue;
            try {
              const parsed = JSON.parse(data);
              switch (currentEvent) {
                case 'progress':
                  callbacks.onProgress(parsed);
                  break;
                case 'done':
                  callbacks.onDone(parsed);
                  break;
                case 'not_found':
                  callbacks.onNotFound(parsed);
                  break;
                case 'error':
                  callbacks.onError(parsed.error || 'Unknown error');
                  break;
              }
            } catch {
              // ignore parse errors for partial data
            }
            currentEvent = '';
          } else if (line.trim() === '') {
            currentEvent = '';
          }
        }
      }
    })
    .catch((err) => {
      if (err.name !== 'AbortError') {
        callbacks.onError(err.message || 'Stream failed');
      }
    });

  return () => controller.abort();
}

export const uploadImage = (file: File) => {
  const fd = new FormData();
  fd.append('file', file);
  return api.post<{ url: string }>('/images/upload', fd);
};
