import React, { useState } from 'react';
import { Search, RefreshCw, Zap, CheckCircle2, AlertTriangle, ShieldAlert, ShoppingCart, Layers } from 'lucide-react';
import { Product } from '../types';

interface CatalogViewProps {
  products: Product[];
  categories: string[];
  selectedCategory: string;
  onSelectCategory: (category: string) => void;
  searchQuery: string;
  onSearchChange: (q: string) => void;
  onAddToCart: (product: Product, quantity: number) => void;
  onTriggerSync: () => void;
  isSyncing: boolean;
}

export const CatalogView: React.FC<CatalogViewProps> = ({
  products,
  categories,
  selectedCategory,
  onSelectCategory,
  searchQuery,
  onSearchChange,
  onAddToCart,
  onTriggerSync,
  isSyncing,
}) => {
  const [quantities, setQuantities] = useState<Record<string, number>>({});

  const handleQtyChange = (sku: string, qty: number, max: number) => {
    const val = Math.max(1, Math.min(qty, max > 0 ? max : 1));
    setQuantities((prev) => ({ ...prev, [sku]: val }));
  };

  return (
    <div className="space-y-6">
      {/* Concurrency Banner */}
      <div className="bg-gradient-to-r from-sky-900/60 to-indigo-900/60 border border-sky-700/60 rounded-xl p-4 sm:p-5 shadow-sm text-slate-100 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-start space-x-3.5">
          <div className="p-2.5 bg-sky-500/20 text-sky-400 rounded-lg shrink-0 border border-sky-400/30">
            <Zap className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h3 className="font-semibold text-white text-base">Pessimistic Locking & Zero Overselling Guarantee</h3>
              <span className="text-[11px] font-mono px-2 py-0.5 bg-amber-500/20 text-amber-300 border border-amber-500/30 rounded-full font-bold">
                SELECT FOR UPDATE
              </span>
            </div>
            <p className="text-xs text-slate-300 mt-1 max-w-2xl leading-relaxed">
              Every stock reservation executes under strict JPA pessimistic locking (<code className="text-sky-300">LockModeType.PESSIMISTIC_WRITE</code>). Check out <span className="font-semibold text-white">SKU-HOT-ITEM-99</span> below to observe high-concurrency reservation safety and atomic inventory release.
            </p>
          </div>
        </div>

        <button
          onClick={onTriggerSync}
          disabled={isSyncing}
          className="inline-flex items-center space-x-2 px-3.5 py-2 bg-sky-600 hover:bg-sky-500 disabled:opacity-50 text-white text-xs font-semibold rounded-lg shadow-sm transition shrink-0"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isSyncing ? 'animate-spin' : ''}`} />
          <span>{isSyncing ? 'Syncing FakeStore...' : 'Sync Live FakeStore API'}</span>
        </button>
      </div>

      {/* Search and Category Filters */}
      <div className="flex flex-col sm:flex-row gap-3 items-center justify-between bg-white p-4 rounded-xl border border-slate-200 shadow-sm">
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
          <input
            type="text"
            placeholder="Search catalog by title, SKU, or keyword..."
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            className="w-full pl-9 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-sky-500 focus:bg-white text-slate-800"
          />
        </div>

        {/* Category Pills */}
        <div className="flex flex-wrap gap-1.5 w-full sm:w-auto">
          <button
            onClick={() => onSelectCategory('')}
            className={`px-3 py-1 rounded-full text-xs font-medium transition ${
              selectedCategory === ''
                ? 'bg-slate-900 text-white'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            All Categories ({products.length})
          </button>
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => onSelectCategory(cat)}
              className={`px-3 py-1 rounded-full text-xs font-medium capitalize transition ${
                selectedCategory === cat
                  ? 'bg-slate-900 text-white'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* Products Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
        {products.map((product) => {
          const qty = quantities[product.sku] || 1;
          const isHotItem = product.sku === 'SKU-HOT-ITEM-99';
          const isOutOfStock = product.availableStock <= 0;

          return (
            <div
              key={product.sku}
              className={`bg-white rounded-xl border transition-all duration-200 flex flex-col justify-between overflow-hidden shadow-sm hover:shadow-md ${
                isHotItem ? 'border-sky-400 ring-2 ring-sky-200' : 'border-slate-200'
              }`}
            >
              {/* Product Header & Media */}
              <div>
                <div className="relative h-44 bg-slate-50 p-4 flex items-center justify-center border-b border-slate-100">
                  <img
                    src={product.imageUrl}
                    alt={product.title}
                    className="max-h-36 max-w-full object-contain transition-transform duration-300 hover:scale-105"
                    loading="lazy"
                  />
                  {/* Origin Badge */}
                  <span
                    className={`absolute top-2.5 right-2.5 text-[10px] font-mono px-2 py-0.5 rounded-full font-semibold border ${
                      product.sourceOrigin === 'LIVE_EXTERNAL_DATA'
                        ? 'bg-emerald-50 text-emerald-700 border-emerald-300'
                        : product.sourceOrigin === 'CACHED_EXTERNAL_DATA'
                        ? 'bg-amber-50 text-amber-700 border-amber-300'
                        : 'bg-indigo-50 text-indigo-700 border-indigo-300'
                    }`}
                  >
                    {product.sourceOrigin === 'LIVE_EXTERNAL_DATA' ? '● LIVE API' : '⚡ CACHED'}
                  </span>

                  {isHotItem && (
                    <span className="absolute top-2.5 left-2.5 text-[10px] font-mono px-2 py-0.5 rounded-full font-bold bg-sky-600 text-white shadow-sm flex items-center gap-1">
                      <Zap className="w-3 h-3" /> BENCHMARK ASSET
                    </span>
                  )}
                </div>

                {/* Details */}
                <div className="p-4">
                  <div className="flex items-center justify-between text-[11px] text-slate-500 uppercase tracking-wider font-mono">
                    <span>{product.category}</span>
                    <span>{product.sku}</span>
                  </div>

                  <h4 className="font-semibold text-sm text-slate-900 mt-1.5 line-clamp-2 leading-snug" title={product.title}>
                    {product.title}
                  </h4>

                  <p className="text-xs text-slate-500 mt-1.5 line-clamp-2 leading-relaxed">
                    {product.description}
                  </p>
                </div>
              </div>

              {/* Price, Stock & Action Footer */}
              <div className="p-4 pt-0 border-t border-slate-50 mt-2">
                <div className="flex items-center justify-between mb-3 pt-2">
                  <div className="text-base font-bold text-slate-900">
                    ${product.price.toFixed(2)}{' '}
                    <span className="text-xs font-normal text-slate-500 font-mono">USD</span>
                  </div>

                  {/* Stock Indicator */}
                  <div className="flex items-center space-x-1.5 text-xs font-medium">
                    {isOutOfStock ? (
                      <span className="px-2 py-0.5 rounded-md bg-rose-50 text-rose-700 font-semibold border border-rose-200">
                        Out of Stock
                      </span>
                    ) : product.availableStock <= 5 ? (
                      <span className="px-2 py-0.5 rounded-md bg-amber-50 text-amber-700 font-semibold border border-amber-200 flex items-center gap-1">
                        <AlertTriangle className="w-3 h-3" /> Low: {product.availableStock} left
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 rounded-md bg-emerald-50 text-emerald-700 font-semibold border border-emerald-200 flex items-center gap-1">
                        <CheckCircle2 className="w-3 h-3" /> {product.availableStock} available
                      </span>
                    )}
                  </div>
                </div>

                {/* Add to Cart Stepper */}
                <div className="flex items-center space-x-2">
                  <div className="flex items-center border border-slate-300 rounded-lg overflow-hidden bg-slate-50">
                    <button
                      type="button"
                      disabled={isOutOfStock || qty <= 1}
                      onClick={() => handleQtyChange(product.sku, qty - 1, product.availableStock)}
                      className="px-2.5 py-1 text-slate-600 hover:bg-slate-200 disabled:opacity-30 text-xs font-bold"
                    >
                      -
                    </button>
                    <span className="px-2 text-xs font-semibold text-slate-800 min-w-[1.75rem] text-center font-mono">
                      {qty}
                    </span>
                    <button
                      type="button"
                      disabled={isOutOfStock || qty >= product.availableStock}
                      onClick={() => handleQtyChange(product.sku, qty + 1, product.availableStock)}
                      className="px-2.5 py-1 text-slate-600 hover:bg-slate-200 disabled:opacity-30 text-xs font-bold"
                    >
                      +
                    </button>
                  </div>

                  <button
                    type="button"
                    disabled={isOutOfStock}
                    onClick={() => onAddToCart(product, qty)}
                    className="flex-1 py-1.5 px-3 bg-slate-900 hover:bg-sky-600 disabled:bg-slate-200 disabled:text-slate-400 text-white text-xs font-semibold rounded-lg transition-colors flex items-center justify-center space-x-1.5"
                  >
                    <ShoppingCart className="w-3.5 h-3.5" />
                    <span>{isOutOfStock ? 'Sold Out' : 'Add to Order'}</span>
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
