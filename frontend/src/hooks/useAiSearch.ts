import { useState } from 'react';
import { aiSearch } from '../api/client';
import type { AiSearchResult } from '../types/liquor';

export function useAiSearch() {
  const [result, setResult] = useState<AiSearchResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const search = async (name: string, provider?: string) => {
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const res = await aiSearch(name, provider);
      setResult(res.data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'AI search failed');
    } finally {
      setLoading(false);
    }
  };

  const reset = () => {
    setResult(null);
    setError(null);
  };

  return { result, loading, error, search, reset };
}
