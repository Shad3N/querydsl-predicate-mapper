# QueryDSL Predicate Mapper

**Type-safe, compile-time validated QueryDSL predicate generation from annotated interfaces.**

[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-Central-blue)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

---

> This library is heavily WIP and not yet ready for production use. Expect large API changes or even paradigm shifts as I iterate on the design. On the other side, you can expect addition of more powerful features.

## The Problem

You need flexible, type-safe filtering on your QueryDSL-backed endpoints — whether that's an internal microservice
boundary or a public-facing REST API. You want:

- **Versatile query APIs** - let callers define what to filter on without exposing your entity internals
- **Compile-time safety** - Q-class paths validated during compilation, not discovered in production
- **Explicit filter contracts** - callers see exactly which fields and operators are available
- **No drift** - filter definitions stay in sync with your data model

But you will most likely struggle with:

- **Runtime failures** from typos in Q-class paths - wrong results, no exceptions
- **Manual wiring** of every field to its predicate operation - code bloat
- **No compile-time safety** when Q-class paths change or fields are renamed

## The Solution

QueryDSL Predicate Mapper uses **Annotation Processing (APT)** to generate predicate implementations at compile time.
Define a filter DTO with `@FilterField` annotations, and the APT generates the predicate wiring — validated against your
Q-class at compile time:

```java
public class UserFilter {
    
    @FilterField(path = "name", op = Op.LIKE)
    private String name;
    
    @FilterField(path = "status", op = Op.IN)
    private List<Status> statuses;
    
    // Getters and setters...
}
```

**Receiver** deserializes and converts to a QueryDSL predicate:

```java
@GetMapping("/users")
public List<User> search(@ModelAttribute UserFilter filter) {
    Predicate predicate = userQueries.filter(filter);
    return userRepository.findAll(predicate);
}
```

**If a Q-class path doesn't exist → compilation fails. No silent ignores. No runtime surprises.**

The filter DTO doubles as an explicit contract — it clearly defines which fields are filterable and with which
operators. This makes it a natural fit for Swagger/OpenAPI documentation: the DTO is referenced directly in your
endpoint spec, so API consumers know exactly what they can query.

---

## Use Cases

### Inter-Microservice Communication

The classic use case: **type-safe queries across service boundaries**.

Both services share a filter DTO library. The sender populates the filter and serializes it as query parameters; the
receiver deserializes it and converts to a QueryDSL predicate. The shared DTO contract ensures both sides stay in sync —
and the APT validates that every `@FilterField` path actually exists on the Q-class.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Shared Library                                 │
│                                                                             │
│   UserFilter {                    @FilterField annotations define:          │
│     @FilterField(path="name")      - Which Q-class path to use              │
│     @FilterField(path="status")    - Which operator to apply                │
│     @FilterField(path="age")       - Compile-time validation                │
│   }                                                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                    │                                    │
                    ▼                                    ▼
