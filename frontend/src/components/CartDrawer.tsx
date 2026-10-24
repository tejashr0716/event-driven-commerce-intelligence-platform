import React, { useState } from 'react';
import { X, Key, ShieldCheck, ShoppingCart, Trash2, ArrowRight, CheckCircle2, AlertCircle } from 'lucide-react';
import { CartItem } from '../types';

interface CartDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  cart: CartItem[];
  onUpdateQuantity: (sku: string, qty: number) => void;
  onRemoveItem: (sku: string) => void;
  onSubmitOrder: (idempotencyKey: string, shippingAddress: string) => Promise<boolean>;
  isSubmitting: boolean;
}

export const CartDrawer: React.FC<CartDrawerProps> = ({
  isOpen,
  onClose,
  cart,
  onUpdateQuantity,
  onRemoveItem,
  onSubmitOrder,
  isSubmitting,
}) => {
  const [idempotencyKey, setIdempotencyKey] = useState<string>(() => crypto.randomUUID());
  const [shippingAddress, setShippingAddress] = useState<string>(
    'Infosys Electronic City Development Centre, Hosur Road, Bengaluru 560100'
  );
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (!isOpen) return null;

  const totalAmount = cart.reduce((acc, item) => acc + item.product.price * item.quantity, 0);

  const handleGenerateNewKey = () => {
    setIdempotencyKey(crypto.randomUUID());
    setErrorMessage(null);
  };

  const handleCheckout = async () => {
    setErrorMessage(null);
    try {
      const success = await onSubmitOrder(idempotencyKey, shippingAddress);
      if (success) {
        onClose();
      }
    } catch (err: any) {
      setErrorMessage(err.message || 'Order reservation failed. Please check stock levels.');
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-slate-900/60 backdrop-blur-sm transition-opacity">
      <div className="absolute inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-md bg-white shadow-2xl flex flex-col justify-between">
          {/* Header */}
          <div className="p-4 sm:p-5 border-b border-slate-200 flex items-center justify-between bg-slate-50">
            <div className="flex items-center space-x-2.5">
              <div className="p-2 bg-sky-100 text-sky-700 rounded-lg">
                <ShoppingCart className="w-5 h-5" />
              </div>
              <div>
                <h3 className="font-bold text-slate-900 text-base">Cart & Idempotent Checkout</h3>
                <p className="text-xs text-slate-500 font-mono">{cart.length} unique items selected</p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-200"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Cart Items List */}
          <div className="flex-1 overflow-y-auto p-4 sm:p-5 space-y-3">
            {cart.length === 0 ? (
              <div className="text-center py-16 text-slate-400">
                <ShoppingCart className="w-12 h-12 mx-auto stroke-1 text-slate-300 mb-2" />
                <p className="text-sm font-medium">Your order cart is empty</p>
                <p className="text-xs text-slate-400 mt-1">Select items from the catalog to begin reservation</p>
              </div>
            ) : (
              cart.map((item) => (
                <div
                  key={item.product.sku}
                  className="flex items-center space-x-3 p-3 bg-slate-50 rounded-lg border border-slate-200"
                >
                  <img
                    src={item.product.imageUrl}
                    alt={item.product.title}
                    className="w-12 h-12 object-contain bg-white p-1 rounded border border-slate-200 shrink-0"
                  />
                  <div className="flex-1 min-w-0">
                    <h5 className="text-xs font-semibold text-slate-800 truncate" title={item.product.title}>
                      {item.product.title}
                    </h5>
                    <div className="flex items-center space-x-2 mt-0.5 text-[11px] text-slate-500 font-mono">
                      <span>${item.product.price.toFixed(2)} ea</span>
                      <span>•</span>
                      <span className="text-sky-700 font-semibold">
                        Subtotal: ${(item.product.price * item.quantity).toFixed(2)}
                      </span>
                    </div>

                    <div className="flex items-center space-x-2 mt-2">
                      <div className="flex items-center border border-slate-300 rounded bg-white">
                        <button
                          type="button"
                          onClick={() => onUpdateQuantity(item.product.sku, item.quantity - 1)}
                          className="px-2 py-0.5 text-xs text-slate-600 hover:bg-slate-100 font-bold"
                        >
                          -
                        </button>
                        <span className="px-2 text-xs font-semibold text-slate-800 font-mono">
                          {item.quantity}
                        </span>
                        <button
                          type="button"
                          onClick={() => onUpdateQuantity(item.product.sku, item.quantity + 1)}
                          className="px-2 py-0.5 text-xs text-slate-600 hover:bg-slate-100 font-bold"
                        >
                          +
                        </button>
                      </div>

                      <button
                        type="button"
                        onClick={() => onRemoveItem(item.product.sku)}
                        className="p-1 text-rose-500 hover:text-rose-700 text-xs flex items-center space-x-0.5"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                        <span>Remove</span>
                      </button>
                    </div>
                  </div>
                </div>
              ))
            )}

            {/* Error Notification */}
            {errorMessage && (
              <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg flex items-start space-x-2.5 text-rose-800 text-xs">
                <AlertCircle className="w-4 h-4 shrink-0 text-rose-600 mt-0.5" />
                <div>
                  <span className="font-semibold">Order Error: </span>
                  {errorMessage}
                </div>
              </div>
            )}

            {/* Idempotency & Contract Proof Section */}
            {cart.length > 0 && (
              <div className="pt-2 space-y-3">
                <div className="p-3.5 bg-slate-900 text-white rounded-xl border border-slate-800 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-semibold flex items-center text-sky-400">
                      <Key className="w-3.5 h-3.5 mr-1" /> Idempotency-Key Header
                    </span>
                    <button
                      type="button"
                      onClick={handleGenerateNewKey}
                      className="text-[10px] text-sky-300 hover:text-white underline font-mono"
                    >
                      Regenerate UUID
                    </button>
                  </div>
                  <input
                    type="text"
                    value={idempotencyKey}
                    onChange={(e) => setIdempotencyKey(e.target.value)}
                    className="w-full text-xs font-mono bg-slate-950 border border-slate-700 rounded px-2.5 py-1 text-sky-300 focus:outline-none"
                  />
                  <p className="text-[11px] text-slate-400 leading-snug">
                    Submitting multiple times with the same key safely returns the identical cached order response without double reservations or duplicate billing.
                  </p>
                </div>

                {/* Shipping Address */}
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-slate-700">Delivery Address</label>
                  <textarea
                    rows={2}
                    value={shippingAddress}
                    onChange={(e) => setShippingAddress(e.target.value)}
                    className="w-full text-xs p-2 bg-slate-50 border border-slate-300 rounded-lg focus:outline-none focus:ring-1 focus:ring-sky-500 text-slate-800"
                  />
                </div>
              </div>
            )}
          </div>

          {/* Footer Checkout Action */}
          {cart.length > 0 && (
            <div className="p-4 sm:p-5 border-t border-slate-200 bg-slate-50 space-y-3">
              <div className="flex items-center justify-between text-sm">
                <span className="text-slate-600 font-medium">Order Total:</span>
                <span className="text-lg font-bold text-slate-900">${totalAmount.toFixed(2)} USD</span>
              </div>

              <button
                type="button"
                disabled={isSubmitting || cart.length === 0}
                onClick={handleCheckout}
                className="w-full py-2.5 px-4 bg-sky-600 hover:bg-sky-500 disabled:opacity-50 text-white font-semibold text-xs rounded-xl shadow transition flex items-center justify-center space-x-2"
              >
                {isSubmitting ? (
                  <span>Reserving Inventory with Pessimistic Lock...</span>
                ) : (
                  <>
                    <span>Place Order (Transactional Outbox)</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
