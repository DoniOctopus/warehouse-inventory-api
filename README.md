# Warehouse Inventory API

A RESTful backend for managing a shop's warehouse inventory: **items**, their **variants**
(size, colour, …), **pricing**, **stock levels**, and **orders** that sell stock without
ever overselling.

Built with Java 17, Spring Boot 3.5, Spring Data JPA, Flyway and H2.

---

## Contents

- [How to run](#how-to-run)
- [Domain model](#domain-model)
- [API overview](#api-overview)
- [API examples](#api-examples)
- [Design decisions](#design-decisions)
- [Assumptions](#assumptions)
- [Possible next steps](#possible-next-steps)

---

## How to run

**Requirements:** JDK 17 or newer. Maven is optional; the Maven wrapper is included.

```bash
# run the tests
./mvnw test            # Windows: mvnw.cmd test

# start the application on http://localhost:8080
./mvnw spring-boot:run # Windows: mvnw.cmd spring-boot:run
```

Or build and run the jar:

```bash
./mvnw package
java -jar target/warehouse-inventory-api-0.0.1-SNAPSHOT.jar
```

Once it is running:

| What | URL |
|---|---|
| Swagger UI (interactive docs) | http://localhost:8080/swagger-ui.html |
| OpenAPI spec | http://localhost:8080/v3/api-docs |
| H2 console | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:file:./data/warehouse`, user `sa`, empty password) |

Ready-to-run requests are in [`requests.http`](requests.http). They can be run directly from IntelliJ IDEA
or VS Code (REST Client extension).

### Data and sample data

- Data is stored in a **file-based H2 database** under `./data/`, so it **survives restarts**.
  Delete the `data/` folder to start from scratch.
- On the first start, Flyway loads **sample data**: 4 items and 10 variants, some low or out of stock,
  so the API can be explored right away. To start with an empty catalog, remove `classpath:db/sample`
  from `spring.flyway.locations` in `application.yml`.
- Tests use a separate in-memory database without sample data.

---

## Domain model

```
Item 1 ──── * Variant 1 ──── * StockMovement      Order 1 ──── * OrderLine
 name          sku (unique)      type                 orderNumber     variantId, sku
 description   name              quantityChange       status          itemName, variantName
 basePrice     price (optional)  quantityAfter        totalAmount     unitPrice, quantity
               stockQuantity     reason, reference                    lineTotal
               attributes {k:v}
```

| Concept | Meaning |
|---|---|
| **Item** | A product in the catalog, e.g. "Basic T-Shirt". It has a base price but is not sold directly. |
| **Variant** | A concrete, sellable version of an item, e.g. "Black / M". This is the SKU, and it **holds the stock**. Its price is optional; when it is empty, the item's base price is used. |
| **StockMovement** | An append-only ledger entry for every stock change (`INITIAL`, `RESTOCK`, `ADJUSTMENT`, `SALE`, `SALE_CANCELLED`). |
| **Order** | A sale of one or more variants. Placing it deducts stock; cancelling it puts the stock back. |

---

## API overview

All endpoints are under `/api`. Request and response bodies are JSON.

### Items
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/items` | Create an item, optionally with its variants |
| `GET` | `/api/items?name=&page=&size=&sort=` | List or search items (paginated) |
| `GET` | `/api/items/{itemId}` | Get an item with its variants and total stock |
| `PUT` | `/api/items/{itemId}` | Update name, description and base price |
| `DELETE` | `/api/items/{itemId}` | Delete an item and its variants |

### Variants
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/items/{itemId}/variants` | Add a variant (with optional `initialStock`) |
| `GET` | `/api/items/{itemId}/variants` | List an item's variants |
| `GET` | `/api/items/{itemId}/variants/{variantId}` | Get a variant |
| `PUT` | `/api/items/{itemId}/variants/{variantId}` | Update SKU, name, price and attributes (**not** stock) |
| `DELETE` | `/api/items/{itemId}/variants/{variantId}` | Delete a variant |

### Inventory
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/variants/{variantId}/stock-movements` | `RESTOCK` (adds stock) or `ADJUSTMENT` (adds or removes stock) |
| `GET` | `/api/variants/{variantId}/stock-movements` | Stock history of a variant, newest first |
| `GET` | `/api/inventory/low-stock?threshold=5` | Variants at or below the threshold |

### Orders
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/orders` | Place an order: deducts stock, or returns **409** if anything is out of stock |
| `GET` | `/api/orders?status=&page=&size=` | List orders (paginated, filter by `COMPLETED` / `CANCELLED`) |
| `GET` | `/api/orders/{orderId}` | Get an order |
| `POST` | `/api/orders/{orderId}/cancel` | Cancel an order and return its stock |

### Status codes
| Code | When |
|---|---|
| `200` / `201` / `204` | Success. `201` responses include a `Location` header. |
| `400` | Validation failed (the response lists each invalid field), malformed JSON, or an unknown sort field |
| `404` | Resource not found |
| `409` | Insufficient stock, duplicate SKU, cancelling an order twice, or a concurrent modification |
| `422` | Business rule violated (e.g. a negative restock, or an order line pointing to a variant that does not exist) |

Errors use the standard [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) format.

---

## API examples

The examples below use the sample data (ids assume a fresh database).

### Create an item with variants

```bash
curl -X POST http://localhost:8080/api/items \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Hoodie",
        "description": "Fleece-lined pullover hoodie",
        "basePrice": 249000,
        "variants": [
          { "sku": "HD-GRY-M", "name": "Grey / M", "initialStock": 10,
            "attributes": { "color": "Grey", "size": "M" } },
          { "sku": "HD-GRY-XL", "name": "Grey / XL", "price": 269000, "initialStock": 4,
            "attributes": { "color": "Grey", "size": "XL" } }
        ]
      }'
```

`201 Created`, with `Location: /api/items/5`:

```json
{
  "id": 5,
  "name": "Hoodie",
  "description": "Fleece-lined pullover hoodie",
  "basePrice": 249000,
  "totalStock": 14,
  "inStock": true,
  "variants": [
    { "id": 11, "itemId": 5, "sku": "HD-GRY-M", "name": "Grey / M",
      "attributes": { "color": "Grey", "size": "M" },
      "price": null, "effectivePrice": 249000,
      "stockQuantity": 10, "inStock": true, "createdAt": "...", "updatedAt": "..." },
    { "id": 12, "itemId": 5, "sku": "HD-GRY-XL", "name": "Grey / XL",
      "attributes": { "color": "Grey", "size": "XL" },
      "price": 269000, "effectivePrice": 269000,
      "stockQuantity": 4, "inStock": true, "createdAt": "...", "updatedAt": "..." }
  ],
  "createdAt": "...",
  "updatedAt": "..."
}
```

### Search items

```bash
curl "http://localhost:8080/api/items?name=shirt&page=0&size=10&sort=name,asc"
```

```json
{ "content": [ { "id": 1, "name": "Basic T-Shirt", "...": "..." } ],
  "page": 0, "size": 10, "totalElements": 1, "totalPages": 1 }
```

### Add a variant to an existing item

```bash
curl -X POST http://localhost:8080/api/items/3/variants \
  -H "Content-Type: application/json" \
  -d '{ "sku": "TOTE-BLK", "name": "Black", "initialStock": 12, "attributes": { "color": "Black" } }'
```

### Update a variant's price

```bash
curl -X PUT http://localhost:8080/api/items/1/variants/3 \
  -H "Content-Type: application/json" \
  -d '{ "sku": "TS-BLK-XL", "name": "Black / XL", "price": 115000,
        "attributes": { "color": "Black", "size": "XL" } }'
```

Send `"price": null` (or leave it out) to make the variant inherit the item's base price again.

### Restock and adjust

```bash
# goods received from a supplier
curl -X POST http://localhost:8080/api/variants/4/stock-movements \
  -H "Content-Type: application/json" \
  -d '{ "type": "RESTOCK", "quantity": 30, "reason": "PO-2026-0117" }'

# two units damaged
curl -X POST http://localhost:8080/api/variants/4/stock-movements \
  -H "Content-Type: application/json" \
  -d '{ "type": "ADJUSTMENT", "quantity": -2, "reason": "Damaged in storage" }'
```

```json
{ "id": 11, "variantId": 4, "type": "ADJUSTMENT", "quantityChange": -2, "quantityAfter": 28,
  "reason": "Damaged in storage", "reference": null, "createdAt": "..." }
```

### Stock history and low-stock report

```bash
curl http://localhost:8080/api/variants/4/stock-movements
curl "http://localhost:8080/api/inventory/low-stock?threshold=3"
```

```json
[
  { "itemId": 1, "itemName": "Basic T-Shirt", "variantId": 4, "sku": "TS-WHT-M", "variantName": "White / M", "stockQuantity": 0 },
  { "itemId": 4, "itemName": "Ceramic Coffee Mug", "variantId": 9, "sku": "MUG-WHT", "variantName": "White", "stockQuantity": 1 }
]
```

### Place an order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{ "items": [ { "variantId": 2, "quantity": 2 }, { "variantId": 9, "quantity": 1 } ],
        "note": "Walk-in customer" }'
```

`201 Created`:

```json
{
  "id": 1,
  "orderNumber": "ORD-20261108-3HZDPA",
  "status": "COMPLETED",
  "totalAmount": 253000.00,
  "note": "Walk-in customer",
  "lines": [
    { "variantId": 2, "sku": "TS-BLK-M", "itemName": "Basic T-Shirt", "variantName": "Black / M",
      "unitPrice": 99000.00, "quantity": 2, "lineTotal": 198000.00 },
    { "variantId": 9, "sku": "MUG-WHT", "itemName": "Ceramic Coffee Mug", "variantName": "White",
      "unitPrice": 55000.00, "quantity": 1, "lineTotal": 55000.00 }
  ],
  "createdAt": "...",
  "cancelledAt": null
}
```

### Try to sell something that is out of stock

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{ "items": [ { "variantId": 9, "quantity": 1 } ] }'
```

`409 Conflict`. Nothing is deducted:

```json
{
  "type": "about:blank",
  "title": "Insufficient stock",
  "status": 409,
  "detail": "Insufficient stock for SKU 'MUG-WHT': requested 1, available 0",
  "instance": "/api/orders",
  "sku": "MUG-WHT",
  "requested": 1,
  "available": 0
}
```

### Cancel an order

```bash
curl -X POST http://localhost:8080/api/orders/1/cancel
```

The order's status becomes `CANCELLED` and its units go back to stock (as `SALE_CANCELLED` movements).

### Validation error

```bash
curl -X POST http://localhost:8080/api/items -H "Content-Type: application/json" \
  -d '{ "name": "", "basePrice": -1 }'
```

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "instance": "/api/items",
  "errors": [
    { "field": "name", "message": "must not be blank" },
    { "field": "basePrice", "message": "must be greater than or equal to 0.00" }
  ]
}
```

---

## Design decisions

### Package by feature, layered inside
```
com.ifg.warehouse
├── catalog     items & variants       (domain / repository / service / dto / web)
├── inventory   stock ledger & reports
├── order       placing and cancelling orders
└── common      error handling, paging, base entity, config
```
Grouping code by business capability keeps each feature cohesive and easy to find. Inside each
feature there is a classic controller → service → repository split. Controllers only accept and return
DTOs (Java records), never entities, so the API contract is decoupled from the persistence model.

### Stock lives on the variant
The question a warehouse has to answer is "how many *black T-shirts in size M* do we have", so the
variant (SKU) is the unit that is priced, stocked and sold. An item's stock is the sum of its
variants, computed on read so it can never go out of sync. A product without real options simply has
one variant (e.g. "Standard").

### Optional variant price with fallback
Most variants cost the same as the item, but some (e.g. XL sizes) cost more. Instead of forcing a
price on every variant, `variant.price` is nullable and the API exposes both `price` (the variant's own
price) and `effectivePrice` (what the customer actually pays). Changing the item's base price then
updates every variant that inherits it.

### Preventing overselling: defence in depth
This is the core requirement, so it is enforced at several levels:

1. **Domain rule:** `Variant.decreaseStock()` refuses to go below zero and throws
   `InsufficientStockException`, which the API returns as **409** with the SKU, the requested
   quantity and the available quantity.
2. **Pessimistic row locks:** when an order is placed, each variant is loaded with
   `SELECT … FOR UPDATE`. Concurrent orders for the same SKU are therefore serialised, so two
   customers can never both buy the last unit. I chose pessimistic over optimistic locking here
   because stock contention on popular items is expected, and waiting a few milliseconds is better
   than failing and retrying.
3. **Deadlock avoidance:** variants are always locked in ascending id order, and duplicate lines in
   one request are merged first.
4. **All-or-nothing orders:** the whole order runs in one transaction. If any line is short, nothing
   is deducted and no order is created.
5. **Database constraint:** `CHECK (stock_quantity >= 0)` is the last line of defence, even against
   bugs or manual SQL.

`OrderTransactionTest` proves this: 20 threads try to buy 1 unit each of a variant with 5 in stock, and
exactly 5 succeed.

### Stock is only changed through an audited ledger
`PUT` on a variant **cannot** change stock. Stock changes only via `RESTOCK` / `ADJUSTMENT` movements
or via orders, and each change goes through one class (`StockLedger`) that updates the variant and
appends a `stock_movements` row in the same transaction. Every stock level can therefore be explained
("why do we have 7?"), which matters in a real warehouse.

### Orders snapshot product data
Order lines copy the SKU, names and unit price at the moment of sale, and reference the variant only by
id (not a foreign key). Historical orders stay correct when prices change or variants are deleted.

### Other choices
- **Flyway migrations + `ddl-auto=validate`:** the schema is versioned and reviewable, and Hibernate
  only checks that the entities match it. This is safer than letting Hibernate generate tables.
- **H2 in file mode:** zero setup for the reviewer, yet data persists across restarts. Moving to
  PostgreSQL only needs a different datasource URL and driver.
- **Optimistic locking (`@Version`)** on variants and orders prevents lost updates from concurrent
  edits and stops a double cancellation from returning stock twice.
- **RFC 9457 Problem Details** give every error the same machine-readable shape.
- **SKUs are normalised to upper case** and unique, so `ts-blk-m` and `TS-BLK-M` cannot both exist.
- **Pagination** on list endpoints (max page size 100) and Hibernate batch fetching to avoid N+1 queries.
- **No Lombok:** plain Java and records keep the code explicit and IDE-independent.

### Testing
- **Unit tests** for the domain rules (`VariantTest`).
- **Integration tests** with MockMvc against the real Spring context and an in-memory database for every
  endpoint, including error paths (`*ApiIntegrationTest`).
- **Transaction tests** without test-managed transactions, so rollback and locking really happen
  (`OrderTransactionTest`).

---

## Assumptions

- **Placing an order means a completed sale.** Stock is deducted immediately. There is no cart, payment,
  reservation or shipping workflow; cancellation is the only way to undo a sale.
- **Variants are the sellable unit.** An item with no variants exists in the catalog but cannot be sold
  until at least one variant is added.
- **One warehouse, one currency.** Prices are stored as `DECIMAL(12,2)` amounts in the shop's currency
  (the sample data uses IDR); multi-currency and multi-location stock are out of scope.
- **Deleting is a hard delete.** Deleting an item removes its variants and their stock history, but
  orders keep their snapshot data. A production system would more likely archive (soft delete).
- **Authentication and authorisation are out of scope.**
- Variant **attributes** are free-form key/value pairs (up to 10) rather than a fixed set of option
  types, to keep the model flexible.

---

## Possible next steps

- Authentication (e.g. Spring Security with roles for staff and admin)
- Stock reservations for carts with expiry, separating "on hand" from "available"
- Soft delete / archiving for items and variants
- PostgreSQL profile and Testcontainers-based tests
- Docker image and CI pipeline
