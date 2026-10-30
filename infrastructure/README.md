# Rightful – Infrastructure & Deployment

This directory contains container definitions, orchestration templates, and deployment scripts for Rightful.

## Directory Structure
- `docker/`: Local development Docker Compose services (PostgreSQL, PgAdmin, local mailcatcher).
- `k8s/`: [PROPOSED] Kubernetes manifests and Helm charts for production cluster deployments.
- `scripts/`: Operational scripts (database backup, key rotation, certificate management).

## Local Development Stack
For local development without an external Supabase instance, you can spin up a local PostgreSQL container:

```bash
cd infrastructure/docker
docker compose up -d
```
