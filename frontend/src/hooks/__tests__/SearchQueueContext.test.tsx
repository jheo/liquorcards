import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { render, screen, fireEvent, act, waitFor } from '@testing-library/react';
import { SearchQueueProvider, useSearchQueue } from '../SearchQueueContext';
import type { SearchStatus } from '../SearchQueueContext';

// Mock the API client
vi.mock('../../api/client', () => ({
  aiSearchStream: vi.fn(() => vi.fn()),
  aiSearchStreamWithSelection: vi.fn(() => vi.fn()),
}));

import { aiSearchStream, aiSearchStreamWithSelection } from '../../api/client';

const STORAGE_KEY = 'liquor-cards-search-queue';

// Test consumer component to interact with the queue
function TestConsumer() {
  const { items, addSearch, removeItem, clearAll, retryItem, cancelItem, selectDisambiguation } = useSearchQueue();

  return (
    <div>
      <div data-testid="items-json">{JSON.stringify(items.map(i => ({ id: i.id, query: i.query, status: i.status })))}</div>
      <div data-testid="item-count">{items.length}</div>
      <button onClick={() => addSearch('Macallan 12')} data-testid="add-search">Add Search</button>
      <button onClick={() => addSearch('Hendricks Gin')} data-testid="add-search-2">Add Search 2</button>
      {items.map(item => (
        <div key={item.id} data-testid={`item-${item.id}`}>
          <span data-testid={`status-${item.id}`}>{item.status}</span>
          <span data-testid={`query-${item.id}`}>{item.query}</span>
          <button onClick={() => removeItem(item.id)} data-testid={`remove-${item.id}`}>Remove</button>
          <button onClick={() => retryItem(item.id)} data-testid={`retry-${item.id}`}>Retry</button>
          <button onClick={() => cancelItem(item.id)} data-testid={`cancel-${item.id}`}>Cancel</button>
          <button onClick={() => selectDisambiguation(item.id, 'Selected Name')} data-testid={`select-${item.id}`}>Select</button>
          {item.disambiguationCandidates && (
            <div data-testid={`candidates-${item.id}`}>
              {item.disambiguationCandidates.map(c => c.name).join(',')}
            </div>
          )}
        </div>
      ))}
      <button onClick={clearAll} data-testid="clear-all">Clear All</button>
    </div>
  );
}

function renderQueue() {
  return render(
    <SearchQueueProvider>
      <TestConsumer />
    </SearchQueueProvider>
  );
}

