import type { ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import './NavItem.css';

interface NavItemProps {
  to: string;
  icon: ReactNode;
  label: string;
}

export function NavItem({ to, icon, label }: NavItemProps) {
  const location = useLocation();
  const isActive = location.pathname === to;

  return (
    <Link to={to} className={`nav-item ${isActive ? 'nav-item-active' : ''}`}>
      <span className="nav-item-icon">{icon}</span>
      {label}
    </Link>
  );
}
