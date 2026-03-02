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

export const aiSearch = (name: string) =>
  api.post<AiSearchResult>('/liquors/ai-lookup', { name });

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

export function aiSearchStream(name: string, callbacks: SseCallbacks): () => void {
  const controller = new AbortController();

  fetch('/api/liquors/ai-lookup-stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
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
      let currentEvent = '';
      let dataBuffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';
        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim();
            dataBuffer = '';
          } else if (line.startsWith('data:')) {
            dataBuffer += line.slice(5).trim();
            if (!dataBuffer) continue;
            try {
              const parsed = JSON.parse(dataBuffer);
              switch (currentEvent) {
                case 'progress':
                  callbacks.onProgress(parsed);
                  break;
                case 'done': {
                  const { resultId } = parsed;
                  const res = await fetch(`/api/liquors/search-result/${resultId}`);
                  if (!res.ok) {
                    callbacks.onError(`Failed to fetch result: HTTP ${res.status}`);
                    break;
                  }
                  const fullResult = await res.json();
                  callbacks.onDone(fullResult);
                  break;
                }
                case 'not_found':
                  callbacks.onNotFound(parsed);
                  break;
                case 'error':
                  callbacks.onError(parsed.error || 'Unknown error');
                  break;
              }
              currentEvent = '';
              dataBuffer = '';
            } catch {
              // JSON incomplete — keep accumulating in dataBuffer
            }
          } else if (line.trim() === '') {
            currentEvent = '';
            dataBuffer = '';
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
