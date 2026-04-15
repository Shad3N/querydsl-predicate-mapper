# QueryDSL Predicate Mapper Examples

This module contains example usage scenarios demonstrating the microservices architecture pattern.

## Module Structure

```
examples/
├── shared/                  # Shared filter DTOs (published to both services)
├── jpa-application/         # Receiver service with JPA/Spring Data
├── no-jpa-application/      # Standalone app with @QueryEntity
└── query-sender/            # Sender service with HttpExchange client
```

## Modules

### shared

Contains filter DTOs shared between sender and receiver services:

- `ProductFilter` - Full-featured filter with all operators
- `OrderFilter` - Filter for non-JPA example
- `ProductStatus` - Enum shared across services

**Key point:** Both services depend on this module, ensuring type safety across service boundaries.

### jpa-application

Receiver service with Spring Data JPA:

- **Entities:** `Product`, `Category` (JPA `@Entity`)
- **Query interface:** `ProductQueries` (`@PredicateMapper`)
- **Repository:** `ProductRepository` (`QuerydslPredicateExecutor`)
- **Service:** `ProductService` - demonstrates predicate usage
- **Controller:** `ProductController` - accepts `ProductFilter` as query params

**Flow:**

```
GET /api/products?name=widget&status=AVAILABLE&activeOnly=true
        ↓
@ModelAttribute ProductFilter filter
        ↓
Predicate predicate = productQueries.filter(filter)
        ↓
repository.findAll(predicate)
```

### no-jpa-application

Standalone application without Spring/JPA:

- **Entities:** `Order`, `Customer` (`@QueryEntity`)
- **Query interface:** `OrderQueries` (`@PredicateMapper`)
- **Service:** `OrderService` - in-memory collection filtering

Demonstrates using the predicate mapper in:

- Batch jobs
- CLI applications
- Non-Spring contexts

### query-sender

Sender service demonstrating filter construction:

- **HTTP Client:** `ProductClient` - Spring 6 `@HttpExchange`
- **Service:** `ProductSenderService` - constructs filters, calls receiver
- **Controller:** `QuerySenderController` - exposes query endpoints

**Flow:**

```
ProductFilter filter = new ProductFilter();
filter.setName("widget");
filter.setStatuses(List.of(AVAILABLE));
filter.setActiveOnly(true);
        ↓
productClient.searchByFilter(filter)
        ↓
HTTP GET /api/products?name=widget&statuses=AVAILABLE&activeOnly=true
        ↓
Receiver converts to QueryDSL predicate
```

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Shared Library                                 │
│                                                                             │
│   ProductFilter {                                                           │
│     @FilterField(path="name", op=LIKE)                                      │
│     @FilterField(path="status", op=IN)                                      │
│     @FilterField(path="category.name", op=EQ)                               │
│   }                                                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                    │                                    │
                    ▼                                    ▼
┌────────────────────────────────┐    ┌────────────────────────────────────────┐
│       query-sender             │    │           jpa-application              │
│                                │    │                                        │
│  ProductFilter filter = ...    │    │  @PredicateMapper                      │
│  filter.setName("widget");     │    │  interface ProductQueries {            │
│  filter.setStatuses(...);      │    │    Predicate filter(ProductFilter);    │
│                                │    │  }                                     │
│  productClient.search(filter); │    │                                        │
│                                │    │  → Generates ProductQueriesImpl        │
│  ✓ Compile-time safe           │    │  → Validates against QProduct          │
│                                │    │  ✓ Compile-time safe                   │
└────────────────────────────────┘    └────────────────────────────────────────┘
```

## Running the Examples

```bash
# Build everything
mvn clean install

# The examples modules are not published to Maven Central
# They're intended for local development and documentation
```

## Generated Code

After running `mvn compile`, check these locations:

**Predicate Mapper implementations:**

- [ProductQueriesImpl.java](jpa-application/target/generated-sources/annotations/io/github/shad3n/predicatemapper/examples/jpa/query/ProductQueriesImpl.java)
- [OrderQueriesImpl.java](no-jpa-application/target/generated-sources/annotations/io/github/shad3n/predicatemapper/examples/nojpa/query/OrderQueriesImpl.java)

The generated code is plain, readable Java.

## Single Endpoint, Infinite Queries

This example demonstrates the powerful architectural benefit: **the receiver service needs only one endpoint per entity
type**.

```java

@GetMapping
public List<Product> search(@ModelAttribute ProductFilter filter) {
    return productService.search(filter);
}
```

The sender can now query by:

- Name pattern (`LIKE`)
- Price range (`GTE`/`LTE`)
- Status list (`IN`)
- Category name (nested path)
- Any combination of the above
- **Any future filter fields you add** — zero receiver changes required

Want to add a new filter criterion? Just add a field to `ProductFilter` with the appropriate `@FilterField` annotation.
The receiver automatically supports it.

**Sender gains full query flexibility with absolutely minimal receiver setup.**
