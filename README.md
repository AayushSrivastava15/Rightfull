# Rightful – Software Licensing Management Platform

> **Open-Source, Free-to-Start, Multi-Tenant SaaS Software Licensing Platform**

Rightful is an enterprise-grade software licensing management platform engineered for independent developers, freelancers, startups, and software companies. It provides turnkey software product registration, tiered plan management, cryptographically signed offline/online entitlements, node-locking, and device heartbeat tracking.

---

## Architecture Blueprint

Rightful is built as a **Modular Monolith** organized in a monorepo structure with clear boundaries and independent build pipelines:

```
License-Hub/ (d:\RIGHTFUL.com\)
│
├── frontend/                # React 19, Vite, TypeScript (strict), Tailwind CSS v4, shadcn/ui
│   ├── src/
│   │   ├── app/             # Application entry and bootstrap
│   │   ├── components/      # UI primitives (shadcn) & shared navigation
│   │   ├── features/        # Feature modules (auth, dashboard, licensing, products, etc.)
│   │   ├── hooks/           # Custom React hooks (useHealth, useWorkspace)
│   │   ├── layouts/         # Layout shells (AppLayout, AuthLayout)
│   │   ├── lib/             # Shared utilities (cn helper)
│   │   ├── routes/          # Client-side router definitions
│   │   ├── services/        # API client and TanStack Query client
│   │   └── types/           # Domain TypeScript definitions
│   └── .env.example         # Frontend environment template
│
├── backend/                 # Spring Boot 3.4.x, Java 25 LTS, Maven
│   ├── src/main/java/com/rightful/
│   │   ├── config/          # OpenAPI 3.1 & CORS configuration
│   │   ├── security/        # Spring Security & Supabase JWT verification
│   │   ├── common/          # Standard ApiResponse, Exceptions, CryptoUtils, HealthController
│   │   ├── identity/        # Supabase user synchronization & profile management
│   │   ├── workspace/       # Multi-tenant workspace isolation & RBAC
│   │   ├── product/         # Product catalog & Ed25519 key management
│   │   ├── plan/            # License plan definitions & quotas
│   │   ├── customer/        # Individual & organization customer entities
│   │   ├── licensing/       # Core license lifecycle & HMAC-SHA-256 hashing
│   │   ├── activation/      # Node-locking, device fingerprints & seat counts
│   │   ├── validation/      # Cryptographic validation & Ed25519 entitlement minting
│   │   ├── billing/         # [PROPOSED] Subscription billing webhooks
│   │   ├── audit/           # Immutable audit trails & validation telemetry
│   │   ├── integration/     # Outgoing developer webhooks & API keys
│   │   └── notification/    # Event alert delivery
│   ├── src/main/resources/  # application.yml, database migrations (Flyway)
│   └── .env.example         # Backend environment template
│
├── docs/architecture/       # Formal architecture specifications (11 documents)
│   ├── product-overview.md
│   ├── technology-stack.md
│   ├── system-architecture.md
│   ├── backend-modules.md
│   ├── database-design.md
│   ├── authentication-security.md
│   ├── api-specification.md
│   ├── workspace-authorization.md
│   ├── license-cryptography.md
│   ├── offline-validation.md
│   └── implementation-roadmap.md
│
├── infrastructure/          # Docker Compose and deployment manifests
│   └── docker/              # Local PostgreSQL container stack
│
├── .gitignore               # Multi-language Git exclusion rules
├── .env.example             # Monorepo root environment template
├── LICENSE                  # Apache License 2.0
└── README.md                # Project documentation
```

---

## Core Technical Specifications

### Technology Stack Matrix

| Layer | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Frontend Framework** | React | 19.x | Declarative component model |
| **Frontend Tooling** | Vite | 8.x | ESM bundler and ultra-fast dev server |
| **Frontend Language** | TypeScript | 5.9+ (Strict Mode) | End-to-end type safety |
| **Frontend Styling** | Tailwind CSS | v4 (`@tailwindcss/vite`) | Utility-first CSS engine |
| **Component Primitives**| shadcn/ui + Radix UI | Radix UI primitives | Accessible, composable UI building blocks |
| **Routing** | React Router | 7.x | Declarative client routing |
| **State & Cache** | TanStack Query | v5 | Asynchronous server-state management |
| **Form Management** | React Hook Form + Zod | Latest | Validated forms and runtime schema guards |
| **Backend Runtime** | Java SE | 25 LTS (build 25.0.2) | Enterprise runtime with modern JDK features |
| **Backend Framework** | Spring Boot | 3.4.x | Dependency injection, MVC, Actuator, Security |
| **Build & Packaging** | Apache Maven | 3.9+ | Declarative build system |
| **Persistence / ORM** | Spring Data JPA / Hibernate | 6.6.x | ACID transactions and repository abstractions |
| **Database Migrations** | Flyway | 10.x / 11.x | Version-controlled, idempotent DDL migrations |
| **Primary Database** | PostgreSQL (Supabase) | 15+ / 16+ | Managed relational database engine |
| **Identity & Auth** | Supabase Auth (Gotrue) | Hosted | Email/password, Google OAuth, GitHub OAuth |
| **Cryptography** | Ed25519 & HMAC-SHA-256 | Java Security / BouncyCastle | Entitlement signing & non-reversible key hashing |
| **API Documentation** | OpenAPI 3.1 / Swagger UI | springdoc-openapi 2.8.5 | Interactive API specification |

