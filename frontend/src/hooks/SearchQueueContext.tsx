import { createContext, useContext, useState, useEffect, useRef, useCallback, type ReactNode } from 'react';
import { aiSearchStream } from '../api/client';
import type { SseProgressEvent } from '../api/client';
import type { AiSearchResult, SuggestionResponse } from '../types/liquor';
import { isSuggestionResponse } from '../types/liquor';

export type SearchStatus = 'pending' | 'searching' | 'done' | 'not_found' | 'error';

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
}

interface SearchQueueContextValue {
  items: SearchItem[];
  addSearch: (query: string) => void;
  removeItem: (id: string) => void;
  clearAll: () => void;
  retryItem: (id: string) => void;
}

const SearchQueueContext = createContext<SearchQueueContextValue | null>(null);

export function SearchQueueProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<SearchItem[]>([]);
  const processingRef = useRef(false);
  const abortRef = useRef<(() => void) | null>(null);

  const addSearch = useCallback((query: string) => {
    const id = `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
    setItems((prev) => [
      ...prev,
      { id, query, status: 'pending', progressSteps: [] },
    ]);
  }, []);

  const removeItem = useCallback((id: string) => {
    setItems((prev) => prev.filter((item) => item.id !== id));
  }, []);

  const clearAll = useCallback(() => {
    setItems((prev) => prev.filter((item) => item.status === 'searching'));
  }, []);

  const retryItem = useCallback((id: string) => {
    setItems((prev) =>
      prev.map((item) =>
        item.id === id
          ? { ...item, status: 'pending' as SearchStatus, error: undefined, result: undefined, suggestions: undefined, progressSteps: [] }
          : item
      )
    );
  }, []);

  useEffect(() => {
    const processNext = () => {
      if (processingRef.current) return;

      const pendingItem = items.find((item) => item.status === 'pending');
      if (!pendingItem) return;

      processingRef.current = true;

      setItems((prev) =>
        prev.map((item) =>
          item.id === pendingItem.id ? { ...item, status: 'searching' as SearchStatus, progressSteps: [] } : item
        )
      );

      const abort = aiSearchStream(pendingItem.query, {
        onProgress: (event: SseProgressEvent) => {
          setItems((prev) =>
            prev.map((item) =>
              item.id === pendingItem.id
                ? {
                    ...item,
                    progressSteps: [
                      ...item.progressSteps,
                      { ...event, timestamp: Date.now() },
                    ],
                  }
                : item
            )
          );
        },
        onDone: (result: AiSearchResult) => {
          setItems((prev) =>
            prev.map((item) =>
              item.id === pendingItem.id
                ? { ...item, status: 'done' as SearchStatus, result }
                : item
            )
          );
          processingRef.current = false;
        },
        onNotFound: (suggestions: unknown) => {
          setItems((prev) =>
            prev.map((item) =>
              item.id === pendingItem.id
                ? { ...item, status: 'not_found' as SearchStatus, suggestions: suggestions as SuggestionResponse }
                : item
            )
          );
          processingRef.current = false;
        },
        onError: (error: string) => {
          setItems((prev) =>
            prev.map((item) =>
              item.id === pendingItem.id
                ? { ...item, status: 'error' as SearchStatus, error }
                : item
            )
          );
          processingRef.current = false;
        },
      });

      abortRef.current = abort;
    };

    processNext();
  }, [items]);

  return (
    <SearchQueueContext.Provider value={{ items, addSearch, removeItem, clearAll, retryItem }}>
      {children}
    </SearchQueueContext.Provider>
  );
}

export function useSearchQueue() {
  const ctx = useContext(SearchQueueContext);
  if (!ctx) throw new Error('useSearchQueue must be used within SearchQueueProvider');
  return ctx;
}
