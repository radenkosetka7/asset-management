# Asset Management - Fullstack Workspace

This repository contains a fullstack Asset Management system:

- `asset_management_api` - Spring Boot 3 backend (Java 21, PostgreSQL, Elasticsearch, Redis)
- `asset-management-ui` - Angular frontend
- `docker-compose.yml` - root orchestration for backend, frontend, and infrastructure

## Project Structure

```text
asset_management/
  docker-compose.yml
  asset_management_api/
  asset-management-ui/
```

## Prerequisites

- Docker + Docker Compose
- (Optional for local development) Java 21, Maven 3.9+, Node.js 22+

## Environment Variables

Create a root `.env` file in `asset_management/.env` (this file is ignored by git):

```env
# Database
POSTGRES_PASSWORD=change_me

# Elasticsearch
ELASTICSEARCH_PASSWORD=change_me

# JWT
SECRET_TOKEN=change_me
REFRESH_TOKEN_SECRET=change_me

# SSL (must match keystore password used for asset_management_api/src/main/resources/keystore.p12)
SSL_PASSWORD=change_me

# OAuth2
GOOGLE_CLIENT_ID=change_me
GOOGLE_CLIENT_SECRET=change_me
GITHUB_CLIENT_ID=change_me
GITHUB_CLIENT_SECRET=change_me
```

Important:

- Do not wrap values in quotes.
- `SSL_PASSWORD` must match the keystore password, otherwise backend startup fails.

## Run Everything with Docker

From repository root:

```powershell
docker compose up -d --build
```

Open:

- Frontend: `http://localhost:4200`
- Backend (HTTPS): `https://localhost:8443`

Useful commands:

```powershell
docker compose ps
docker compose logs -f backend
docker compose down
```

## Run Services Separately (Optional)

Start infrastructure only:

```powershell
docker compose up -d postgres elasticsearch redis
```

Run backend locally:

```powershell
Set-Location .\asset_management_api
mvn spring-boot:run
```

Run frontend locally:

```powershell
Set-Location .\asset-management-ui
npm install
npm run ng -- serve --host 0.0.0.0 --port 4200 --proxy-config proxy.conf.json
```

## Security and Git Hygiene

- `.gitignore` excludes all `.env` files recursively.
- Keep secrets only in local `.env` files.
- If any `.env` was committed in the past, remove it from git history before publishing externally.

## Troubleshooting

- `keystore password was incorrect`
  - Check `SSL_PASSWORD` in root `.env`.
  - Ensure it matches `asset_management_api/src/main/resources/keystore.p12` password.

- `Could not safely identify store assignment ... Redis repository`
  - These messages are informational in this project unless followed by runtime Redis connection errors.

## Additional Module Docs

- Backend details: `asset_management_api/README.md`
- Frontend details: `asset-management-ui/README.md`