┌────────────────────────────────┐    ┌────────────────────────────────────────┐
│       Sender Service           │    │           Receiver Service             │
│                                │    │                                        │
│  UserFilter filter = new...    │    │  @PredicateMapper                      │
│  filter.setName("john");       │    │  interface UserQueries {               │
│  filter.setStatuses(...);      │    │    @ToPredicate(QUser.class)           │
│                                │    │    Predicate filter(UserFilter f);     │
│  // HTTP: /users?name=john...  │    │  }                                     │
│  client.searchUsers(filter);   │    │                                        │
│                                │    │  → Generates UserQueriesImpl           │
│  ✓ Compile-time safe           │    │  → Validates against QUser             │
│  ✓ Same filter object          │    │  ✓ Compile-time safe                   │
│                                │    │  ✓ No manual wiring                    │
└────────────────────────────────┘    └────────────────────────────────────────┘
```

The shared library contains:

- Filter DTOs with `@FilterField` annotations
- `Op` enum for operators
- `@PredicateMapper` and `@ToPredicate` annotations

**Q-classes stay private** to the receiver service — they never leak into the shared library.

### Public-Facing API Filtering

The same pattern works for public REST APIs. The filter DTO explicitly defines which fields are filterable and with
which operators — no need to expose your entity internals. This pairs naturally with **Swagger/OpenAPI**: the filter DTO
is referenced directly in your endpoint documentation, so API consumers see exactly what they can query.

```java
// The filter DTO IS your API contract
public class ProductFilter {
    
    @FilterField(path = "name", op = Op.LIKE)
    private String name;

    @FilterField(path = "category.name", op = Op.EQ)
    private String category;

    @FilterField(path = "price", op = Op.GTE)
    private BigDecimal minPrice;

    @FilterField(path = "price", op = Op.LTE)
    private BigDecimal maxPrice;
    
    @FilterField(path = "status", op = Op.IN)
    private List<ProductStatus> statuses;

    // Non-filtered fields (pagination, sorting) - no @FilterField
    private Integer page;
    private Integer size;
    
    // Getters and setters...
}
```

```java

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @GetMapping
    public Page<Product> search(@ModelAttribute ProductFilter filter, Pageable pageable) {
        return productService.search(filter, pageable);
    }
}
```

With Springdoc (Swagger/OpenAPI), the `ProductFilter` appears in the endpoint docs automatically — consumers know
exactly which query parameters are available and what types they accept.

> **Note:** Spring's `@ModelAttribute` silently ignores unknown query parameters. A custom `WebMvcConfigurer` for strict
> query parameter validation is planned as a future feature — it will reject requests with unrecognized parameters instead
> of silently ignoring them.

---

## Features

- **Compile-time validation** - Q-class paths verified during compilation
- **Type-safe query declarations** - Interfaces as query contracts
- **Zero runtime reflection** - Generated code is plain, readable Java
- **Multiple operators** - `EQ`, `NOT_EQ`, `LTE`, `GTE`, `LIKE`, `IN`, `IS_NULL`, `IS_NOT_NULL`
- **Nested path support** - Traverse Q-class relationships: `"category.name"`, `"address.city"`
- **Swagger/OpenAPI friendly** - Filter DTOs serve as documented query parameter contracts
- **Spring integration** - Generated implementations are `@Component` beans
- **Predicate composition** - Combine multiple predicates with standard QueryDSL operators

---

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>io.github.shad3n</groupId>
    <artifactId>querydsl-predicate-mapper</artifactId>
    <version>0.0.1</version>
</dependency>
```

### 2. Define Your Filter/Query Object

```java
public class UserFilter {
    
    @FilterField(path = "name", op = Op.LIKE)
    private String name;
    
    @FilterField(path = "email", op = Op.EQ)
    private String email;
    
    @FilterField(path = "age", op = Op.GTE)
    private Integer minAge;
    
    @FilterField(path = "age", op = Op.LTE)
    private Integer maxAge;
    
    @FilterField(path = "status", op = Op.IN)
    private List<Status> statuses;
    
    @FilterField(path = "deletedAt", op = Op.IS_NULL)
    private Boolean notDeleted;
    
    // Getters and setters...
}
```

### 3. Declare Your Query Interface

```java
@PredicateMapper
public interface UserQueries {
    
    @ToPredicate(QUser.class)
    Predicate filter(UserFilter filter);
}
```

### 4. Use It

```java
import com.querydsl.core.types.Predicate;

@Service
public class UserService {
    
    private final UserQueries userQueries;
    private final UserRepository userRepository;
    
    public UserService(UserQueries userQueries, UserRepository userRepository) {
        this.userQueries = userQueries;
        this.userRepository = userRepository;
    }
    
    public List<User> search(UserFilter filter) {
        Predicate predicate = userQueries.filter(filter);
        return userRepository.findAll(predicate);
    }
    
    public List<User> searchActive(UserFilter filter) {
        // Compose predicates using varargs (requires default method in repository)
        Predicate userPredicate = userQueries.filter(filter);
        Predicate activePredicate = QUser.user.status.eq(Status.ACTIVE);
        return userRepository.findAll(userPredicate, activePredicate);
    }
}
```

