import { useState, useEffect, useCallback } from 'react';
import { getLiquors } from '../api/client';
import type { Liquor } from '../types/liquor';

export function useLiquors(initialParams?: Record<string, string>) {
  const [liquors, setLiquors] = useState<Liquor[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetch = useCallback(async (params?: Record<string, string>) => {
    setLoading(true);
    setError(null);
    try {
      const res = await getLiquors(params);
      setLiquors(res.data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch liquors');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetch(initialParams);
  }, []);

  return { liquors, loading, error, refetch: fetch };
}
