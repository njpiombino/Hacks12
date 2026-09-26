# Networker

A personable take on professional networking. Sign in, connect with people you actually know, keep private notes about them, and schedule meetings on a shared calendar.

- `networkerUI/networker`: Angular 21 frontend (Tailwind, Auth0 SPA SDK)
- `networkerAPI`: Spring Boot 4 REST API (JPA, OAuth2 resource server)
- PostgreSQL via `docker-compose.yml`

## Auth0

The app is already configured for the Auth0 tenant `dev-meac52y4dgsxm7hb.us.auth0.com`:

- API `Networker API`, identifier (audience) `https://networker-api`
- Single Page Application `Networker`, with `http://localhost:4200` as its callback, logout and web-origin URL

The domain and client ID are in `networkerUI/networker/src/environments/environment.ts`, and the issuer is the default in `application.yaml`. Neither is secret.

To manage these settings, ask the tenant owner to add you in the [Auth0 dashboard](https://manage.auth0.com). To use your own tenant instead, create the same API and app there, then update `environment.ts` and set `AUTH0_ISSUER`.

## Run it

Run each part in its own terminal, starting from the repo root:

```bash
# 1. Database (Postgres in Docker, on port 55432)
docker compose up -d

# 2. API on :8080
cd networkerAPI
./mvnw spring-boot:run

# 3. Frontend on :4200
cd networkerUI/networker
npm install
npm start
```

Open http://localhost:4200.

The database uses port 55432 rather than 5432 so it doesn't clash with any Postgres already installed on your machine. After a reboot, run `docker compose up -d` again.

On first start, the API seeds five sample people who accept connection requests right away. That gives you someone to connect with, write notes about and invite to meetings. Set `DEMO_DATA=false` to turn this off.

## Configuration

API environment variables (defaults in `application.yaml`):

| Variable | Default |
| --- | --- |
| `AUTH0_ISSUER` | `https://dev-meac52y4dgsxm7hb.us.auth0.com/` (keep the trailing slash) |
| `AUTH0_AUDIENCE` | `https://networker-api` |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | `jdbc:postgresql://localhost:55432/networker` / `networker` / `networker` |
| `CORS_ORIGINS` | `http://localhost:4200` |
| `DEMO_DATA` | `true` |

## API overview

All endpoints need an Auth0 access token for the API audience.

| | |
| --- | --- |
| `GET /api/me`, `PUT /api/me`, `POST /api/me/sync` | Your profile (created on first visit) |
| `GET /api/people?q=`, `GET /api/people/{id}` | Search and view people |
| `GET/POST /api/connections`, `POST /api/connections/{id}/accept`, `DELETE /api/connections/{id}` | Connection requests |
| `GET/POST /api/people/{id}/notes`, `PUT/DELETE /api/notes/{id}`, `GET /api/notes/recent` | Private notes |
| `GET /api/meetings?from=&to=`, `POST /api/meetings`, `PUT/DELETE /api/meetings/{id}`, `GET /api/people/{id}/meetings` | Calendar |

## Tests

```bash
cd networkerAPI && ./mvnw test           # API flow tests against in-memory H2
cd networkerUI/networker && npx ng test   # frontend unit tests
```