---

## Generated Code

The APT generates clean, readable implementation classes. For the interface above:

```java
@Component
public class UserQueriesImpl implements UserQueries {
    
    @Override
    public Predicate filter(UserFilter dto) {
        QUser q = new QUser("user");
        BooleanBuilder builder = new BooleanBuilder();
        
        if (dto.getName() != null) {
            builder.and(q.name.like(dto.getName()));
        }
        if (dto.getEmail() != null) {
            builder.and(q.email.eq(dto.getEmail()));
        }
        if (dto.getMinAge() != null) {
            builder.and(q.age.goe(dto.getMinAge()));
        }
        if (dto.getMaxAge() != null) {
            builder.and(q.age.loe(dto.getMaxAge()));
        }
        if (dto.getStatuses() != null) {
            builder.and(q.status.in(dto.getStatuses()));
        }
        if (Boolean.TRUE.equals(dto.getNotDeleted())) {
            builder.and(q.deletedAt.isNull());
        }
        
        Predicate result = builder.getValue();
        return result != null ? result : Expressions.TRUE;
    }
}
```

**No magic. No reflection. Just straightforward Java you can read and debug.**

---

## Operators

| Operator | Q-Class Method | Description |
|----------|----------------|-------------|
| `EQ` | `eq()` | Equality |
| `NOT_EQ` | `ne()` | Inequality |
| `LTE` | `loe()` | Less or equal |
| `GTE` | `goe()` | Greater or equal |
| `LIKE` | `like()` | SQL LIKE pattern |
| `IN` | `in()` | Value in collection |
| `IS_NULL` | `isNull()` | Null check (Boolean flag) |
| `IS_NOT_NULL` | `isNotNull()` | Not null check (Boolean flag) |

---

## Compile-Time Validation

The APT validates every `@FilterField` against the referenced Q-class:

```java
@FilterField(path = "nonExistentField", op = Op.EQ)
private String broken;
```

```
error: Q-class path 'nonExistentField' does not exist on QUser
```

**Compilation fails fast. No silent ignores. No mysterious empty results in production.**

---

## Nested Paths

Traverse relationships in your Q-class:

```java
@FilterField(path = "category.name", op = Op.EQ)
private String categoryName;

@FilterField(path = "address.city.country.code", op = Op.EQ)
private String countryCode;
```

---

## Spring Data JPA Integration

Use with `QuerydslPredicateExecutor` for seamless repository integration:

```java
public interface UserRepository extends JpaRepository<User, UUID>, QuerydslPredicateExecutor<User> {
    
    // Override to return List instead of Iterable
    @Override
    List<User> findAll(Predicate predicate);

    /**
     * Convenience method: find all matching ALL given predicates (AND composition).
     */
    default List<User> findAll(Predicate... predicates) {
        return findAll(ExpressionUtils.allOf(predicates));
    }
}
```

```java
@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final UserQueries userQueries;
    
    public UserService(UserRepository userRepository, UserQueries userQueries) {
        this.userRepository = userRepository;
        this.userQueries = userQueries;
    }
    
    public List<User> search(UserFilter filter) {
        Predicate predicate = userQueries.filter(filter);
        return userRepository.findAll(predicate);
    }
    
    public Page<User> searchPaged(UserFilter filter, Pageable pageable) {
        Predicate predicate = userQueries.filter(filter);
        return userRepository.findAll(predicate, pageable);
    }
    
    public List<User> searchActive(UserFilter filter) {
        // Compose predicates using varargs - cleaner than ExpressionUtils.allOf()
        Predicate userPredicate = userQueries.filter(filter);
        Predicate activePredicate = QUser.user.status.eq(Status.ACTIVE);
        return userRepository.findAll(userPredicate, activePredicate);
    }
}
```

> **Note:** `QuerydslPredicateExecutor.findAll(Predicate)` returns `Iterable<T>` by default. Override it in your repository interface to return `List<T>` for convenience.

