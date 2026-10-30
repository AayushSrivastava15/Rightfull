# Rightful – Technology Stack Specification

## 1. Overview

Rightful is structured as a modern monorepo combining a high-performance React TypeScript frontend with an enterprise-grade Java 25 / Spring Boot backend, supported by Supabase PostgreSQL and Supabase Auth.

---

## 2. Technology Stack Matrix

| Component Layer | Technology | Version / Specification | Rationale & Responsibility |
| :--- | :--- | :--- | :--- |
| **Frontend Framework** | React | 19.x | Declarative UI, modern concurrent features, component model. |
| **Build Tooling** | Vite | 8.x | Lightning-fast HMR, optimized ESM bundling, plugin ecosystem. |
| **Language (Web)** | TypeScript | 5.x / 7.x | Strict mode type safety, unified domain contracts. |
| **Styling & CSS** | Tailwind CSS | v4 (`@tailwindcss/vite`) | Modern CSS-first engine, zero config, ultra-fast compilation. |
| **Component Primitives**| shadcn/ui + Radix UI | Radix UI primitives | Accessible, unstyled primitives composed with Tailwind. |
| **Client Routing** | React Router | 7.x / 6.x | Client-side routing, nested layouts, loader boundaries. |
| **Data Fetching & Cache**| TanStack Query | v5 (`@tanstack/react-query`)| Server state caching, optimistic updates, background sync. |
| **Form Management** | React Hook Form | 7.x | Performant uncontrolled forms with reactive validation. |
| **Schema Validation** | Zod | 3.x | Runtime validation of API responses and form submissions. |
| **Icons & Motion** | Lucide React + Motion | Latest | Visual iconography and fluid micro-interactions. |
| **Backend Runtime** | Java SE | 25 LTS (Oracle JDK 25.0.2) | Enterprise runtime, modern virtual threads, pattern matching. |
| **Backend Framework** | Spring Boot | 3.4.x | Production-ready DI container, web MVC, actuator, security. |
| **Dependency Manager** | Apache Maven | 3.9.x | Declarative builds, reproducible dependencies, multi-profile. |
| **Security & Auth** | Spring Security 6.x | OAuth2 Resource Server | Supabase JWT signature verification via remote JWKS endpoint. |
| **Persistence (ORM)** | Spring Data JPA / Hibernate | 6.6.x | Relational mapping, repository abstractions, transactional boundaries. |
| **Database Migrations** | Flyway | 10.x / 11.x | Version-controlled, idempotent SQL migrations. |
| **Database Engine** | PostgreSQL (Supabase) | 15.x / 16.x | ACID compliance, JSONB attributes, row-level security readiness. |
| **Identity Provider** | Supabase Auth | Hosted Gotrue | Email/password, Google OAuth, GitHub OAuth, JWT token issuance. |
| **API Documentation** | OpenAPI 3.1 / Swagger UI | springdoc-openapi 2.8.x | Interactive API documentation, client generation. |
| **Cryptography** | Ed25519 & HMAC-SHA-256 | Java Security / BouncyCastle | Entitlement token asymmetric signing, secure key hashing. |

---

## 3. Decision Records & Justifications

### 3.1 Backend: Spring Boot on Java 25 LTS
- **Decision**: Standardize on Spring Boot 3.4+ running on Java 25 LTS.
- **Justification**: Java 25 LTS provides long-term stability, performance optimizations (Project Loom virtual threads, Project Panama foreign function interface, structured concurrency), and strong type systems. Spring Boot provides battle-tested production scaffolding.

### 3.2 Frontend: React + Vite + Tailwind CSS v4
- **Decision**: Avoid full-stack meta-frameworks like Next.js for the core management console.
- **Justification**: Rightful's primary frontend is a high-security administrative SaaS dashboard and developer portal. A single-page application (SPA) backed by a dedicated Spring Boot REST API provides clear separation of concerns, independent deployment cadences, and eliminates server-side rendering vulnerabilities and Node.js proxy bottlenecks.

### 3.3 Authentication: Supabase Auth + Spring Boot Resource Server
- **Decision**: Offload authentication to Supabase Auth while maintaining application authorization inside Spring Boot.
- **Justification**: Supabase Auth provides turnkey support for social providers (Google, GitHub) and email/password with secure session refreshes. Spring Boot verifies tokens via Supabase JWKS asynchronously and maps the `sub` claim to internal user profiles and workspace roles.

