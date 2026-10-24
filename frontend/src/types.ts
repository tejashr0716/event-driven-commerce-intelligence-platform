export type UserRole = 'CUSTOMER' | 'ADMIN' | 'INVENTORY_MANAGER';

export type DataSourceMode = 'LIVE_EXTERNAL_DATA' | 'CACHED_EXTERNAL_DATA' | 'SEEDED_DEMO_DATA';

export type OrderStatus = 'PENDING' | 'INVENTORY_RESERVED' | 'PAYMENT_AUTHORIZED' | 'COMPLETED' | 'CANCELLED' | 'FAILED';

export interface Product {
  id: number;
  sku: string;
  title: string;
  description: string;
  category: string;
  price: number;
  currency: string;
  imageUrl: string;
  sourceOrigin: DataSourceMode;
  availableStock: number;
}

export interface CartItem {
  product: Product;
  quantity: number;
}

export interface OrderItemDto {
  sku: string;
  quantity: number;
  unitPrice: number;
  productTitle: string;
}

export interface Order {
  id: number;
  userId: number;
  customerEmail: string;
  idempotencyKey: string;
  status: OrderStatus;
  totalAmount: number;
  currency: string;
  shippingAddress: string;
  failureReason?: string;
  items: OrderItemDto[];
  createdAt: string;
  updatedAt: string;
}

export interface UserSession {
  userId: number;
  email: string;
  fullName: string;
  role: UserRole;
  token: string;
}

export interface AnalyticsSummary {
  totalOrders: number;
  completedOrders: number;
  cancelledOrders: number;
  failedOrders: number;
  grossMerchandiseValue: number;
  reservationFailureRatePercent: number;
  cancellationRatePercent: number;
  pendingOutboxEvents: number;
  categoryDemand: Record<string, number>;
  currentStockLevels: Record<string, number>;
}

export interface SystemStatus {
  serviceName: string;
  status: string;
  environment: string;
  catalogSource: string;
  pendingOutboxCount: number;
  adapters: Record<string, string>;
}

export interface OutboxEvent {
  id: number;
  aggregateType: string;
  aggregateId: string;
  eventType: string;
  payloadJson: string;
  status: string;
  retryCount: number;
  errorMessage?: string;
  createdAt: string;
  processedAt?: string;
}

export interface AuditLogItem {
  id: number;
  correlationId: string;
  actorId: string;
  actorName: string;
  actionType: string;
  entityId: string;
  status: string;
  detailsJson: string;
  timestamp: string;
}