`QuerydslPredicateExecutor` provides:

```java
Optional<User> findOne(Predicate predicate);
Iterable<User> findAll(Predicate predicate);  // Override to List<T> in your repository
List<User> findAll(Predicate predicate, Sort sort);
Page<User> findAll(Predicate predicate, Pageable pageable);
long count(Predicate predicate);
boolean exists(Predicate predicate);
```

---

## Predicate Composition

Combine predicates from multiple sources using `ExpressionUtils`:

```java
import com.querydsl.core.types.ExpressionUtils;

// External user filter
Predicate external = userQueries.filter(userFilter);

// Internal security filter
Predicate security = QUser.user.tenantId.eq(currentTenantId);

// Soft-delete filter
Predicate notDeleted = QUser.user.deletedAt.isNull();

// Combine all predicates
Predicate combined = ExpressionUtils.allOf(external, security, notDeleted);
repository.findAll(combined);
```

> **Note:** `Predicate` interface doesn't have `.and()` method. Use `ExpressionUtils.allOf(predicate1, predicate2, ...)` or `BooleanBuilder` to combine predicates.

### Convenience: Varargs in Repository

For cleaner composition, add a default method to your repository:

```java
public interface UserRepository extends JpaRepository<User, UUID>, QuerydslPredicateExecutor<User> {
    
    @Override
    List<User> findAll(Predicate predicate);

    default List<User> findAll(Predicate... predicates) {
        return findAll(ExpressionUtils.allOf(predicates));
    }
}

// Usage - pass multiple predicates directly
repository.findAll(userPredicate, securityPredicate, notDeletedPredicate);
```

---

## Why This Approach?

### vs. Manual Predicate Building

| Manual | Predicate Mapper |
|--------|------------------|
| Runtime errors from typos | Compile-time validation |
| Boilerplate per field | Annotation-driven |
| Easy to miss null checks | Generated null safety |
| Hard to maintain | Single source of truth |

### vs. Query by Example (QBE)

| QBE | Predicate Mapper |
|-----|------------------|
| Limited operators | Full operator support |
| No nested paths | Arbitrary path depth |
| Implicit matching | Explicit declarations |
| Runtime failures | Compile-time safety |

### vs. Specifications/Criteria API

| Specifications | Predicate Mapper |
|----------------|------------------|
| Verbose lambdas | Clean annotations |
| String-based paths | Type-checked paths |
| Runtime errors | Compile-time errors |

### vs. `@QuerydslPredicate` (Spring Data)

| `@QuerydslPredicate` | Predicate Mapper |
|----------------------|------------------|
| Web-layer only (controllers) | Works anywhere in your app |
| Convention-based operators | Explicit operator per field |
| No compile-time validation | Paths validated at compile time |
| Implicit binding by field name | Explicit `@FilterField` declarations |
| Limited operator control | Full operator control per field |
| Hard to compose predicates | Standard QueryDSL composition |
| Silent ignores on unknown paths | Compilation fails on invalid paths |
| Tied to HTTP query params | Works with any object (DTOs, domain objects, etc.) |

`@QuerydslPredicate` is great for simple CRUD endpoints where you want to expose entity fields directly to HTTP query parameters. But when you need:

- **Compile-time safety** for Q-class paths
- **Explicit operator control** (this field is `LIKE`, that field is `IN`)
- **Use outside the web layer** (service layer, batch jobs, internal APIs)
- **Predicate composition** with other query logic
- **Custom filter objects** that don't map 1:1 to entity fields
- **Single endpoint evolution** - receiver defines one endpoint, and all future query parameter changes are handled automatically by adding fields to the shared filter object, with full type safety preserved across both services

...that's where Predicate Mapper shines.

---

## Building

```bash
mvn clean install -pl core
```

---

## License

MIT License - see [LICENSE](LICENSE) for details.

---

## Contributing

Contributions welcome! Please open an issue or submit a pull request.
Something is not working as expected? Found a bug? Want to see a new feature? Let me know!
