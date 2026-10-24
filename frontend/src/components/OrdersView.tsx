import React from 'react';
import { Order, OrderStatus } from '../types';
import { CheckCircle2, Clock, XCircle, AlertOctagon, RotateCcw, Package, ArrowRight, ShieldAlert } from 'lucide-react';

interface OrdersViewProps {
  orders: Order[];
  onCancelOrder: (orderId: number) => void;
  isCancelling: boolean;
  onRefresh: () => void;
}

export const OrdersView: React.FC<OrdersViewProps> = ({
  orders,
  onCancelOrder,
  isCancelling,
  onRefresh,
}) => {
  const getStatusBadge = (status: OrderStatus) => {
    switch (status) {
      case 'COMPLETED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-300">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" /> COMPLETED
          </span>
        );
      case 'INVENTORY_RESERVED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-sky-100 text-sky-800 border border-sky-300">
            <Clock className="w-3.5 h-3.5 text-sky-600" /> INVENTORY RESERVED
          </span>
        );
      case 'PAYMENT_AUTHORIZED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-100 text-indigo-800 border border-indigo-300">
            <Package className="w-3.5 h-3.5 text-indigo-600" /> PAYMENT AUTHORIZED
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-700 border border-slate-300">
            <XCircle className="w-3.5 h-3.5 text-slate-500" /> CANCELLED
          </span>
        );
      case 'FAILED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-800 border border-rose-300">
            <AlertOctagon className="w-3.5 h-3.5 text-rose-600" /> FAILED
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 border border-amber-300">
            <Clock className="w-3.5 h-3.5 text-amber-600" /> PENDING
          </span>
        );
    }
  };

  const renderStepper = (currentStatus: OrderStatus) => {
    if (currentStatus === 'CANCELLED') {
      return (
        <div className="p-3 bg-slate-100 rounded-lg text-xs text-slate-600 flex items-center justify-between border border-slate-200">
          <span className="flex items-center gap-1.5 font-medium text-slate-700">
            <XCircle className="w-4 h-4 text-slate-500" /> Order Cancelled
          </span>
          <span className="text-[11px] text-slate-500 font-mono">
            Reserved inventory restored to available stock pool
          </span>
        </div>
      );
    }

    if (currentStatus === 'FAILED') {
      return (
        <div className="p-3 bg-rose-50 rounded-lg text-xs text-rose-700 flex items-center justify-between border border-rose-200">
          <span className="flex items-center gap-1.5 font-semibold text-rose-800">
            <ShieldAlert className="w-4 h-4 text-rose-600" /> Risk Threshold Exceeded
          </span>
          <span className="text-[11px] text-rose-600 font-mono">
            Transaction declined & inventory automatically released
          </span>
        </div>
      );
    }

    const steps = [
      { key: 'PENDING', label: '1. Order Created' },
      { key: 'INVENTORY_RESERVED', label: '2. Stock Reserved' },
      { key: 'PAYMENT_AUTHORIZED', label: '3. Payment Authorized' },
      { key: 'COMPLETED', label: '4. Completed' },
    ];

    const stepWeights: Record<string, number> = {
      PENDING: 1,
      INVENTORY_RESERVED: 2,
      PAYMENT_AUTHORIZED: 3,
      COMPLETED: 4,
    };

    const currentWeight = stepWeights[currentStatus] || 1;

    return (
      <div className="grid grid-cols-4 gap-2 pt-2 pb-1">
        {steps.map((step, idx) => {
          const isDone = currentWeight >= idx + 1;
          const isCurrent = currentWeight === idx + 1;

          return (
            <div key={step.key} className="text-center">
              <div
                className={`h-1.5 rounded-full mb-1.5 transition-all duration-300 ${
                  isDone ? 'bg-sky-500' : 'bg-slate-200'
                }`}
              />
              <span
                className={`text-[10px] font-mono leading-tight block truncate ${
                  isCurrent
                    ? 'font-bold text-sky-700'
                    : isDone
                    ? 'font-medium text-slate-700'
                    : 'text-slate-400'
                }`}
              >
                {step.label}
              </span>
            </div>
          );
        })}
      </div>
    );
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-bold text-slate-900">Order Tracking & Lifecycle State Machine</h2>
          <p className="text-xs text-slate-500 font-mono">
            Real-time fulfillment tracking with deterministic customer cancellation and stock release
          </p>
        </div>
        <button
          onClick={onRefresh}
          className="px-3 py-1.5 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg text-xs font-semibold text-slate-700 shadow-sm transition"
        >
          Refresh Orders
        </button>
      </div>

      {orders.length === 0 ? (
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <Package className="w-12 h-12 text-slate-300 mx-auto mb-3 stroke-1" />
          <h4 className="text-sm font-semibold text-slate-800">No orders placed yet</h4>
          <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
            Place an order from the Catalog tab to witness the transactional outbox and state machine execution.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {orders.map((order) => {
            const canCancel =
              order.status === 'PENDING' ||
              order.status === 'INVENTORY_RESERVED' ||
              order.status === 'PAYMENT_AUTHORIZED';

            return (
              <div
                key={order.id}
                className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden p-5 space-y-4 hover:border-slate-300 transition"
              >
                {/* Header Info */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-slate-100 gap-2">
                  <div className="flex items-center space-x-3">
                    <span className="text-base font-bold text-slate-900">Order #{order.id}</span>
                    {getStatusBadge(order.status)}
                    <span className="text-xs text-slate-400 font-mono">
                      {new Date(order.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                    </span>
                  </div>

                  <div className="flex items-center space-x-3">
                    <span className="text-sm font-bold text-slate-900">
                      ${order.totalAmount.toFixed(2)} {order.currency}
                    </span>
                    {canCancel && (
                      <button
                        onClick={() => onCancelOrder(order.id)}
                        disabled={isCancelling}
                        className="px-2.5 py-1 bg-rose-50 hover:bg-rose-100 border border-rose-200 text-rose-700 rounded-lg text-xs font-semibold transition"
                      >
                        Cancel & Release Stock
                      </button>
                    )}
                  </div>
                </div>

                {/* State Machine Stepper */}
                {renderStepper(order.status)}

                {/* Items List */}
                <div className="bg-slate-50 rounded-lg p-3 border border-slate-100">
                  <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider block mb-2 font-mono">
                    Reserved Items ({order.items.length})
                  </span>
                  <div className="divide-y divide-slate-200">
                    {order.items.map((item, idx) => (
                      <div key={idx} className="py-1.5 flex items-center justify-between text-xs">
                        <div className="flex items-center space-x-2">
                          <span className="font-mono text-slate-400 font-semibold">{item.sku}</span>
                          <span className="text-slate-800 font-medium truncate max-w-xs">{item.productTitle}</span>
                        </div>
                        <div className="text-slate-600 font-mono">
                          {item.quantity} × ${item.unitPrice.toFixed(2)} = ${(item.quantity * item.unitPrice).toFixed(2)}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Metadata & Idempotency Key */}
                <div className="flex flex-wrap items-center justify-between text-[11px] text-slate-500 pt-1 font-mono gap-2">
                  <div>
                    <span className="text-slate-400">Customer: </span>
                    <span className="text-slate-700 font-semibold">{order.customerEmail}</span>
                  </div>
                  <div>
                    <span className="text-slate-400">Idempotency-Key: </span>
                    <span className="text-sky-700 bg-sky-50 px-2 py-0.5 rounded border border-sky-200">{order.idempotencyKey}</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
