import React, { useState, useEffect, useCallback } from 'react';
import { Navbar } from './components/Navbar';
import { CatalogView } from './components/CatalogView';
import { CartDrawer } from './components/CartDrawer';
import { OrdersView } from './components/OrdersView';
import { AnalyticsView } from './components/AnalyticsView';
import { DistributedSystemsView } from './components/DistributedSystemsView';
import {
  Product,
  CartItem,
  Order,
  AnalyticsSummary,
  SystemStatus,
  OutboxEvent,
  AuditLogItem,
  UserSession,
} from './types';

export const App: React.FC = () => {
  const [currentSession, setCurrentSession] = useState<UserSession>({
    userId: 1,
    email: 'customer@infosys.com',
    fullName: 'Enterprise Customer',
    role: 'CUSTOMER',
    token: '',
  });

  const [activeTab, setActiveTab] = useState<string>('catalog');
  const [isCartOpen, setIsCartOpen] = useState<boolean>(false);

  // Core Data State
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [cart, setCart] = useState<CartItem[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [analytics, setAnalytics] = useState<AnalyticsSummary | null>(null);
  const [systemStatus, setSystemStatus] = useState<SystemStatus | null>(null);
  const [outboxEvents, setOutboxEvents] = useState<OutboxEvent[]>([]);
  const [dlqEvents, setDlqEvents] = useState<OutboxEvent[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLogItem[]>([]);

  // Loading flags
  const [isSyncing, setIsSyncing] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [isCancelling, setIsCancelling] = useState<boolean>(false);
  const [isDispatching, setIsDispatching] = useState<boolean>(false);
  const [isAnalyticsLoading, setIsAnalyticsLoading] = useState<boolean>(false);

  // Authenticate user on switch
  const authenticateUser = useCallback(async (email: string) => {
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password: 'Infosys@2026' }),
      });
      if (res.ok) {
        const data = await res.json();
        setCurrentSession({
          userId: data.userId,
          email: data.email,
          fullName: data.fullName,
          role: data.role,
          token: data.token,
        });
      }
    } catch (e) {
      console.error('Failed to log in:', e);
    }
  }, []);

  // Fetch Catalog
  const fetchProducts = useCallback(async () => {
    try {
      let url = '/api/products?page=0&size=50';
      if (searchQuery) url += `&query=${encodeURIComponent(searchQuery)}`;
      if (selectedCategory) url += `&category=${encodeURIComponent(selectedCategory)}`;

      const res = await fetch(url);
      if (res.ok) {
        const data = await res.json();
        setProducts(data.content || []);
      }
    } catch (e) {
      console.error('Failed to fetch products:', e);
    }
  }, [searchQuery, selectedCategory]);

  // Fetch Categories
  const fetchCategories = useCallback(async () => {
    try {
      const res = await fetch('/api/products/categories');
      if (res.ok) {
        const data = await res.json();
        setCategories(data || []);
      }
    } catch (e) {
      console.error('Failed to fetch categories:', e);
    }
  }, []);

  // Fetch Orders
  const fetchOrders = useCallback(async () => {
    try {
      const headers: Record<string, string> = {};
      if (currentSession.token) {
        headers['Authorization'] = `Bearer ${currentSession.token}`;
      }

      // If customer, fetch my orders; if admin/inventory, fetch all
      const endpoint =
        currentSession.role === 'CUSTOMER' ? '/api/orders/my?size=20' : '/api/orders?size=20';

      const res = await fetch(endpoint, { headers });
      if (res.ok) {
        const data = await res.json();
        setOrders(data.content || []);
      }
    } catch (e) {
      console.error('Failed to fetch orders:', e);
    }
  }, [currentSession]);

  // Fetch Analytics
  const fetchAnalytics = useCallback(async () => {
    setIsAnalyticsLoading(true);
    try {
      const res = await fetch('/api/analytics/summary');
      if (res.ok) {
        const data = await res.json();
        setAnalytics(data);
      }
    } catch (e) {
      console.error('Failed to fetch analytics:', e);
    } finally {
      setIsAnalyticsLoading(false);
    }
  }, []);

  // Fetch Distributed Systems state
  const fetchDistributedState = useCallback(async () => {
    try {
      const [statusRes, outboxRes, dlqRes, auditRes] = await Promise.all([
        fetch('/api/system/status'),
        fetch('/api/system/outbox?limit=20'),
        fetch('/api/system/dlq'),
        fetch('/api/system/audit?page=0&size=15'),
      ]);

      if (statusRes.ok) setSystemStatus(await statusRes.json());
      if (outboxRes.ok) setOutboxEvents(await outboxRes.json());
      if (dlqRes.ok) setDlqEvents(await dlqRes.json());
      if (auditRes.ok) {
        const auditData = await auditRes.json();
        setAuditLogs(auditData.content || []);
      }
    } catch (e) {
      console.error('Failed to fetch distributed state:', e);
    }
  }, []);

  // Initial Load
  useEffect(() => {
    authenticateUser('customer@infosys.com');
    fetchProducts();
    fetchCategories();
    fetchAnalytics();
    fetchDistributedState();
  }, [authenticateUser, fetchCategories, fetchDistributedState]);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  // Cart operations
  const handleAddToCart = (product: Product, quantity: number) => {
    setCart((prev) => {
      const existing = prev.find((item) => item.product.sku === product.sku);
      if (existing) {
        return prev.map((item) =>
          item.product.sku === product.sku
            ? { ...item, quantity: Math.min(item.quantity + quantity, product.availableStock) }
            : item
        );
      }
      return [...prev, { product, quantity }];
    });
    setIsCartOpen(true);
  };

  const handleUpdateQuantity = (sku: string, quantity: number) => {
    if (quantity <= 0) {
      handleRemoveItem(sku);
      return;
    }
    setCart((prev) =>
      prev.map((item) => (item.product.sku === sku ? { ...item, quantity } : item))
    );
  };

  const handleRemoveItem = (sku: string) => {
    setCart((prev) => prev.filter((item) => item.product.sku !== sku));
  };

  // Submit Order (Idempotent)
  const handleSubmitOrder = async (idempotencyKey: string, shippingAddress: string): Promise<boolean> => {
    setIsSubmitting(true);
    try {
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        'Idempotency-Key': idempotencyKey,
      };
      if (currentSession.token) {
        headers['Authorization'] = `Bearer ${currentSession.token}`;
      }

      const body = {
        items: cart.map((item) => ({
          sku: item.product.sku,
          quantity: item.quantity,
          unitPrice: item.product.price,
          productTitle: item.product.title,
        })),
        shippingAddress,
        currency: 'USD',
      };

      const res = await fetch('/api/orders', {
        method: 'POST',
        headers,
        body: JSON.stringify(body),
      });

      if (!res.ok) {
        const errorData = await res.json().catch(() => ({}));
        throw new Error(errorData.message || `Server returned HTTP ${res.status}`);
      }

      setCart([]);
      await fetchProducts();
      await fetchOrders();
      await fetchAnalytics();
      await fetchDistributedState();
      setActiveTab('orders');
      return true;
    } finally {
      setIsSubmitting(false);
    }
  };

  // Cancel Order
  const handleCancelOrder = async (orderId: number) => {
    setIsCancelling(true);
    try {
      const headers: Record<string, string> = {};
      if (currentSession.token) {
        headers['Authorization'] = `Bearer ${currentSession.token}`;
      }

      const res = await fetch(`/api/orders/${orderId}/cancel`, {
        method: 'POST',
        headers,
      });

      if (res.ok) {
        await fetchProducts();
        await fetchOrders();
        await fetchAnalytics();
        await fetchDistributedState();
      } else {
        const err = await res.json().catch(() => ({}));
        alert(err.message || 'Failed to cancel order.');
      }
    } finally {
      setIsCancelling(false);
    }
  };

  // Trigger FakeStore Catalog Sync
  const handleTriggerSync = async () => {
    setIsSyncing(true);
    try {
      const res = await fetch('/api/products/sync', { method: 'POST' });
      if (res.ok) {
        await fetchProducts();
        await fetchCategories();
        await fetchDistributedState();
      }
    } finally {
      setIsSyncing(false);
    }
  };

  // Dispatch Outbox Publisher
  const handleDispatchOutbox = async () => {
    setIsDispatching(true);
    try {
      const res = await fetch('/api/system/outbox/dispatch', { method: 'POST' });
      if (res.ok) {
        await fetchOrders();
        await fetchAnalytics();
        await fetchDistributedState();
      }
    } finally {
      setIsDispatching(false);
    }
  };

  const cartTotalItems = cart.reduce((sum, item) => sum + item.quantity, 0);

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col font-sans">
      {/* Top Navbar & Role Switcher */}
      <Navbar
        currentSession={currentSession}
        onSwitchUser={authenticateUser}
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        cartCount={cartTotalItems}
        onOpenCart={() => setIsCartOpen(true)}
      />

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {activeTab === 'catalog' && (
          <CatalogView
            products={products}
            categories={categories}
            selectedCategory={selectedCategory}
            onSelectCategory={setSelectedCategory}
            searchQuery={searchQuery}
            onSearchChange={setSearchQuery}
            onAddToCart={handleAddToCart}
            onTriggerSync={handleTriggerSync}
            isSyncing={isSyncing}
          />
        )}

        {activeTab === 'orders' && (
          <OrdersView
            orders={orders}
            onCancelOrder={handleCancelOrder}
            isCancelling={isCancelling}
            onRefresh={fetchOrders}
          />
        )}

        {activeTab === 'analytics' && (
          <AnalyticsView
            analytics={analytics}
            onRefresh={fetchAnalytics}
            isLoading={isAnalyticsLoading}
          />
        )}

        {activeTab === 'distributed' && (
          <DistributedSystemsView
            outboxEvents={outboxEvents}
            dlqEvents={dlqEvents}
            systemStatus={systemStatus}
            auditLogs={auditLogs}
            onDispatchOutbox={handleDispatchOutbox}
            isDispatching={isDispatching}
            onRefresh={fetchDistributedState}
          />
        )}
      </main>

      {/* Idempotent Cart & Checkout Drawer */}
      <CartDrawer
        isOpen={isCartOpen}
        onClose={() => setIsCartOpen(false)}
        cart={cart}
        onUpdateQuantity={handleUpdateQuantity}
        onRemoveItem={handleRemoveItem}
        onSubmitOrder={handleSubmitOrder}
        isSubmitting={isSubmitting}
      />
    </div>
  );
};
