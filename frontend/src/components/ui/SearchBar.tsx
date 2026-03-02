import { Search, X } from 'lucide-react';
import './Input.css';
import './SearchBar.css';

interface SearchBarProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}

export function SearchBar({ value, onChange, placeholder = 'Search...' }: SearchBarProps) {
  return (
    <div className="search-bar">
      <Search size={18} className="search-icon" />
      <input
        className="input"
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
      />
      {value && (
        <button className="clear-btn" onClick={() => onChange('')}>
          <X size={16} />
        </button>
      )}
    </div>
  );
}
