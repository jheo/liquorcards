import { useState, useEffect, useRef, useCallback } from 'react';
import { aiSearch } from '../api/client';
import type { AiSearchResult } from '../types/liquor';

export type SearchStatus = 'pending' | 'searching' | 'done' | 'error';

export interface SearchItem {
  id: string;
  query: string;
  provider: string;
  status: SearchStatus;
  result?: AiSearchResult;
  error?: string;
}

export function useSearchQueue() {
  const [items, setItems] = useState<SearchItem[]>([]);
  const processingRef = useRef(false);

  const addSearch = useCallback((query: string, provider: string = 'claude') => {
    const id = `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
    setItems((prev) => [
      ...prev,
      { id, query, provider, status: 'pending' },
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
        item.id === id ? { ...item, status: 'pending' as SearchStatus, error: undefined, result: undefined } : item
      )
    );
  }, []);

  useEffect(() => {
    const processNext = async () => {
      if (processingRef.current) return;

      const pendingItem = items.find((item) => item.status === 'pending');
      if (!pendingItem) return;

      processingRef.current = true;

      setItems((prev) =>
        prev.map((item) =>
          item.id === pendingItem.id ? { ...item, status: 'searching' as SearchStatus } : item
        )
      );

      try {
        const res = await aiSearch(pendingItem.query, pendingItem.provider);
        setItems((prev) =>
          prev.map((item) =>
            item.id === pendingItem.id
              ? { ...item, status: 'done' as SearchStatus, result: res.data }
              : item
          )
        );
      } catch (err) {
        setItems((prev) =>
          prev.map((item) =>
            item.id === pendingItem.id
              ? {
                  ...item,
                  status: 'error' as SearchStatus,
                  error: err instanceof Error ? err.message : 'AI search failed',
                }
              : item
          )
        );
      } finally {
        processingRef.current = false;
      }
    };

    processNext();
  }, [items]);

  return { items, addSearch, removeItem, clearAll, retryItem };
}
