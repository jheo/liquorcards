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

export const uploadImage = (file: File) => {
  const fd = new FormData();
  fd.append('file', file);
  return api.post<{ url: string }>('/images/upload', fd);
};
