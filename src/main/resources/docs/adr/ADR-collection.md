# Architecture Decision Records

> Inventory API — Java 21 · Spring Boot 3.x · PostgreSQL  
> Each ADR captures a decision made during development, the context that drove it, the tradeoffs accepted, and the alternatives considered.

---

## ADR-001 — Ledger Over Direct Quantity Mutation

**Status:** Accepted  
**Date:** Week 1

### Context
Inventory quantity must be tracked reliably across concurrent writes, with full history available for audit and reconciliation. The simplest implementation would store a `quantity` column on the product and update it in place.

### Decision
All inventory changes are recorded as immutable rows in `inventory_ledger`. Current stock level is derived by `SELECT SUM(quantity_delta) FROM inventory_ledger WHERE product_id = ?`. No quantity column exists on `products`.

### Consequences
- Complete audit trail — every change has a who, what, when, and why
- State is replayable — the ledger can be summed at any point in time to reconstruct historical stock levels
- Reconciliation is possible — a drift detector can compare the ledger sum against any cached balance
- Write path is append-only — no UPDATE contention on a shared quantity column
- Read cost increases as the ledger grows — mitigated in Week 5 by introducing a balance table only after benchmarking justifies it

### Alternatives Considered
**Direct quantity mutation (`UPDATE products SET quantity = quantity - ?`):** Rejected. Loses history on every write, making bugs irreversible and audits impossible. Race conditions under concurrent writes require locking that the ledger pattern avoids structurally.

**Full event sourcing:** Considered but rejected as over-engineered for this scope. The ledger pattern provides the core benefits of event sourcing — immutability, replayability, auditability — without the infrastructure complexity of an event store and projection rebuilds.

---

## ADR-002 — Atomic Write via CTE Over Application-Level Locking

**Status:** Accepted  
**Date:** Week 2

### Context
Every SHIP movement must check that sufficient stock exists before inserting the ledger row. A naive two-step approach — read current stock in Java, then insert if sufficient — creates a race condition:

```
Thread A: reads stock = 10, passes check
Thread B: reads stock = 10, passes check
Thread A: inserts -10 → stock = 0
Thread B: inserts -10 → stock = -10  ← invariant violated
```

### Decision
The stock check and ledger insert are combined into a single PostgreSQL CTE executed in one round-trip:

```sql
WITH stock_check AS (
    SELECT coalesce(sum(quantity_delta), 0) + :quantityDelta AS projected
    FROM inventory_ledger
    WHERE product_id = :productId
)
INSERT INTO inventory_ledger (...)
SELECT ... FROM stock_check WHERE projected >= 0
RETURNING *
```

If projected stock is negative the `WHERE` clause prevents the insert and the CTE returns no rows. An empty result set signals `InsufficientStockException` to the caller.

### Consequences
- Stock check and insert are atomic within a single PostgreSQL transaction — no window for concurrent requests to interleave
- One DB round-trip instead of two — lower latency under load
- The CTE has a single responsibility: enforce the non-negative stock invariant at write time
- Empty result set is overloaded — it means either insufficient stock or a concurrent failure; the service layer must distinguish these via prior validation

### Alternatives Considered
**Pessimistic locking (`SELECT FOR UPDATE`):** Would serialize access to the product's ledger rows, preventing the race condition. Rejected because it holds locks for the duration of the transaction, reducing throughput under concurrent load and risking deadlocks.

**Optimistic locking (`@Version`):** Appropriate for low-contention entities. Rejected here because the ledger has no single row to version — stock level is an aggregate. Applying optimistic locking would require a separate balance row, which reintroduces the two-table update problem.

**Application-level distributed lock (Redis):** Rejected as premature infrastructure complexity. The DB is already the authority on consistency — solving this at the DB layer is simpler and more reliable.

---

## ADR-003 — Service-Layer Validation Before CTE Execution

**Status:** Accepted  
**Date:** Week 2

### Context
The CTE in ADR-002 can fail for multiple reasons — insufficient stock, inactive product, non-existent product. An empty CTE result set does not communicate which condition failed. The service layer needs to throw typed exceptions with meaningful messages.

### Decision
Product existence and active status are validated in the service layer before the CTE executes. The CTE's only remaining responsibility is the atomic stock check and insert. Failure reasons are surfaced as typed exceptions before any write is attempted.

```
1. Load product           → ResourceNotFoundException if absent
2. Check active status    → InactiveProductException if inactive
3. Validate business rules → BadRequestException for rule violations
4. Execute CTE            → InsufficientStockException if returns empty
```

### Consequences
- Each failure mode maps to a distinct, typed exception with a meaningful message
- The CTE has one job — enforce the non-negative stock invariant
- One additional DB read per write request (product load)
- At the throughput this API will realistically handle, the extra round-trip is immeasurable

### Alternatives Considered
**Full DB-layer validation inside the CTE:** The CTE could join against `products` to check existence and active status in one query. Rejected because an empty result set would not distinguish between "product not found", "product inactive", and "insufficient stock" — forcing the service to run follow-up queries anyway to determine the cause, losing the round-trip benefit.

---

## ADR-004 — Idempotency Enforced at DB Layer

**Status:** Accepted  
**Date:** Week 2

