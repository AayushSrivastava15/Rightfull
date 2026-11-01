# Rightfull – Database Connection Setup & Verification (Step 3.5)

## 1. Overview & Architecture

Rightful uses **Supabase PostgreSQL** as its managed database engine. In local development and cloud production, the Spring Boot backend connects to Supabase via the **Supabase Session Pooler** (port 5432) over TLS/SSL (`sslmode=require`).

Automated unit, slice, and integration tests execute against an isolated, in-memory **H2 database in PostgreSQL mode**, strictly guaranteeing that testing operations never interact with or pollute the live development database.

---

## 2. Configuration Responsibilities

Configuration is managed strictly using Java Properties (`.properties`). YAML files are prohibited.

```
backend/src/main/resources/
├── application.properties             # Shared baseline (server, Actuator, Flyway defaults, HikariCP pool)
├── application-local.properties       # Local profile (Supabase PostgreSQL via environment variables)
├── application-test.properties        # Test profile (isolated in-memory H2, zero network calls)
└── application.properties.example     # Clean template for version control (no secrets)
```

### 2.1 File Responsibilities

1. **`application.properties` (Shared Baseline)**:
   - Application identity (`spring.application.name=rightfull-backend`).
   - Server port 8080 and GZIP compression.
   - Common HikariCP pool parameters (pool size 10, min idle 2, timeouts, connection test query `SELECT 1`).
   - JPA/Hibernate defaults (`spring.jpa.hibernate.ddl-auto=none`, `open-in-view=false`).
   - Common Flyway defaults (`locations=classpath:db/migration`, baseline on migrate, execution disabled by default).
   - Spring Boot Actuator health and metrics exposure.
   - Optional local `.env` loader (`spring.config.import=optional:file:.env[.properties]`).

2. **`application-local.properties` (Local Development Profile)**:
   - Connects strictly to Supabase PostgreSQL using `org.postgresql.Driver`.
   - Reads connection parameters exclusively from environment variables: `${DATABASE_URL}`, `${DATABASE_USER}`, `${DATABASE_PASSWORD}`.
   - Contains **no fallback credentials** that silently connect to H2.
   - Requires PostgreSQL SSL (`sslmode=require`).
   - HikariCP pool name: `RightfullLocalHikariPool`.

3. **`application-test.properties` (Automated Test Profile)**:
   - Configures isolated in-memory H2 database (`jdbc:h2:mem:rightfull_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`).
   - Disables Flyway migrations during tests.
   - Enforces `ddl-auto=none`.
   - Never touches live Supabase credentials or networks.

---

## 3. Required Environment Variables

To run Rightfull locally with the `local` profile, configure the following variables in `backend/.env` (which is excluded by `.gitignore`) or in your system environment:

| Variable Name | Required? | Example Value | Description |
| :--- | :---: | :--- | :--- |
| `DATABASE_URL` | **Yes** | `jdbc:postgresql://aws-0-[region].pooler.supabase.com:5432/postgres?sslmode=require` | Supabase Session Pooler JDBC URL. |
| `DATABASE_USER`| **Yes** | `rightfull_app.[project-ref]` | Runtime application user (least-privilege, `NOBYPASSRLS`). |
| `DATABASE_PASSWORD`| **Yes** | `[your-app-password]` | Runtime application user password. |
| `FLYWAY_ENABLED`| No | `true` | Flyway migration toggle. |
| `FLYWAY_USER` | No | `postgres.[project-ref]` | Migration administrator user with DDL privileges. |
| `FLYWAY_PASSWORD` | No | `[your-admin-password]` | Migration administrator password. |
| `DATABASE_DRIVER` | No | `org.postgresql.Driver` | JDBC driver class name. |
| `SPRING_PROFILES_ACTIVE` | No | `local` | Active Spring profile (defaults to `local`). |
| `PORT` | No | `8080` | HTTP server port. |

