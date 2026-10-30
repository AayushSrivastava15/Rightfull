# Rightful – Database Architecture & Entity Design

## 1. Overview & Strategy

Rightful leverages **Supabase PostgreSQL** as its primary persistent relational datastore. The persistence model is designed to support:
- Strict multi-tenant isolation via workspace partitioning.
- High-throughput license validation and device heartbeats.
- Zero plaintext storage of sensitive license keys (HMAC-SHA-256 hashed).
- Idempotent and version-controlled schema evolution using **Flyway**.

> [!NOTE]
> Detailed entity mappings, foreign keys, and indexes will be finalized in **Milestone Step 3.3**. The schema layout below represents the architectural design blueprint.

---

## 2. High-Level Entity Relationship Diagram (Proposed)

```mermaid
erDiagram
    USERS ||--o{ WORKSPACE_MEMBERS : has
    WORKSPACES ||--o{ WORKSPACE_MEMBERS : contains
    WORKSPACES ||--o{ PRODUCTS : owns
    PRODUCTS ||--o{ PLANS : defines
    WORKSPACES ||--o{ CUSTOMERS : manages
    PLANS ||--o{ LICENSES : provisions
    CUSTOMERS ||--o{ LICENSES : holds
    LICENSES ||--o{ ACTIVATIONS : permits
    LICENSES ||--o{ VALIDATION_LOGS : records
    WORKSPACES ||--o{ AUDIT_LOGS : records

    USERS {
        uuid id PK
        string supabase_user_id UK
        string email UK
        string full_name
        timestamp created_at
    }

    WORKSPACES {
        uuid id PK
        string name
        string slug UK
        string owner_user_id FK
        timestamp created_at
    }

    WORKSPACE_MEMBERS {
        uuid id PK
        uuid workspace_id FK
        uuid user_id FK
        string role "OWNER, ADMIN, DEVELOPER, VIEWER"
        timestamp joined_at
    }

    PRODUCTS {
        uuid id PK
        uuid workspace_id FK
        string name
        string slug
        string ed25519_public_key
        text ed25519_private_key_encrypted
        timestamp created_at
    }

    PLANS {
        uuid id PK
        uuid product_id FK
        string name
        string code
        int max_seats
        int offline_grace_period_hours "Default: 7"
        int validation_interval_minutes "Default: 5"
        jsonb features
        timestamp created_at
    }

    CUSTOMERS {
        uuid id PK
        uuid workspace_id FK
        string type "INDIVIDUAL, ORGANIZATION"
        string name
        string email
        string company_name
        timestamp created_at
    }

    LICENSES {
        uuid id PK
        uuid workspace_id FK
        uuid product_id FK
        uuid plan_id FK
        uuid customer_id FK
        string key_prefix "e.g. RF-A1B2"
        string key_hash UK "HMAC-SHA-256"
        string status "ACTIVE, SUSPENDED, REVOKED, EXPIRED"
        int max_seats
        int used_seats
        timestamp expires_at
        timestamp last_validated_at
        timestamp created_at
    }

    ACTIVATIONS {
        uuid id PK
        uuid license_id FK
        string device_fingerprint
        string device_name
        string ip_address
        string platform
        timestamp activated_at
        timestamp last_heartbeat_at
        string status "ACTIVE, DEACTIVATED"
    }

    VALIDATION_LOGS {
        uuid id PK
        uuid license_id FK
        string ip_address
        string status "SUCCESS, REJECTED, EXPIRED, SEAT_EXCEEDED"
        string reason
        timestamp validated_at
    }

    AUDIT_LOGS {
        uuid id PK
        uuid workspace_id FK
        uuid actor_user_id FK
        string action
        string target_type
        uuid target_id
        jsonb metadata
        timestamp created_at
    }
```

---

## 3. Cryptographic Storage Guarantees

1. **License Key Storage**:
   - Plaintext license keys (e.g., `RF-PROD-98FA-C12B-789E-AA01`) are returned **only once** upon creation.
   - The database stores only a non-reversible `key_hash = HMAC-SHA256(key, application_hmac_secret)` and a short display prefix (e.g. `RF-PROD-98FA-****`).
   - If a database backup is compromised, attackers cannot reconstruct valid license keys.
2. **Product Entitlement Signing Keys**:
   - Each product possesses a unique Ed25519 keypair.
   - The public key is publicly accessible and embedded into client applications.
   - The private key is encrypted at rest using envelope encryption (AES-256-GCM with master key) before saving into `ed25519_private_key_encrypted`.

---

## 4. Multi-Tenant Partitioning & Data Isolation

- **Workspace Scoping**: All operational tables (`products`, `plans`, `customers`, `licenses`, `audit_logs`) carry a mandatory, foreign-keyed `workspace_id` column.
- **Query Protection**: Application layer services enforce workspace-scoped criteria on all database reads and writes. Cross-workspace data querying is strictly prevented.
- **Row Level Security (RLS) Readiness**: Supabase RLS policies will be introduced in tandem with direct Supabase client calls if required, while Spring Boot enforces security via JPA Specification predicates and tenant filter interceptors.

---

## 5. Migration Strategy with Flyway

- All DDL statements are maintained under `backend/src/main/resources/db/migration/`.
- File naming convention follows standard Flyway semantics: `V<Version>__<Description>.sql`.
- In accordance with milestone boundaries, no business tables are executed initially; `V1__init_schema.sql` establishes the schema baseline.
