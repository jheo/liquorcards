import type { ReactNode } from 'react';
import './StatBadge.css';

interface StatBadgeProps {
  icon: ReactNode;
  label: string;
  value: string;
}

export function StatBadge({ icon, label, value }: StatBadgeProps) {
  return (
    <div className="stat-badge">
      <span className="stat-badge-icon">{icon}</span>
      <span className="stat-badge-label">{label}</span>
      <span className="stat-badge-value">{value}</span>
    </div>
  );
}
