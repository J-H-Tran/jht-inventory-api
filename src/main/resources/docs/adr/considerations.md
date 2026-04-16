Correct — your query is currently **broken**.

You introduced a composite cursor:

```sql
(i.occurredAt, i.id) < (:lastSeenOccurredAt, :lastSeenId)
```

…but you never pass `lastSeenOccurredAt`. That makes the query:

* invalid logically
* incomplete at runtime (JPQL won’t bind it)

---

# 1. Core Issue

Your API only accepts:

```java
@RequestParam(required = false) UUID lastSeenId
```

But your query requires:

```text
(lastSeenOccurredAt, lastSeenId)
```

You are missing half the cursor.

---

# 2. Why this matters (important)

You switched to:

```text
ORDER BY occurredAt DESC, id DESC
```

This means:

> position in the dataset is defined by **(occurredAt, id)**

So your cursor must also be:

> (occurredAt, id)

Not just `id`.

---

# 3. Your current state (inconsistent)

| Component       | Uses            |
| --------------- | --------------- |
| Ordering        | occurredAt, id  |
| Cursor (API)    | id only         |
| Query condition | occurredAt + id |

→ This is internally inconsistent → pagination will break.

---

# 4. Two Correct Options

## Option A — Proper composite cursor (recommended)

### API change

```java
@RequestParam(required = false) UUID lastSeenId,
@RequestParam(required = false) OffsetDateTime lastSeenOccurredAt
```

---

### Repository fix

```java
@Query("""
    SELECT i FROM InventoryLedger i
    WHERE i.productId = :productId
    AND (
        :lastSeenId IS NULL OR
        (i.occurredAt < :lastSeenOccurredAt OR
         (i.occurredAt = :lastSeenOccurredAt AND i.id < :lastSeenId))
    )
    ORDER BY i.occurredAt DESC, i.id DESC
""")
```

Note:

* JPQL does **not reliably support tuple comparison** `(a, b) < (x, y)`
* You must expand it manually

---

### Response change

```java
public record MovementHistoryResponse(
    List<InventoryProductMovementResponse> content,
    boolean hasMore,
    UUID nextCursor,
    OffsetDateTime nextOccurredAt
) {}
```

---

### Cursor assignment

```java
var last = content.get(content.size() - 1);

UUID nextCursor = hasMore ? last.id() : null;
OffsetDateTime nextOccurredAt = hasMore ? last.occurredAt() : null;
```

---

## Option B — Simplify: use ID only (acceptable shortcut)

Only valid if:

* IDs are strictly ordered (e.g. UUIDv7, ULID)

Then:

### Query

```java
@Query("""
    SELECT i FROM InventoryLedger i
    WHERE i.productId = :productId
    AND (:lastSeenId IS NULL OR i.id < :lastSeenId)
    ORDER BY i.id DESC
""")
```

---

### Tradeoff

| Option           | Pros            | Cons                    |
| ---------------- | --------------- | ----------------------- |
| Composite cursor | correct, stable | more complex API        |
| ID-only          | simple          | unsafe with random UUID |

---

# 5. Subtle bug you still have

You fixed pagination mechanics, but missed:

```java
WHERE i.productId = :productId
```

This is **missing in your current query**.

Right now:
→ you are paginating across ALL products

---

# 6. Deeper design critique

You’re exposing raw cursor primitives:

```text
lastSeenId
lastSeenOccurredAt
```

This leaks implementation details.

Better:

```text
cursor=base64("occurredAt|id")
```

Then decode internally.

---

# 7. Mental Model Correction

Key rule:

> Cursor must fully represent ordering position

If ordering is:

```text
A DESC, B DESC
```

Cursor must include:

```text
(A, B)
```

Anything less = undefined behavior.

---

# 8. What you did well

* Switched to keyset pagination
* Fixed `limit + 1` pattern
* Correct `nextCursor` logic (almost)

---

# Final Pushback

Right now you are:

> implementing a pattern you partially understand

That’s dangerous because:

* it works in simple cases
* silently breaks under real load

You need to be able to answer:

* Why do we need a composite cursor?
* Why can’t ID alone work?
* What happens if timestamps collide?
* What guarantees ordering stability?

If you can’t answer those without hesitation, you’re still pattern-matching, not reasoning.

---

If you want next step:
I can give you a concurrency scenario where:

* same timestamp
* interleaved inserts

and make you predict exactly what your API returns.

## Let's continue with using raw cursor primitives and implement based on that. I fully understand that from a best practice and design I approach I would never release this in a real production environment. But hopefully it provides useful learning experience.

That’s a reasonable constraint for learning—as long as you stay disciplined about correctness.

Right now, if you commit to **raw cursor primitives**, you must fully commit to the rule:

> Cursor = complete ordering state

Since your ordering is:

```text
occurredAt DESC, id DESC
```

your cursor must be:

```text
(lastSeenOccurredAt, lastSeenId)
```

Anything less will produce subtle bugs.

---

# 1. Minimal Correct Implementation (Raw Cursor Version)

## Controller

