# INTEGRATION NOTES — how the final app was assembled (read me before the viva)

Final app = feature/auth (foundation, exceptions, EtaEngine, exporters)
+ feature/orders (OrderService, Factory, Observer, ItemDAO, VendorDashboard)
+ feature/frontend (AppTheme, StudentDashboardFrame, dialogs, poller, badges)
+ new glue written during integration. Verified by 29 automated tests
(login/regex/routing, ETA math, forward-only status, cancel rules,
Producer-Consumer persistence, Observer notifications, CSV export).

## Who is on screen
- Login: Person A's styled LoginFrame (regex -> SwingWorker -> BCrypt).
- Student: Person C's StudentDashboardFrame, now LIVE via MongoOrderDataSource.
- Vendor: Person B's VendorDashboard (Observer + PriorityQueue popular items
  + CountDownLatch preload + menu upload). C's VendorDashboardFrame remains
  in the tree as an alternate; it compiles against the same interface.

## New glue files
- gui/MongoOrderDataSource.java — real implementation of C's OrderDataSource
  interface, backed by OrderService/DAOs. Translates statuses between the
  DB convention (PLACED/PREPARING/READY/SERVED) and C's UI convention
  (pending/preparing/ready/served), maps vendor names <-> ids, waits for the
  consumer thread to persist before returning the new order id.
- Main.java (global crash handler -> error-log.txt + splash), SeedItems.java
  (idempotent menu seeding), io/VendorOrderExporter.java (static, so B's
  existing call compiles).

## Real bugs found & fixed in existing code (tell the professor - it's good!)
1. OrderDAO.insertOrder never copied MongoDB's generated _id back into the
   Order -> every placed order had a null id; cancel/status-update crashed.
   Fix: order.setId(doc.getObjectId("_id").toHexString()) after insertOne.
2. OrderDAO.countActiveOrders counted lowercase "pending"/"preparing" while
   OrderService writes "PLACED"/"PREPARING" -> ETA queue was always 0.
   Fix: count Order.STATUS_PLACED / STATUS_PREPARING (constants added).
3. OrderFactory set placedAt to LocalDateTime.now().toString() but the model
   field is java.util.Date -> compile error. Fix: new Date().
4. OrderService called EtaEngine.calculateEta STATICALLY; it's an instance
   method. Fix: new EtaEngine().calculateEta(...).
5. VendorDashboard formatted placedAt with String.substring on a Date ->
   compile error. Fix: SimpleDateFormat("HH:mm:ss").
6. ItemDAO called Database.getDatabase(); the accessor is getDb().
7. VendorDashboard removed its observer in windowClosing, which never fires
   on dispose() -> ghost observers kept receiving updates. Fix: windowClosed.
8. Consumer thread was non-daemon (app never exited after closing windows)
   and one RuntimeException would kill it silently forever. Fix: daemon
   thread factory + try/catch inside the loop.
9. C's files had `package main.java.com.collegefest.gui;` -> fixed to
   `com.collegefest.gui`. Her PlaceOrderDialog's hardcoded vendor list now
   loads real vendor names from MongoOrderDataSource's cache.
10. Order.java gained the STATUS_* constants; use them everywhere.

## Run order on a fresh machine
JDK 17 + Eclipse -> Import Existing Maven Project -> run SeedItems.java ONCE
-> run Main.java -> STU001/student123 or VEN001/vendor123. Run Main twice
for the two-window demo (student places order -> vendor sees it -> vendor
advances status -> student's table updates within 5 s via polling).

## UPDATE 2 — Self-service accounts + per-vendor menus (verified: 40/40 tests)

New files:
- service/AccountService.java — signup rules in one testable place:
  username regex ^[A-Za-z0-9_]{3,20}$, password >= 6 chars (stored ONLY as
  BCrypt hash), display/stall name required, username unique, stall name
  unique (students order BY stall name). Students and vendors both live in
  the "users" collection separated by the role field — that role is what
  routes each login to the right dashboard.
- gui/CreateAccountDialog.java — the two-step flow from the mock-ups:
  chooser ("Are you a student or a vendor?") then the role's form (vendors
  give their STALL NAME first — it goes into User.name, which is exactly
  what students see in the ordering dropdown, so new stalls are orderable
  immediately). All DB work on SwingWorkers; on success the dialog closes
  back to the login page with the new username pre-filled.

