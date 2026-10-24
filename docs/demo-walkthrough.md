# Interactive Demo & Verification Walkthrough

Follow these steps to exercise the full end-to-end user journey in your browser at `http://localhost:8080/`.

---

## 1. Persona Identity Switching

1. Open `http://localhost:8080/`.
2. Observe the top navigation bar with the **Role** identity switcher.
3. Switch between:
   - **`customer@infosys.com`:** Default shopper persona.
   - **`admin@infosys.com`:** Operations administrator with complete platform oversight.
   - **`inventory@infosys.com`:** Fulfillment manager focused on SKU stock levels and restocks.

---

## 2. Product Discovery & Live API Sync

1. On the **Catalog & Stock** tab, search for items by keyword (e.g. "backpack", "SSD", "jacket") or filter by category pills.
2. Observe provenance badges:
   - `● LIVE API`: Items fetched directly from the live public FakeStore API.
   - `⚡ CACHED`: Default fallback items ensuring continuous operation if external network is unavailable.
3. Click **"Sync Live FakeStore API"** to trigger an on-demand catalog ingestion cycle.
4. Locate **`SKU-HOT-ITEM-99`** ("Enterprise High-Demand UltraBook Pro 16") with limited initial stock (5 units) designed for race condition testing.

---

## 3. Idempotent Checkout & Pessimistic Reservation

1. Select items from the catalog and add them to the cart.
2. Click the cart icon in the top right header to open the **Cart & Idempotent Checkout** drawer.
3. Note the pre-populated client-generated **`Idempotency-Key`** UUID header.
4. Click **"Place Order (Transactional Outbox)"**.
5. Observe the order confirmation: inventory is atomically reserved under `SELECT FOR UPDATE` and an outbox event is created.
6. **Idempotency Replay Test:** Re-submit the identical request with the same `Idempotency-Key`. Notice that the application immediately returns the cached order response without deducting additional stock or creating duplicate order records.

---

## 4. Real-Time Order Lifecycle & Customer Cancellation

1. Navigate to the **Order Tracking** tab.
2. Observe the visual state stepper showing the lifecycle progress:
   - `1. Order Created` $\rightarrow$ `2. Stock Reserved` $\rightarrow$ `3. Payment Authorized` $\rightarrow$ `4. Completed`.
3. Locate an active order and click **"Cancel & Release Stock"**.
4. Confirm that the order status immediately transitions to `CANCELLED` and the reserved inventory is restored to the available stock pool without page reload lag.

---

## 5. Operational Stream Analytics & Outbox DLQ

1. Navigate to the **Analytics Stream** tab:
   - Review Gross Merchandise Value (GMV) of settled orders.
   - Observe the **Functional Stream Category Demand** progress meters computed dynamically via Java 21 Streams (`groupingBy` and Map-Reduce).
   - Review real-time SKU stock levels.
2. Navigate to the **Outbox & DLQ** tab:
   - Review recent events in the Transactional Outbox log (`ORDER_CREATED`, `ORDER_CANCELLED`).
   - Click **"Dispatch Outbox Now"** to trigger event publishing.
   - Inspect the **Dead Letter Queue (DLQ)** to observe poisoned or declined transactions routed safely without blocking subsequent event streams.
   - Review the **Immutable Audit Trail** table verifying correlation IDs.
