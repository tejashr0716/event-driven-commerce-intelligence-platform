import React from 'react';
import { ShoppingBag, ShieldCheck, Activity, Database, ShoppingCart, UserCheck, RefreshCw } from 'lucide-react';
import { UserRole, UserSession } from '../types';

interface NavbarProps {
  currentSession: UserSession;
  onSwitchUser: (email: string) => void;
  activeTab: string;
  setActiveTab: (tab: string) => void;
  cartCount: number;
  onOpenCart: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  currentSession,
  onSwitchUser,
  activeTab,
  setActiveTab,
  cartCount,
  onOpenCart,
}) => {
  const users = [
    { email: 'customer@infosys.com', label: 'Customer', role: 'CUSTOMER' },
    { email: 'admin@infosys.com', label: 'Administrator', role: 'ADMIN' },
    { email: 'inventory@infosys.com', label: 'Inventory Mgr', role: 'INVENTORY_MANAGER' },
  ];

  return (
    <header className="sticky top-0 z-40 bg-slate-900 border-b border-slate-800 text-white shadow-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo & Platform Title */}
          <div className="flex items-center space-x-3 cursor-pointer" onClick={() => setActiveTab('catalog')}>
            <div className="p-2 bg-gradient-to-tr from-sky-600 to-indigo-600 rounded-lg shadow-sm">
              <ShoppingBag className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-bold text-base tracking-tight text-white">Commerce Intelligence</span>
                <span className="px-2 py-0.5 text-xs font-semibold bg-sky-950 text-sky-400 border border-sky-800 rounded-full">
                  Java 21 • Spring Boot 3.3
                </span>
              </div>
              <p className="text-xs text-slate-400">Infosys Specialist Programmer Flagship Platform</p>
            </div>
          </div>

          {/* Navigation Tabs */}
          <nav className="hidden md:flex space-x-1">
            <button
              onClick={() => setActiveTab('catalog')}
              className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                activeTab === 'catalog'
                  ? 'bg-sky-600 text-white shadow-sm'
                  : 'text-slate-300 hover:text-white hover:bg-slate-800'
              }`}
            >
              Catalog & Stock
            </button>
            <button
              onClick={() => setActiveTab('orders')}
              className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                activeTab === 'orders'
                  ? 'bg-sky-600 text-white shadow-sm'
                  : 'text-slate-300 hover:text-white hover:bg-slate-800'
              }`}
            >
              Order Tracking
            </button>
            <button
              onClick={() => setActiveTab('analytics')}
              className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                activeTab === 'analytics'
                  ? 'bg-sky-600 text-white shadow-sm'
                  : 'text-slate-300 hover:text-white hover:bg-slate-800'
              }`}
            >
              Analytics Stream
            </button>
            <button
              onClick={() => setActiveTab('distributed')}
              className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${
                activeTab === 'distributed'
                  ? 'bg-sky-600 text-white shadow-sm'
                  : 'text-slate-300 hover:text-white hover:bg-slate-800'
              }`}
            >
              Outbox & DLQ
            </button>
          </nav>

          {/* Identity Switcher & Cart */}
          <div className="flex items-center space-x-3">
            {/* Persona Switcher */}
            <div className="flex items-center space-x-1 bg-slate-800/80 p-1 rounded-lg border border-slate-700">
              <span className="text-xs text-slate-400 pl-2 pr-1 font-mono flex items-center">
                <UserCheck className="w-3.5 h-3.5 mr-1 text-sky-400" />
                Role:
              </span>
              <select
                aria-label="Select active user role"
                value={currentSession.email}
                onChange={(e) => onSwitchUser(e.target.value)}
                className="bg-slate-900 text-xs font-medium text-sky-300 rounded px-2 py-1 border border-slate-700 focus:outline-none focus:ring-1 focus:ring-sky-500 cursor-pointer"
              >
                {users.map((u) => (
                  <option key={u.email} value={u.email}>
                    {u.label} ({u.role})
                  </option>
                ))}
              </select>
            </div>

            {/* Cart Trigger Button */}
            <button
              onClick={onOpenCart}
              className="relative p-2 text-slate-300 hover:text-white hover:bg-slate-800 rounded-lg transition-colors border border-slate-700"
              title="View Cart & Idempotent Checkout"
            >
              <ShoppingCart className="w-5 h-5 text-sky-400" />
              {cartCount > 0 && (
                <span className="absolute -top-1.5 -right-1.5 bg-rose-600 text-white text-[10px] font-bold px-1.5 py-0.5 rounded-full border-2 border-slate-900 animate-pulse">
                  {cartCount}
                </span>
              )}
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