---

## Key Licensing Requirements & Product Policies

- **Asymmetric Entitlement Signing**: All license entitlements issued to client applications are signed using **Ed25519**. Client apps verify license integrity offline using the product's public key without exposing private signing keys.
- **HMAC-SHA-256 Key Hashing**: Plaintext license keys are returned only once upon generation. The database stores only a cryptographic `key_hash` using HMAC-SHA-256. Database compromises cannot expose valid license keys.
- **Heartbeat & Grace Cycle**:
  - Connected client applications submit an online heartbeat every **5 minutes** (`/api/v1/validation/heartbeat`).
  - If a client goes offline, it automatically transitions into an **offline grace period** defaulting to **7 hours**.
  - Grace periods are measured from the last successful validation timestamp and are configurable per product or license plan tier.
- **Workspace Isolation**: Multi-tenant data segregation ensures zero cross-tenant access. Every product, plan, customer, and license belongs strictly to a workspace.

---

## Getting Started: Local Development

### Prerequisites
- **Node.js**: v20+ (Tested on v24.15.0) and npm v10+
- **Java**: Java 25 LTS (Tested on Oracle JDK 25.0.2)
- **Maven**: 3.9+ (Tested on Apache Maven 3.9.15)
- **Docker**: (Optional) for running local PostgreSQL

### 1. Repository Setup & Environment Variables
Clone the repository and copy the environment configuration templates:
```bash
# Frontend environment
cp frontend/.env.example frontend/.env.local

# Backend environment
cp backend/.env.example backend/.env
```

### 2. Running Local Database (Optional)
If not connecting directly to a Supabase PostgreSQL instance:
```bash
cd infrastructure/docker
docker compose up -d
```

### 3. Running Backend (Spring Boot)
```bash
cd backend
mvn clean spring-boot:run
```
- API Base URL: `http://localhost:8080/api/v1`
- Health Endpoint: `http://localhost:8080/api/v1/health`
- Swagger UI Documentation: `http://localhost:8080/swagger-ui.html`
- OpenAPI Specification: `http://localhost:8080/v3/api-docs`

To execute backend automated tests:
```bash
cd backend
mvn clean test
```

### 4. Running Frontend (React + Vite)
```bash
cd frontend
npm install
npm run dev
```
- Frontend Application URL: `http://localhost:5173`

To execute frontend automated tests and linters:
```bash
cd frontend
npm test          # Run Vitest test suite
npm run lint      # Run ESLint checks
npm run build     # Execute TypeScript compiler and production Vite build
```

---

## Architecture Documentation Reference

Detailed architectural specifications and design records are maintained in `docs/architecture/`:

1. [Product Overview](docs/architecture/product-overview.md) – Personas, value propositions, and core capabilities.
2. [Technology Stack](docs/architecture/technology-stack.md) – In-depth stack justification and version matrix.
3. [System Architecture](docs/architecture/system-architecture.md) – Monolith topology and data flows.
4. [Backend Modules](docs/architecture/backend-modules.md) – Package-by-feature hierarchy and boundaries.
5. [Database Design](docs/architecture/database-design.md) – Entity relationships and storage guarantees.
6. [Authentication & Security](docs/architecture/authentication-security.md) – Supabase Auth integration & Spring Resource Server.
7. [API Specification](docs/architecture/api-specification.md) – REST conventions and endpoint blueprint.
8. [Workspace Authorization](docs/architecture/workspace-authorization.md) – Multi-tenant isolation & RBAC.
9. [License Cryptography](docs/architecture/license-cryptography.md) – Ed25519 signing and HMAC-SHA-256 hashing.
10. [Offline Validation](docs/architecture/offline-validation.md) – Heartbeat states and 7-hour offline grace period.
11. [Implementation Roadmap](docs/architecture/implementation-roadmap.md) – Progressive milestone deliverables.

---

## Confirmed Next Milestone

**Step 3.3 – Final Database Entity Mapping and Security Design**
- Creation of full Flyway relational migrations for business schemas (`workspaces`, `products`, `plans`, `customers`, `licenses`, `activations`, `audit_logs`).
- JPA Entity implementations and Spring Data repositories.
- Method-level workspace security evaluators and Supabase JWKS decoder integration.