Changed files:
- gui/LoginFrame.java — new "Create account" button + hint pill text; the
  old STU###/VEN### regex is now the username regex (STU001/VEN001 still
  match, so seeded demo accounts keep working). Routing was ALWAYS by the
  role field, so free-form usernames route correctly automatically.
- gui/PlaceOrderDialog.java — fully rewritten: vendor dropdown loads live
  from MongoDB; CHANGING VENDOR RELOADS THAT VENDOR'S OWN MENU (old bug:
  one hardcoded list for everyone); every item row has a [-] 0 [+]
  quantity stepper starting at 0; Place order enables at total >= 1;
  quantity N is encoded as N copies of the item name (what ETA averaging
  and popularity ranking already expect).
- gui/MongoOrderDataSource.java — new helpers menuForVendorName() and
  freshVendorNames() (background-thread only).

New tests (TC28-TC38): signup stores role + real BCrypt hash; stall
appears in findAllVendors; duplicate username / short password / duplicate
stall all rejected with clear messages; NEW student routes to
StudentDashboardFrame and NEW vendor to VendorDashboard by ROLE; menus
differ per vendor (5 vs 0), vendor-added item appears only in his menu;
an order placed by STALL NAME lands in exactly that vendor's queue with
the right quantities.

---

## Feature round — July 11

All six requested features, plus the connectivity audit and a fresh test pass.

### 1. Order total in the Place Order window
`PlaceOrderDialog` now keeps an item-name → price map alongside the quantity
map and shows a live amber **"Total: ₹X"** next to the Cancel / Place order
buttons. It updates on every +/- click (Σ price × quantity) and shows paise
only when needed (₹199 vs ₹199.50).

### 2. One consistent look (charcoal + amber) across every screen
- `VendorDashboard` repainted from indigo/navy to the shared `AppTheme`
  palette (charcoal surfaces, amber accents) — layout and features kept.
- `StudentDashboardFrame` restructured to MIRROR the vendor dashboard:
  same top bar (logo · "CollegeFest Eats" · "Student Portal · name (id)" ·
  Logout), same bottom status bar, same table styling and row height.
- `LoginFrame`, `CreateAccountDialog` and the `Main` splash re-paletted to
  the same charcoal/amber (field names and error texts untouched).

### 3. Vendor menu management with Out-of-Stock toggles  (NEW section)
Sidebar of the vendor dashboard, directly below "Add Menu Item":
**"My Menu (tick = in stock)"** — every item the logged-in vendor has ever
added, loaded live from MongoDB (`ItemDAO.findByVendor`), one checkbox per
row. Untick → `ItemDAO.updateAvailability(id,false)`; tick → back on sale.
Because the full list is always visible, an out-of-stock item is never
lost — the vendor can re-enable it any time. Newly added items appear in
the list immediately.

Student side of the same feature: `PlaceOrderDialog` now shows
out-of-stock items greyed-out with a red **OUT OF STOCK** tag and no +/-
stepper, so they are visible but can never be added. As a safety net,
`MongoOrderDataSource.placeOrder()` re-checks availability at order time —
if an item sells out while the dialog is open, the student gets a clear
message instead of a ghost order.

### 4. Logout on the vendor dashboard
Top-bar Logout button, exact mirror of the student side: clears the
session from OrderService's ConcurrentHashMap, disposes the window
(observer + poll timer cleaned up via windowClosed) and returns to
LoginFrame.

### 5. Same order ID on both dashboards
New shared helpers: `AppTheme.shortId()` (one format: `#A1B2C3`, last six
characters, uppercase) and `ShortIdRenderer`. Both dashboards keep the
FULL MongoDB `_id` in the table model (so status changes and cancels always
hit the right document) and render it short — hover shows the full id.
The ETA banner and the "order placed" dialog use the same format. This
also fixed a real bug: the old vendor dashboard resolved the clicked order
by ROW NUMBER against a fresh fetch, which could mark the WRONG order if
anything changed between polls — it now reads the full id from the model.

