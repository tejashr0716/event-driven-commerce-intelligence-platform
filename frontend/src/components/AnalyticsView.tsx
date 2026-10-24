import React from 'react';
import { AnalyticsSummary } from '../types';
import { DollarSign, CheckCircle2, XCircle, AlertTriangle, Activity, PackageCheck, Layers, BarChart3 } from 'lucide-react';

interface AnalyticsViewProps {
  analytics: AnalyticsSummary | null;
  onRefresh: () => void;
  isLoading: boolean;
}

export const AnalyticsView: React.FC<AnalyticsViewProps> = ({ analytics, onRefresh, isLoading }) => {
  if (!analytics) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-12 text-center text-slate-500">
        <Activity className="w-8 h-8 animate-spin mx-auto text-sky-500 mb-2" />
        <p className="text-sm">Computing stream analytics with Java 21 Functional Streams...</p>
      </div>
    );
  }

  const categoryEntries = Object.entries(analytics.categoryDemand || {});
  const totalCategoryUnits = categoryEntries.reduce((sum, [_, count]) => sum + count, 0);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center space-x-2">
            <h2 className="text-lg font-bold text-slate-900">Operational Analytics & Stream Metrics</h2>
            <span className="px-2 py-0.5 text-xs font-mono bg-indigo-50 text-indigo-700 border border-indigo-200 rounded-full font-semibold">
              Java 21 Streams
            </span>
          </div>
          <p className="text-xs text-slate-500 font-mono">
            High-throughput event aggregations computed via functional pipelines without external analytics batch lag
          </p>
        </div>
        <button
          onClick={onRefresh}
          disabled={isLoading}
          className="px-3.5 py-1.5 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg text-xs font-semibold text-slate-700 shadow-sm transition"
        >
          {isLoading ? 'Recalculating...' : 'Refresh Metrics'}
        </button>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: GMV */}
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex items-start justify-between">
          <div>
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider font-mono">
              Gross Merchandise Value
            </span>
            <div className="text-2xl font-black text-slate-900 mt-1">
              ${analytics.grossMerchandiseValue.toFixed(2)}
            </div>
            <span className="text-[11px] text-emerald-600 font-medium flex items-center gap-1 mt-1 font-mono">
              <CheckCircle2 className="w-3 h-3" /> Settled COMPLETED Volume
            </span>
          </div>
          <div className="p-2.5 bg-emerald-50 text-emerald-600 rounded-lg border border-emerald-200">
            <DollarSign className="w-5 h-5" />
          </div>
        </div>

        {/* Card 2: Total Orders */}
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex items-start justify-between">
          <div>
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider font-mono">
              Total Order Volume
            </span>
            <div className="text-2xl font-black text-slate-900 mt-1">{analytics.totalOrders}</div>
            <div className="text-[11px] text-slate-500 font-mono mt-1 space-x-2">
              <span className="text-emerald-700 font-semibold">{analytics.completedOrders} won</span>
              <span>•</span>
              <span className="text-slate-600">{analytics.cancelledOrders} can</span>
              <span>•</span>
              <span className="text-rose-600">{analytics.failedOrders} fail</span>
            </div>
          </div>
          <div className="p-2.5 bg-sky-50 text-sky-600 rounded-lg border border-sky-200">
            <PackageCheck className="w-5 h-5" />
          </div>
        </div>

        {/* Card 3: Rates */}
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex items-start justify-between">
          <div>
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider font-mono">
              Risk & Fail Rate
            </span>
            <div className="text-2xl font-black text-slate-900 mt-1">
              {analytics.reservationFailureRatePercent}%
            </div>
            <span className="text-[11px] text-slate-500 font-mono mt-1 block">
              Cancellation: {analytics.cancellationRatePercent}%
            </span>
          </div>
          <div className="p-2.5 bg-rose-50 text-rose-600 rounded-lg border border-rose-200">
            <AlertTriangle className="w-5 h-5" />
          </div>
        </div>

        {/* Card 4: Outbox Lag */}
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex items-start justify-between">
          <div>
            <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider font-mono">
              Pending Outbox Lag
            </span>
            <div className="text-2xl font-black text-slate-900 mt-1">
              {analytics.pendingOutboxEvents}
            </div>
            <span className="text-[11px] text-indigo-600 font-mono font-medium mt-1 block">
              Transactional Outbox Buffer
            </span>
          </div>
          <div className="p-2.5 bg-indigo-50 text-indigo-600 rounded-lg border border-indigo-200">
            <BarChart3 className="w-5 h-5" />
          </div>
        </div>
      </div>

      {/* Two Column Layout: Category Demand & Real-Time Stock Levels */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Category Demand */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-4">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div className="flex items-center space-x-2">
              <Layers className="w-4 h-4 text-sky-600" />
              <h3 className="font-bold text-sm text-slate-900">Functional Stream Category Demand</h3>
            </div>
            <span className="text-xs font-mono text-slate-500">{totalCategoryUnits} total units sold</span>
          </div>

          {categoryEntries.length === 0 ? (
            <p className="text-xs text-slate-400 py-6 text-center">No completed order units recorded yet.</p>
          ) : (
            <div className="space-y-3">
              {categoryEntries.map(([category, count]) => {
                const percentage = totalCategoryUnits > 0 ? (count / totalCategoryUnits) * 100 : 0;
                return (
                  <div key={category} className="space-y-1">
                    <div className="flex items-center justify-between text-xs font-mono">
                      <span className="capitalize font-semibold text-slate-800">{category}</span>
                      <span className="text-slate-600">
                        {count} units ({percentage.toFixed(1)}%)
                      </span>
                    </div>
                    <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-sky-500 rounded-full transition-all duration-500"
                        style={{ width: `${Math.max(percentage, 4)}%` }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Real-time Inventory Snapshot */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-4">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div className="flex items-center space-x-2">
              <Activity className="w-4 h-4 text-indigo-600" />
              <h3 className="font-bold text-sm text-slate-900">Real-Time Inventory State Snapshot</h3>
            </div>
            <span className="text-xs font-mono text-slate-500">
              {Object.keys(analytics.currentStockLevels || {}).length} Tracked SKUs
            </span>
          </div>

          <div className="max-h-72 overflow-y-auto divide-y divide-slate-100 pr-1">
            {Object.entries(analytics.currentStockLevels || {}).map(([sku, stock]) => {
              const isHot = sku === 'SKU-HOT-ITEM-99';
              return (
                <div key={sku} className="py-2 flex items-center justify-between text-xs">
                  <div className="flex items-center space-x-2">
                    <span className="font-mono font-semibold text-slate-700">{sku}</span>
                    {isHot && (
                      <span className="px-1.5 py-0.2 text-[9px] bg-sky-100 text-sky-800 font-bold rounded">
                        HOT TEST
                      </span>
                    )}
                  </div>

                  <div>
                    {stock <= 0 ? (
                      <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-rose-50 text-rose-700 font-mono">
                        0 Stockout
                      </span>
                    ) : stock <= 5 ? (
                      <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-amber-50 text-amber-700 font-mono">
                        {stock} Low
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-emerald-50 text-emerald-700 font-mono">
                        {stock} units
                      </span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};
