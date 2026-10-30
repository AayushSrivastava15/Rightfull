# Rightful – Backend Modules & Package-by-Feature Architecture

## 1. Modular Monolith Architecture Pattern

Rightful adheres to a strict **package-by-feature** architectural pattern under the root namespace `com.rightful`. Rather than grouping code horizontally by layer (all controllers together, all repositories together), code is partitioned vertically by business capability.

This architectural style ensures:
- High cohesion within each domain.
- Loose coupling between domains.
- Clear ownership of entities and database queries.
- Straightforward path to extracting services in the future if organizational or scaling requirements ever demand it, without the premature complexity of microservices.

---

## 2. Package Hierarchy & Responsibilities

```
com.rightful
├── config/                  # Infrastructure configurations (Web, OpenAPI, Flyway, Async, Caching)
├── security/                # Spring Security, Supabase JWT verification, JWKS caching, context filters
├── common/                  # Shared primitives, exceptions, standard response envelopes, utils
│   ├── exception/           # BusinessException, ResourceNotFoundException, UnauthorizedException
│   ├── response/            # ApiResponse<T>, PageResponse<T>, ErrorResponse
│   └── util/               # Cryptography helpers (Ed25519, HMAC-SHA-256), TimeUtils, SlugUtils
├── identity/                # Internal user entity, Supabase user sync, profile management
├── workspace/               # Workspaces, organizations, multi-tenant isolation, membership & roles
├── product/                 # Software products catalog, releases, platforms, API credentials
├── plan/                    # License tiers/plans, feature flags, entitlements, quota definitions
├── customer/                # License holders (individuals, corporate accounts, billing contacts)
├── licensing/               # Core license lifecycle (creation, status, suspension, expiration, revocation)
├── activation/              # Node-locking, device fingerprints, seat count allocation, heartbeat tracking
├── validation/              # Cryptographic validation engine, Ed25519 entitlement token minting
├── billing/                 # [PROPOSED] Payment processor webhooks, subscription sync, tier upgrades
├── audit/                   # Immutable audit logs, validation access records, security events
├── integration/             # Outgoing webhooks, external developer API keys, third-party connectors
└── notification/            # System email/webhook notifications (license expiring, seat limit alerts)
```

---

## 3. Module Anatomy (Standard Structure per Feature)

Each feature module is structured around the following internal packages:

```
com.rightful.<module>/
├── controller/              # Spring MVC REST Controllers (@RestController, @RequestMapping)
├── service/                 # Domain interfaces & transactional implementations (@Service, @Transactional)
├── repository/              # Spring Data JPA interfaces extending JpaRepository & JpaSpecificationExecutor
├── entity/                  # JPA @Entity definitions with auditing metadata (createdAt, updatedAt)
├── dto/
│   ├── request/             # Incoming request payloads with Bean Validation (@Valid, @NotNull)
│   └── response/            # Outgoing response DTOs (preventing internal entity leakage)
├── mapper/                  # Pure mapping mappers converting between entities and DTOs
└── enums/                   # Domain-specific status enumerations, types, and flags
```

---

## 4. Module Interaction Rules

1. **Direct Repository Access Boundary**:
   - A module's repository **must never** be accessed directly by another module.
   - Cross-domain interactions must happen through the target module's **Service interface** or via domain event publishing.
2. **Workspace Isolation Boundary**:
   - Every tenant-scoped entity must retain a foreign key or relationship to `Workspace`.
   - All repository query methods must include `workspaceId` in filtering predicates or use workspace-aware specifications to prevent cross-tenant data leakage.
3. **DTO Separation**:
   - Entities must never be returned directly from REST controllers. Controllers always serialize validated, typed DTOs wrapped in the unified `ApiResponse<T>` envelope.
4. **Proposed vs Confirmed Modules**:
   - **Confirmed**: `security`, `common`, `identity`, `workspace`, `product`, `plan`, `customer`, `licensing`, `activation`, `validation`, `audit`, `integration`, `notification`.
   - **Proposed**: `billing` (will be integrated with Stripe / LemonSqueezy in a subsequent milestone).

