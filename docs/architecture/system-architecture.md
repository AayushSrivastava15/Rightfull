# Rightful – System Architecture

## 1. System Topology Overview

Rightful is architected as a **Modular Monolith** with clear packaging boundaries and strict domain encapsulation. It balances operational simplicity with enterprise-grade modularity.

```mermaid
flowchart TB
    subgraph ClientLayer["Clients & End Users"]
        WebSPA["Rightful Admin SPA (React / Vite)"]
        ClientApps["Licensed Applications (Desktop, CLI, On-Prem)"]
    end

    subgraph IdentityProvider["Identity Provider"]
        SupaAuth["Supabase Auth (Gotrue)\n- Email / Password\n- Google OAuth\n- GitHub OAuth"]
    end

    subgraph BackendMonolith["Rightful Core (Spring Boot / Java 25)"]
        GatewayFilter["Spring Security Filter Chain\n(Supabase JWKS Validation)"]
        
        subgraph CoreDomains["Package-By-Feature Modules"]
            IdentityMod["identity"]
            WorkspaceMod["workspace"]
            ProductMod["product"]
            PlanMod["plan"]
            CustomerMod["customer"]
            LicensingMod["licensing"]
            ActivationMod["activation"]
            ValidationMod["validation"]
            BillingMod["billing (Proposed)"]
            AuditMod["audit"]
            IntegrationMod["integration"]
            NotificationMod["notification"]
        end
        
        GatewayFilter --> CoreDomains
    end

    subgraph DataLayer["Storage & Data Engine"]
        PostgresDB[(Supabase PostgreSQL)]
    end

    WebSPA -->|1. Authenticate| SupaAuth
    SupaAuth -->|2. Issue JWT| WebSPA
    WebSPA -->|3. API Requests + Bearer JWT| GatewayFilter
    GatewayFilter -->|Validate Signature| SupaAuth
    CoreDomains -->|4. Read/Write| PostgresDB
    ClientApps -->|5. License Validation & Heartbeat (5 min)| GatewayFilter
```

---

## 2. Request & Communication Lifecycle

### 2.1 Management Plane (Frontend to Backend)
1. User authenticates via Supabase Auth inside the React SPA (Email/Password, Google OAuth, or GitHub OAuth).
2. Supabase client stores the session and provides an RS256/ES256 signed JWT Access Token.
3. React SPA sends HTTP requests to the Rightful REST API (`/api/v1/...`) with the `Authorization: Bearer <token>` header.
4. Spring Security intercepts the request, retrieves public keys from the Supabase JWKS endpoint (`{SUPABASE_URL}/auth/v1/.well-known/jwks.json`), verifies signature, claims, and expiry.
5. `SecurityContext` establishes the authenticated principal (`sub` ID).
6. Application services verify workspace membership and permissions before executing domain operations.

### 2.2 Licensing Data Plane (Client Application to Backend)
1. A licensed software application initializes with a license key configured by the end user or admin.
2. The client application calls `/api/v1/validation/verify` supplying:
   - License key
   - Product ID
   - Hardware / Device Fingerprint
   - Activation token (if previously bound)
3. Rightful hashes the license key with HMAC-SHA-256 and locates the license record.
4. The system evaluates:
   - Expiration date
   - Status (ACTIVE, SUSPENDED, REVOKED)
   - Max seat allocation & device activation status
   - Entitlement claims
5. Upon successful validation:
   - Generates an Ed25519-signed Entitlement Token (contains expiry, features, device ID, grace period).
   - Updates `last_validated_at` timestamp.
   - Resets the 7-hour offline grace window.
   - Returns the signed token to the client.
6. The client app schedules the next heartbeat in 5 minutes. If offline, it verifies the Ed25519 signature locally using the embedded product public key and verifies that elapsed time since last validation is `< 7 hours`.

---

## 3. High Availability & Scalability Attributes

- **Stateless Application Tier**: The Spring Boot backend maintains zero session state. Horizontal scaling is trivial behind a standard load balancer.
- **Connection Pooling**: HikariCP maintains optimized connection pools to Supabase PostgreSQL with statement caching.
- **Caching & JWKS Resiliency**: Remote JWKS public keys are cached in memory with automatic rotation fallback, avoiding per-request external HTTP calls.
- **Fail-Closed Licensing**: If an entitlement cannot be verified and the grace period has elapsed, the client SDK fails securely according to configured plan rules.