### 3.1 Template Reference
A sanitized template is provided at [`backend/.env.example`](file:///d:/RIGHTFUL.com/backend/.env.example):
```bash
cp backend/.env.example backend/.env
# Edit backend/.env with your Supabase pooler credentials
```

---

## 4. Supabase Session Pooler Details

Supabase provides two connection pooler modes on AWS infrastructure:
1. **Transaction Pooler (Port 6543)**: Intended for short-lived, stateless queries and serverless workers. Does not support prepared statements or session-level state.
2. **Session Pooler (Port 5432)**: Recommended for persistent application servers using HikariCP and Spring Data JPA. Fully supports prepared statement caching, transaction boundaries, and advisory locks.

### Connection String Format
```
jdbc:postgresql://aws-0-[region].pooler.supabase.com:5432/postgres?sslmode=require
```

- **User**: `postgres.[project-ref]` (The project reference prefix is mandatory when connecting via Supabase Pooler).
- **SSL**: `sslmode=require` ensures all network traffic between the backend and Supabase is encrypted with TLS.

---

## 5. Running the Backend & Verifying Connectivity

### 5.1 Starting the Application Locally
From the `backend/` directory:
```powershell
.\mvnw.cmd spring-boot:run
```

### 5.2 Automated Connection Verification (`DatabaseConnectionVerifier`)
Rightful includes an automated startup verifier ([`DatabaseConnectionVerifier.java`](file:///d:/RIGHTFUL.com/backend/src/main/java/com/rightfull/config/DatabaseConnectionVerifier.java)) that implements Spring Boot's `ApplicationRunner`:
1. Borrows a live JDBC connection from `RightfullLocalHikariPool`.
2. Inspects `DatabaseMetaData` and detects database vendor and version.
3. Executes validation query: `SELECT 1`.
4. Logs confirmation:
   ```
   INFO c.r.c.DatabaseConnectionVerifier - Database connectivity verified successfully: PostgreSQL (Version: 17.11)
   ```
5. Never exposes credentials, hosts, or passwords in application logs.

### 5.3 Health Endpoint Verification
While the backend is running:

1. **Application Health**:
   ```powershell
   Invoke-RestMethod -Uri "http://localhost:8080/api/v1/health"
   # Returns: {"status":"UP","application":"rightfull-backend"}
   ```

2. **Actuator Observability & Database Health**:
   ```powershell
   Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"
   # Returns:
   # {
   #   "status": "UP",
   #   "components": {
   #     "db": {
   #       "status": "UP",
   #       "details": { "database": "PostgreSQL", "validationQuery": "SELECT 1", "result": 1 }
   #     }
   #   }
   # }
   ```

---

## 6. Test Database Isolation

All automated tests run under the `test` profile (`@ActiveProfiles("test")`):
- `H2` is scoped exclusively to `<scope>test</scope>` in `pom.xml`.
- Tests run against in-memory `jdbc:h2:mem:rightfull_test`.
- The `DatabaseConnectionVerifier` executes during test startup and confirms:
  ```
  INFO c.r.c.DatabaseConnectionVerifier - Database connectivity verified successfully: H2 (Version: 2.4.240 (2025-09-22))
  ```
- Because H2 is scoped to `test`, production and local runs cannot accidentally select H2.

---

## 7. Flyway Migration Policy & Step 3.6 Core Baseline

- **Current Status (Step 3.6)**: `spring.flyway.enabled=true` (enabled in local profile; disabled in test profile).
- **Migration Location**: `backend/src/main/resources/db/migration/`
- **Applied Migration**: `V1__init_core_schema.sql` (checksum `-850693739`, successfully applied to Supabase PostgreSQL 17.11).
- **History Table**: `public.flyway_schema_history`
- **Hibernate DDL Policy**: `spring.jpa.hibernate.ddl-auto=none`. Hibernate will never generate or alter tables automatically.
- **Dependencies**: Spring Boot 4 modular starter `org.springframework.boot:spring-boot-starter-flyway` + `org.flywaydb:flyway-database-postgresql`.
- **Idempotency Verified**: On subsequent application startups, Flyway validates existing migrations without re-running scripts.

---

## 8. Security Precautions

1. **Zero Secret Commitment**:
   - `application*.properties`, `.env`, and `*.env` are strictly excluded in [`.gitignore`](file:///d:/RIGHTFUL.com/.gitignore).
   - Only sanitized `.example` templates are tracked.
2. **Log Redaction**: Database passwords and connection URLs containing credentials are never logged.
3. **Fail-Fast Configuration**: If `DATABASE_URL` is omitted in the local profile, Spring Boot fails immediately with an unresolvable placeholder exception rather than silently falling back to an in-memory database.

