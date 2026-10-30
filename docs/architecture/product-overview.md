# Rightful – Product Overview

## 1. Executive Summary

**Rightful** is an open-source, free-to-start, multi-tenant SaaS software licensing management platform. It is engineered to give independent developers, freelancers, startups, and software enterprises complete control over software monetization, entitlement verification, and license enforcement without sacrificing user privacy or developer autonomy.

Rightful bridges the gap between complex proprietary licensing infrastructure (e.g., FlexLM, Nalpeiron) and fragmented ad-hoc licensing code by delivering a unified, developer-first platform featuring cryptographically signed entitlements, offline validation, and multi-tenant isolation.

---

## 2. Target Audience & Personas

1. **Independent Developers & Indie Hackers**:
   - Building desktop apps (Electron, Tauri, Qt, C#/.NET), CLI tools, or on-premise SaaS agents.
   - Require a frictionless, zero-cost onboarding tier with straightforward API integration.
2. **Freelancers & Micro-Agencies**:
   - Delivering bespoke software or plugins to multiple clients with distinct seat and domain constraints.
3. **Mid-Market & Enterprise Software Vendors**:
   - Requiring multi-tenant workspace isolation, role-based access control, automated seat management, and cryptographic auditability.

---

## 3. Core Value Propositions

- **Cryptographic Independence**: Entitlements are signed with asymmetric cryptography (Ed25519). Client applications can verify license validity offline using public keys without round-tripping to central servers for every execution.
- **Fail-Safe Offline Resilience**: Built-in, configurable offline grace periods (defaulting to 7 hours) prevent sudden user lockouts during network disruptions or air-gapped field operations.
- **Strict Multi-Tenancy**: Data isolation at the workspace level ensures that organizations, teams, and products operate in secure silos.
- **Hybrid Online/Offline Lifecycle**: Active applications heart-beat every 5 minutes when connected to sync entitlements, revoke compromised keys immediately, and reset offline grace timers.
- **Zero Raw Key Storage**: License keys are hashed using HMAC-SHA-256 prior to persistence, ensuring that database breaches cannot leak plaintext license secrets.

---

## 4. Key Functional Capabilities

| Feature Domain | Capability Description |
| :--- | :--- |
| **Product Management** | Register software products, define releases, channels, and client SDK configurations. |
| **Plan Architecture** | Configure tiered license plans (features, seats, grace period, renewal cycles). |
| **License Issuance** | Generate cryptographic license keys for individuals (named users) and organizations. |
| **Device & Seat Management** | Control device fingerprints, max concurrent seats, node-locking, and activations. |
| **Entitlement Engine** | Issue Ed25519 cryptographically signed JSON Web Entitlements (JWE/JWT tokens). |
| **Validation & Heartbeats** | 5-minute online heartbeats with automated fallback to 7-hour offline grace periods. |
| **Lifecycle Operations** | Revocation, suspension, automatic expiration, seat reclamation, and renewal. |
| **Audit & Telemetry** | Immutable audit logs for validation attempts, device registrations, and plan changes. |
| **Integration & SDKs** | RESTful public APIs, developer webhooks, and client-side verification SDKs. |

---

## 5. Architectural Principles

- **Production-Grade Engineering**: Strict separation of concerns, high test coverage, deterministic builds, and comprehensive API documentation.
- **Modular Monolith**: Package-by-feature backend architecture, ensuring clean boundaries between modules while avoiding distributed systems overhead.
- **Zero Credential Exposure**: Supabase Auth handles identity and token issuance; Spring Boot acts as an OAuth2 Resource Server validating JWTs via JWKS without storing user passwords.
- **Open Standards**: Ed25519 signatures, HMAC-SHA-256 key hashing, OpenAPI 3.1 specifications, and PostgreSQL ACID guarantees.

