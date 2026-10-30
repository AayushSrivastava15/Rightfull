# Rightful – API Specification & Protocols

## 1. REST API Design Standards

Rightful exposes a production-grade RESTful API designed with predictable conventions:
- **Base URL**: `/api/v1`
- **Payload Format**: `application/json; charset=UTF-8`
- **Envelope Standard**: All endpoints return responses enclosed in an `ApiResponse<T>` wrapper.
- **Error Format**: RFC 7807 (Problem Details) compliant error payloads.
- **Idempotency**: Safe methods (`GET`, `HEAD`, `OPTIONS`) are idempotent. Critical state-mutating operations support an `Idempotency-Key` header.

---

## 2. Standard Envelope Formats

### 2.1 Success Envelope
```json
{
  "success": true,
  "data": { ... },
  "message": "Operation completed successfully",
  "timestamp": "2026-10-30T21:00:00Z"
}
```

### 2.2 Error Envelope (RFC 7807 Compatible)
```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "License with key prefix 'RF-A1B2' was not found",
    "details": [],
    "path": "/api/v1/licenses/RF-A1B2",
    "timestamp": "2026-10-30T21:00:00Z"
  }
}
```

---

## 3. High-Level Endpoint Catalog (Planned)

### 3.1 System & Observability
- `GET /api/v1/health` – Public application health, version, and runtime status.
- `GET /actuator/health` – Spring Boot Actuator health probe.
- `GET /v3/api-docs` – OpenAPI 3.1 JSON specification.
- `GET /swagger-ui/index.html` – Interactive Swagger UI.

### 3.2 Workspaces & Members (`/api/v1/workspaces`)
- `GET /api/v1/workspaces` – List workspaces the authenticated user belongs to.
- `POST /api/v1/workspaces` – Create a new tenant workspace.
- `GET /api/v1/workspaces/{workspaceId}` – Get workspace details and quota summary.
- `GET /api/v1/workspaces/{workspaceId}/members` – List workspace members and roles.
- `POST /api/v1/workspaces/{workspaceId}/members/invite` – Invite a new member by email.

### 3.3 Products & Plans (`/api/v1/workspaces/{workspaceId}/products`)
- `GET /api/v1/workspaces/{workspaceId}/products` – List registered products.
- `POST /api/v1/workspaces/{workspaceId}/products` – Register a new software product.
- `GET /api/v1/workspaces/{workspaceId}/products/{productId}` – Product details and cryptographic keys.
- `POST /api/v1/workspaces/{workspaceId}/products/{productId}/plans` – Create a license plan tier.

### 3.4 Customers (`/api/v1/workspaces/{workspaceId}/customers`)
- `GET /api/v1/workspaces/{workspaceId}/customers` – List customers (individuals & organizations).
- `POST /api/v1/workspaces/{workspaceId}/customers` – Create customer record.

### 3.5 Licenses (`/api/v1/workspaces/{workspaceId}/licenses`)
- `POST /api/v1/workspaces/{workspaceId}/licenses` – Generate/issue a new license key.
- `GET /api/v1/workspaces/{workspaceId}/licenses` – Filter and list licenses.
- `GET /api/v1/workspaces/{workspaceId}/licenses/{licenseId}` – License detail, seat allocation, activations.
- `POST /api/v1/workspaces/{workspaceId}/licenses/{licenseId}/revoke` – Revoke license immediately.
- `POST /api/v1/workspaces/{workspaceId}/licenses/{licenseId}/suspend` – Suspend license temporarily.
- `POST /api/v1/workspaces/{workspaceId}/licenses/{licenseId}/renew` – Renew license expiry.

### 3.6 Validation & Device Activation (Public Endpoints)
- `POST /api/v1/validation/verify` – Public license validation & entitlement minting.
- `POST /api/v1/validation/activate` – Register and lock a new device seat.
- `POST /api/v1/validation/deactivate` – Deactivate a device seat.
- `POST /api/v1/validation/heartbeat` – 5-minute recurring heartbeat keeping entitlement active.