### Context
Network timeouts and client retries mean the same movement request can arrive more than once. Without idempotency protection, a retry inserts a duplicate ledger row and corrupts the stock level.

### Decision
`inventory_ledger.idempotency_key` has a `UNIQUE` constraint. The client supplies an `Idempotency-Key` request header. The DB constraint is the enforcement mechanism — a duplicate key causes a `DataIntegrityViolationException` which the global exception handler translates to `409 Conflict`.

No application-level check for duplicate keys is performed before the insert. The constraint fires instead.

### Consequences
- Idempotency is enforced even under concurrent retries — no race condition window between "check if key exists" and "insert"
- No separate idempotency keys table required for deduplication
- Duplicate requests return `409` rather than the original `201` response — a client cannot distinguish a retry success from a conflict without storing the original response
- Full idempotency (returning the original response on retry) requires a separate response cache table, deferred as a future enhancement

### Alternatives Considered
**Application-level pre-insert check:** Query for the key before inserting. Rejected — creates the same race condition as the two-step stock check. Two concurrent retries can both pass the check before either inserts.

**Separate idempotency keys table with response cache:** Stores the response body alongside the key, enabling `200 OK` with the original response on retry. Deferred — adds write complexity and a TTL management concern. The `409` approach is honest and safe for current scope.

---

## ADR-005 — Idempotency Key as Request Header

**Status:** Accepted  
**Date:** Week 2

### Context
The idempotency key must travel from the client to the server with each request. It can be placed in the request body or in a request header.

### Decision
`Idempotency-Key` is a required request header, not a body field.

### Consequences
- Separates infrastructure metadata (retry safety) from business data (what movement to record)
- Consistent with industry convention — Stripe, PayPay, and most payment APIs use the header pattern
- The key is validated independently of body validation, producing a clear error if absent
- Clients must handle header injection, which is standard in HTTP client libraries

### Alternatives Considered
**Body field:** Including `idempotencyKey` in the request body works technically. Rejected because it conflates transport-layer retry semantics with business payload. A movement's meaning should not change because it carries a retry key.

---

## ADR-006 — Controlled Vocabulary for Adjustment Reasons as a DB Table

**Status:** Accepted  
**Date:** Week 1

### Context
ADJUST movements require a mandatory reason code. The valid set of reason codes must be enforced. Two options: a Java enum or a DB table with a FK constraint.

### Decision
Reason codes are stored in `adjustment_reasons` as a DB table. `inventory_ledger.reason_code` is a FK referencing that table. Valid codes are seeded via a Flyway migration.

### Consequences
- New reason codes can be added via a Flyway migration without a code deployment
- The FK constraint enforces valid values at the DB layer, surviving application bugs and direct DB writes
- Adding a code requires a migration rather than a code change — operationally simpler for a running system
- Application code references reason codes as plain `String` constants rather than enum values

### Alternatives Considered
**Java enum:** Compile-time safety for known codes. Rejected because it locks the valid set into the codebase — adding a new reason code requires a code change, recompile, and redeployment. The DB table and enum would need manual synchronisation and will drift over time.

---

## ADR-007 — Package-by-Feature Modulith Structure

**Status:** Accepted  
**Date:** Week 1

### Context
The application must be structured for maintainability and future extractability into separate services without the overhead of microservices during the learning phase.

### Decision
Code is organised by feature module (`catalog`, `movements`, `stock`, `shared`) not by layer (`controller`, `service`, `repository`). Modules communicate via domain events or through `shared`. No cross-module repository access is permitted.

### Consequences
- Module boundaries are visible in the package structure from day one
- Each module owns its entity, repository, service, and API surface
- Future extraction into a separate service requires replacing in-process calls with HTTP/gRPC — the interface boundary already exists
- `shared` is a dependency of all modules — changes to `shared` have wide impact and should be made carefully

### Alternatives Considered
**Layer-by-layer structure (`controller/`, `service/`, `repository/`):** Familiar but couples all features at every layer. A change to inventory logic touches files across three packages. Rejected because it produces a "big ball of mud" that resists decomposition.

**Microservices from the start:** Clean separation but introduces distributed transaction complexity, network overhead, and infrastructure burden on a solo learning project. Rejected — the modulith gives the same boundary discipline at zero infrastructure cost.

---

## ADR-008 — Maven Over Gradle

**Status:** Accepted  
**Date:** Week 1

### Context
Build tooling choice for a solo Java learning project. Both Maven and Gradle are viable. The project requires no custom build logic.

### Decision
Maven with `pom.xml`. Spring Boot parent POM manages dependency versions.

### Consequences
- Every Spring Boot guide, Stack Overflow answer, and official documentation defaults to Maven — reduces friction when debugging dependency issues
- XML verbosity is a feature for a learning project — explicit about what is included and why
- Scope declarations (`runtime`, `optional`, `test`) are enforced visibly
- No Groovy or Kotlin DSL context switching within the project

### Alternatives Considered
**Gradle (Groovy DSL):** More concise. Familiar from prior Kotlin work. Rejected because Groovy DSL is loosely typed — build file errors surface at runtime not at edit time. For a project focused on Java depth, not build tooling fluency, Maven reduces friction.

**Gradle (Kotlin DSL):** Type-safe. Rejected because it introduces Kotlin syntax into a Java-only project, adding unnecessary context switching.