```java
@GetMapping("/movements")
public ResponseEntity<MovementHistoryResponse> inventoryMovementHistory(
        @RequestParam("productId") UUID id,
        @RequestParam(required = false) UUID lastSeenId,
        @RequestParam(required = false) OffsetDateTime lastSeenOccurredAt,
        @RequestParam(defaultValue = "20") @Max(100) int limit
) {
    return ResponseEntity.ok(
            inventoryService.reportInventoryMovementHistory(
                    id, lastSeenId, lastSeenOccurredAt, limit
            )
    );
}
```

---

## Service

```java
@Transactional
public MovementHistoryResponse reportInventoryMovementHistory(
        UUID productId,
        UUID lastSeenId,
        OffsetDateTime lastSeenOccurredAt,
        int limit
) {
    if (!productRepository.existsById(productId)) {
        throw new ResourceNotFoundException("Product", productId.toString());
    }

    var entries = ledgerRepository.findByProductIdAfter(
            productId,
            lastSeenId,
            lastSeenOccurredAt,
            PageRequest.of(0, limit + 1)
    ).stream()
     .map(InventoryProductMovementResponse::from)
     .toList();

    boolean hasMore = entries.size() > limit;

    var content = hasMore
            ? entries.subList(0, limit)
            : entries;

    UUID nextCursor = null;
    OffsetDateTime nextOccurredAt = null;

    if (hasMore) {
        var last = content.get(content.size() - 1);
        nextCursor = last.id();
        nextOccurredAt = last.occurredAt();
    }

    return new MovementHistoryResponse(content, hasMore, nextCursor, nextOccurredAt);
}
```

---

## Repository (JPQL-safe version)

You cannot rely on tuple comparison. Expand it:

```java
@Query("""
    SELECT i FROM InventoryLedger i
    WHERE i.productId = :productId
    AND (
        :lastSeenId IS NULL OR
        i.occurredAt < :lastSeenOccurredAt OR
        (
            i.occurredAt = :lastSeenOccurredAt
            AND i.id < :lastSeenId
        )
    )
    ORDER BY i.occurredAt DESC, i.id DESC
""")
List<InventoryLedger> findByProductIdAfter(
        @Param("productId") UUID productId,
        @Param("lastSeenId") UUID lastSeenId,
        @Param("lastSeenOccurredAt") OffsetDateTime lastSeenOccurredAt,
        Pageable pageable
);
```

---

## Response DTO

```java
public record MovementHistoryResponse(
        List<InventoryProductMovementResponse> content,
        boolean hasMore,
        UUID nextCursor,
        OffsetDateTime nextOccurredAt
) {}
```

---

# 2. How the Client Uses It

### First request:

```http
GET /movements?productId=123
```

---

### Response:

```json
{
  "content": [...],
  "hasMore": true,
  "nextCursor": "uuid-1",
  "nextOccurredAt": "2026-04-16T10:15:30Z"
}
```

---

### Next request:

```http
GET /movements?productId=123
    &lastSeenId=uuid-1
    &lastSeenOccurredAt=2026-04-16T10:15:30Z
```

---

# 3. Subtle Edge Cases (this is where learning happens)

## Case 1 — Same timestamp

Multiple rows:

```text
occurredAt = same
```

Your tie-breaker:

```text
id DESC
```

Your condition:

```sql
occurredAt < X
OR (occurredAt = X AND id < Y)
```

This guarantees:

* no duplicates
* no skips

---

## Case 2 — New inserts during pagination

New row inserted with:

```text
occurredAt > current page
```

→ appears only in future “first page” calls
→ does NOT corrupt current pagination

This is exactly what you want.

---

## Case 3 — Missing half of cursor

If client sends:

```text
lastSeenId != null
lastSeenOccurredAt = null
```

Your query becomes unsafe.

### You should enforce:

```java
if (lastSeenId != null && lastSeenOccurredAt == null) {
    throw new IllegalArgumentException("Both cursor fields must be provided");
}
```

---

# 4. Critical DB Index (you need this)

Without this, your query degrades fast:

```sql
CREATE INDEX idx_ledger_product_occurred_id
ON inventory_ledger (product_id, occurred_at DESC, id DESC);
```

---

# 5. Mental Model (what you should internalize)

You are implementing:

> “seek-based pagination over a sorted dataset”

Core invariant:

```text
(next page) = all rows strictly AFTER last seen row in ordering
```

Translated into SQL:

```text
(A, B) < (cursorA, cursorB)
```

---

# 6. Where you’re still slightly naive (pushback)

You said this is for learning—but even for learning, be careful:

* You’re exposing internal sort keys directly
* You’re trusting client to pass consistent cursor state
* You’re not validating cursor integrity

That’s fine temporarily—but recognize:

> This is a **leaky abstraction**, not just a “non-production” shortcut

---

# Final Check

If you can clearly explain:

* why `(occurredAt, id)` is required
* why `<` (not `>`) is used with DESC ordering
* why `limit + 1` is necessary
* how duplicates are prevented

Then you’re no longer copying the pattern—you understand it.

---

If you want to push further:
Next step is removing `occurredAt` entirely and designing a system that guarantees ordering via IDs alone (UUIDv7 or sequence-based). That’s where real-world systems simplify this problem.