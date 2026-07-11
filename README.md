# CollegeFestEats

College fest food-ordering desktop app — Java Swing + MongoDB Atlas.

## Run

```
mvn exec:java          # launches the app (splash -> login)
mvn test               # runs the 31-test suite (headless-safe)
```

Demo accounts: `STU001 / student123` (student), `VEN001 / vendor123`
(vendor) — or create new accounts from the login screen.
Do **not** run `SeedData` again; the demo users already exist in Atlas.

## What's inside

- **Login / Create account** — regex-validated usernames, BCrypt-hashed
  passwords, role-based routing (student vs vendor dashboards).
- **Student dashboard** — "Home" tab with live active orders (5-second
  SwingWorker polling), rush-aware ETA banner, place / cancel orders,
  order history with CSV export.
- **Place Order dialog** — live vendor list and per-vendor menus from
  MongoDB, +/- quantity steppers, running **Total ₹** amount, and
  out-of-stock items shown greyed-out (visible, never orderable).
- **Vendor dashboard** — live incoming-order table (polling + Observer
  push), forward-only status buttons, "Most Ordered Today" (HashMap +
  PriorityQueue), CSV export, Add-Menu-Item form, and a **My Menu** panel
  with per-item in-stock/out-of-stock toggles, plus Logout.
- Both dashboards share one charcoal + amber theme (`AppTheme`) and show
  the **same short order id** (`#A1B2C3`) for the same order.

## Architecture, patterns & the merge story

See `INTEGRATION_NOTES.md` — DAO / Service / GUI layering, Observer,
Factory, Singleton, Producer-Consumer (LinkedBlockingQueue +
ExecutorService), ConcurrentHashMap sessions, and the full changelog of
the integration and feature rounds.
