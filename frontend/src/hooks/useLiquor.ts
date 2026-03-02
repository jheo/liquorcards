import { useState, useEffect } from 'react';
import { getLiquor } from '../api/client';
import type { Liquor } from '../types/liquor';

export function useLiquor(id: number | undefined) {
  const [liquor, setLiquor] = useState<Liquor | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setError(null);
    getLiquor(id)
      .then((res) => setLiquor(res.data))
      .catch((err) => setError(err instanceof Error ? err.message : 'Failed to fetch liquor'))
      .finally(() => setLoading(false));
  }, [id]);

  return { liquor, loading, error };
}