### 6. "Active Orders" tab renamed "Home" on the student dashboard.

### Connectivity audit ("no loose connections")
- Vendor dashboard now POLLS MongoDB every 5 s through the same
  OrderPoller + MongoOrderDataSource pipeline the student side uses (the
  Observer push only works inside one running copy of the app; polling
  makes orders placed from a second machine appear too). Observer kept
  for instant same-JVM updates, Refresh button kept.
- Vendor poll key is the real `vendorId` everywhere (old name-as-key path
  is gone with the deleted legacy frame).
- Shared `SimpleDateFormat` in MongoOrderDataSource was reachable from
  three poller threads at once — SimpleDateFormat is not thread-safe, so
  formatting now goes through a synchronized helper.
- Removed the hardcoded "Rajesh Food Stall" fallback vendor: a cold cache
  with the DB unreachable now yields an empty list and a friendly message,
  never a ghost vendor.
- Observer race guarded: OrderService can notify while the vendor window
  is still constructing (observer registers before polling starts).
- pom.xml: jar mainClass pointed at a class that doesn't exist
  (`MainServer`) — fixed to `com.collegefest.Main`; added JUnit 5 +
  Surefire 3 (headless) + exec-maven-plugin (`mvn exec:java`).
- Deleted `VendorDashboardFrame.java` — a leftover pre-merge vendor screen
  (dummy in-memory menu, vendor NAME as poll key, logout that didn't clear
  the session). LoginFrame never routed to it; keeping two vendor
  dashboards invited exactly the kind of drift this round was fixing.
- `AddMenuItemDialog` kept but marked as currently unused (the live vendor
  sidebar form replaced it).

### Tests — 31/31 passing
`src/test/java` restored with 31 JUnit 5 tests: models (6), exceptions
(4), OrderFactory (3), EtaEngine formula via a new DB-free `computeEta`
seam (4), OrderService transition rules + sessions via a package-private
static `isValidTransition` (5), SimulatedOrderDataSource contract (4),
AppTheme colours + shortId (3), OrderRow (1), CSV exporter (1).
Run with `mvn test` (headless-safe; no test opens a window or a DB
connection).

## UPDATE 3 — Vendor-first student home, menu windows, cancelled visibility (46/46 tests)

Student dashboard restructured into THREE tabs (AppTheme.styleTabs makes
them readable on the dark palette):
- "Vendors" (the new home): a "🔥 Popular Stalls" bar (vendors ranked by
  non-cancelled order count — HashMap + PriorityQueue in
  MongoOrderDataSource.popularStallNames) above a live list of every
  stall; each row's "View Menu →" opens that stall's VendorMenuFrame.
- "Active Orders": the existing live table + Cancel button (ordering
  button removed — ordering now starts from the Vendors tab).
- "Order History": unchanged; includes CANCELLED orders (red badge).

New windows (both fully themed):
- gui/VendorMenuFrame.java (student-facing): one row per item with price,
  LIVE per-item ETA (adapter.etaFor -> the real EtaEngine against the
  stall's current queue), and [-] 0 [+] steppers; out-of-stock items are
  listed greyed with no steppers (visible but unorderable — the adapter
  also refuses them server-side); bottom-left running Total ₹, bottom-right
  Place order; on success jumps the dashboard to Active Orders.
- gui/VendorMenuManagerFrame.java (vendor-facing): opened from the new
  "🍔 My Menu" top-bar button; Add Menu Item form (left) + full item list
  with In-stock toggles (right). The old sidebar was removed from the
  vendor main window, which is now orders-only.

Adapter changes (MongoOrderDataSource): fetchIncomingOrdersForVendor no
longer filters CANCELLED — vendors now SEE student cancellations (red
badge; status buttons still refuse to touch them); new etaFor() and
popularStallNames() helpers (background-thread only).

New tests TC39-TC43b: vendor sees cancelled; history includes cancelled;
popularity ranking; etaFor == EtaEngine; out-of-stock listed but ordering
refused. PlaceOrderDialog remains in the tree as a legacy/reference class
(no longer used by any screen).
