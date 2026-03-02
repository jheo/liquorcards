import { createContext, useContext, useState, useEffect, useRef, useCallback, type ReactNode } from 'react';
import { aiSearchStream, aiSearchStreamWithSelection } from '../api/client';
import type { SseProgressEvent } from '../api/client';
import type { AiSearchResult, SuggestionResponse, DisambiguationCandidate } from '../types/liquor';

export type SearchStatus = 'pending' | 'searching' | 'done' | 'not_found' | 'error' | 'cancelled' | 'disambiguation';

export interface ProgressStep {
  step: string;
  message: string;
  messageKo: string;
  timestamp: number;
}

export interface SearchItem {
  id: string;
  query: string;
  status: SearchStatus;
  result?: AiSearchResult;
  suggestions?: SuggestionResponse;
  error?: string;
  progressSteps: ProgressStep[];
  disambiguationCandidates?: DisambiguationCandidate[];
  disambiguationType?: 'vintage' | 'expression' | 'variant';
  prioritized?: boolean;
}

interface SearchQueueContextValue {
  items: SearchItem[];
  addSearch: (query: string) => void;
  removeItem: (id: string) => void;
  clearAll: () => void;
  retryItem: (id: string) => void;
  cancelItem: (id: string) => void;
  selectDisambiguation: (id: string, selectedName: string) => void;
}

const STORAGE_KEY = 'liquor-cards-search-queue';

function loadQueueFromStorage(): SearchItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const items: SearchItem[] = JSON.parse(raw);
    // Convert any in-progress items to error (search was interrupted)
    return items.map((item) =>
      item.status === 'searching'
        ? { ...item, status: 'error' as SearchStatus, error: '검색이 중단되었습니다 / Search was interrupted' }
        : item
    );
  } catch {
    return [];
  }
}

function saveQueueToStorage(items: SearchItem[]) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  } catch {
    // Ignore storage errors
  }
}

const MAX_CONCURRENT = 3;

const SearchQueueContext = createContext<SearchQueueContextValue | null>(null);

export function SearchQueueProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<SearchItem[]>(() => loadQueueFromStorage());
  const activeIdsRef = useRef<Set<string>>(new Set());
  const abortMapRef = useRef<Map<string, () => void>>(new Map());

  // Persist queue to localStorage on changes
  useEffect(() => {
    saveQueueToStorage(items);
  }, [items]);

  const addSearch = useCallback((query: string) => {
    const id = `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
    setItems((prev) => [
      ...prev,
      { id, query, status: 'pending', progressSteps: [] },
    ]);
  }, []);

  const removeItem = useCallback((id: string) => {
    abortMapRef.current.get(id)?.();
    abortMapRef.current.delete(id);
    setItems((prev) => prev.filter((item) => item.id !== id));
  }, []);

  const clearAll = useCallback(() => {
    setItems((prev) => prev.filter((item) => item.status === 'searching'));
  }, []);

  const retryItem = useCallback((id: string) => {
    setItems((prev) =>
      prev.map((item) =>
        item.id === id
          ? { ...item, status: 'pending' as SearchStatus, error: undefined, result: undefined, suggestions: undefined, disambiguationCandidates: undefined, progressSteps: [] }
          : item
      )
    );
  }, []);

  const cancelItem = useCallback((id: string) => {
    abortMapRef.current.get(id)?.();
    abortMapRef.current.delete(id);
    activeIdsRef.current.delete(id);
    setItems((prev) =>
      prev.map((item) =>
        item.id === id && item.status === 'searching'
          ? { ...item, status: 'cancelled' as SearchStatus }
          : item
      )
    );
  }, []);

  const selectDisambiguation = useCallback((id: string, selectedName: string) => {
    setItems((prev) =>
      prev.map((item) =>
        item.id === id
          ? { ...item, query: selectedName, status: 'pending' as SearchStatus, disambiguationCandidates: undefined, disambiguationType: undefined, progressSteps: [], prioritized: true }
          : item
      )
    );
  }, []);

  const startSearch = useCallback((item: SearchItem) => {
    activeIdsRef.current.add(item.id);

    setItems((prev) =>
      prev.map((i) =>
        i.id === item.id ? { ...i, status: 'searching' as SearchStatus, progressSteps: [], prioritized: false } : i
      )
    );

    const streamFn = item.disambiguationCandidates ? aiSearchStreamWithSelection : aiSearchStream;

    const finalize = (id: string) => {
      abortMapRef.current.delete(id);
      activeIdsRef.current.delete(id);
    };

    const abort = streamFn(item.query, {
      onProgress: (event: SseProgressEvent) => {
        setItems((prev) =>
          prev.map((i) =>
            i.id === item.id
              ? {
                  ...i,
                  progressSteps: [
                    ...i.progressSteps,
                    { ...event, timestamp: Date.now() },
                  ],
                }
              : i
          )
        );
      },
      onDone: (result: AiSearchResult) => {
        finalize(item.id);
        setItems((prev) =>
          prev.map((i) =>
            i.id === item.id
              ? { ...i, status: 'done' as SearchStatus, result }
              : i
          )
        );
      },
      onDisambiguation: (candidates: DisambiguationCandidate[], type?: string) => {
        finalize(item.id);
        setItems((prev) =>
          prev.map((i) =>
            i.id === item.id
              ? { ...i, status: 'disambiguation' as SearchStatus, disambiguationCandidates: candidates, disambiguationType: type as SearchItem['disambiguationType'] }
              : i
          )
        );
      },
      onNotFound: (suggestions: unknown) => {
        finalize(item.id);
        setItems((prev) =>
          prev.map((i) =>
            i.id === item.id
              ? { ...i, status: 'not_found' as SearchStatus, suggestions: suggestions as SuggestionResponse }
              : i
          )
        );
      },
      onError: (error: string) => {
        finalize(item.id);
        setItems((prev) =>
          prev.map((i) =>
            i.id === item.id
              ? { ...i, status: 'error' as SearchStatus, error }
              : i
          )
        );
      },
    });

    abortMapRef.current.set(item.id, abort);
  }, []);

  useEffect(() => {
    const fillSlots = () => {
      while (activeIdsRef.current.size < MAX_CONCURRENT) {
        const next = items.find((i) => i.status === 'pending' && i.prioritized && !activeIdsRef.current.has(i.id))
          || items.find((i) => i.status === 'pending' && !activeIdsRef.current.has(i.id));
        if (!next) break;
        startSearch(next);
      }
    };

    fillSlots();
  }, [items, startSearch]);

  return (
    <SearchQueueContext.Provider value={{ items, addSearch, removeItem, clearAll, retryItem, cancelItem, selectDisambiguation }}>
      {children}
    </SearchQueueContext.Provider>
  );
}

export function useSearchQueue() {
  const ctx = useContext(SearchQueueContext);
  if (!ctx) throw new Error('useSearchQueue must be used within SearchQueueProvider');
  return ctx;
}