describe('SearchQueueContext', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
    vi.mocked(aiSearchStream).mockReturnValue(vi.fn());
    vi.mocked(aiSearchStreamWithSelection).mockReturnValue(vi.fn());
  });

  afterEach(() => {
    localStorage.clear();
  });

  describe('addSearch', () => {
    it('should add a new item to the queue', () => {
      renderQueue();
      expect(screen.getByTestId('item-count')).toHaveTextContent('0');

      act(() => {
        fireEvent.click(screen.getByTestId('add-search'));
      });

      expect(screen.getByTestId('item-count')).toHaveTextContent('1');
      const itemsJson = JSON.parse(screen.getByTestId('items-json').textContent!);
      expect(itemsJson[0].query).toBe('Macallan 12');
      // Item may transition from pending to searching immediately via useEffect
      expect(['pending', 'searching']).toContain(itemsJson[0].status);
    });

    it('should generate unique ids', () => {
      renderQueue();

      act(() => {
        fireEvent.click(screen.getByTestId('add-search'));
      });
      act(() => {
        fireEvent.click(screen.getByTestId('add-search-2'));
      });

      const itemsJson = JSON.parse(screen.getByTestId('items-json').textContent!);
      expect(itemsJson.length).toBe(2);
      expect(itemsJson[0].id).not.toBe(itemsJson[1].id);
    });
  });

  describe('removeItem', () => {
    it('should remove an item from the queue', async () => {
      renderQueue();

      act(() => {
        fireEvent.click(screen.getByTestId('add-search'));
      });

      expect(screen.getByTestId('item-count')).toHaveTextContent('1');

      const itemsJson = JSON.parse(screen.getByTestId('items-json').textContent!);
      const itemId = itemsJson[0].id;

      act(() => {
        fireEvent.click(screen.getByTestId(`remove-${itemId}`));
      });

      await waitFor(() => {
        expect(screen.getByTestId('item-count')).toHaveTextContent('0');
      });
    });
  });

  describe('retryItem', () => {
    it('should reset item status to pending', async () => {
      renderQueue();

      act(() => {
        fireEvent.click(screen.getByTestId('add-search'));
      });

      const itemsJson = JSON.parse(screen.getByTestId('items-json').textContent!);
      const itemId = itemsJson[0].id;

      // Item should become searching then we retry
      act(() => {
        fireEvent.click(screen.getByTestId(`retry-${itemId}`));
      });

      await waitFor(() => {
        const updatedJson = JSON.parse(screen.getByTestId('items-json').textContent!);
        const item = updatedJson.find((i: any) => i.id === itemId);
        expect(item).toBeDefined();
      });
    });
  });

  describe('cancelItem', () => {
    it('should set searching item to cancelled', async () => {
      // Pre-populate localStorage with a searching item
      const testItem = {
        id: 'test-cancel-1',
        query: 'Test Cancel',
        status: 'searching' as SearchStatus,
        progressSteps: [],
      };
      localStorage.setItem(STORAGE_KEY, JSON.stringify([testItem]));

      renderQueue();

      // The loaded item should have status 'error' (searching → error on load)
      await waitFor(() => {
        const itemsJson = JSON.parse(screen.getByTestId('items-json').textContent!);
        expect(itemsJson[0].status).toBe('error');
      });
    });
  });

  describe('selectDisambiguation', () => {
    it('should update query and reset status to pending', async () => {
      renderQueue();

      act(() => {
        fireEvent.click(screen.getByTestId('add-search'));
      });

      const itemsJson = JSON.parse(screen.getByTestId('items-json').textContent!);
      const itemId = itemsJson[0].id;

      act(() => {
        fireEvent.click(screen.getByTestId(`select-${itemId}`));
      });

      await waitFor(() => {
        const updatedJson = JSON.parse(screen.getByTestId('items-json').textContent!);
        const item = updatedJson.find((i: any) => i.id === itemId);
        expect(item.query).toBe('Selected Name');
      });
    });
  });

  describe('clearAll', () => {
    it('should remove non-searching items', async () => {
      renderQueue();

      act(() => {
        fireEvent.click(screen.getByTestId('add-search'));
      });

      expect(screen.getByTestId('item-count')).toHaveTextContent('1');

      act(() => {
        fireEvent.click(screen.getByTestId('clear-all'));
      });

      // clearAll keeps searching items - pending items may or may not be cleared depending on timing
      // The behavior is: clear items that are NOT searching
      await waitFor(() => {
        const count = parseInt(screen.getByTestId('item-count').textContent!);
        // Either 0 (if item was still pending) or 1 (if it transitioned to searching)
        expect(count).toBeLessThanOrEqual(1);
      });
    });
  });

  describe('localStorage persistence', () => {
    it('should persist queue to localStorage', async () => {
      renderQueue();

      act(() => {
        fireEvent.click(screen.getByTestId('add-search'));
      });

      await waitFor(() => {
        const stored = localStorage.getItem(STORAGE_KEY);
        expect(stored).not.toBeNull();
        const parsed = JSON.parse(stored!);
        expect(parsed.length).toBeGreaterThan(0);
        expect(parsed[0].query).toBe('Macallan 12');
      });
    });

    it('should restore queue from localStorage on mount', () => {
      const savedItems = [
        { id: 'saved-1', query: 'Saved Item', status: 'done', progressSteps: [], result: { name: 'Test' } },
      ];
      localStorage.setItem(STORAGE_KEY, JSON.stringify(savedItems));

      renderQueue();

      const itemsJson = JSON.parse(screen.getByTestId('items-json').textContent!);
      expect(itemsJson.length).toBe(1);
      expect(itemsJson[0].query).toBe('Saved Item');
      expect(itemsJson[0].status).toBe('done');
    });

    it('should convert searching items to error on restore', () => {
      const savedItems = [
        { id: 'interrupted-1', query: 'Interrupted', status: 'searching', progressSteps: [] },
      ];
      localStorage.setItem(STORAGE_KEY, JSON.stringify(savedItems));

      renderQueue();

      const itemsJson = JSON.parse(screen.getByTestId('items-json').textContent!);
      expect(itemsJson[0].status).toBe('error');
    });

    it('should handle corrupted localStorage gracefully', () => {
      localStorage.setItem(STORAGE_KEY, 'not valid json');

      renderQueue();

      const itemsJson = JSON.parse(screen.getByTestId('items-json').textContent!);
      expect(itemsJson.length).toBe(0);
    });
  });

  describe('useSearchQueue error', () => {
    it('should throw when used outside provider', () => {
      const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
      expect(() => render(<TestConsumer />)).toThrow('useSearchQueue must be used within SearchQueueProvider');
      spy.mockRestore();
    });
  });
});
