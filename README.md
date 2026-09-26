# Networker

A personable take on professional networking. Sign in, connect with people you actually know, keep private notes about them, and schedule meetings on a shared calendar.

- `networkerUI/networker`: Angular 21 frontend (Tailwind, Auth0 SPA SDK)
- `networkerAPI`: Spring Boot 4 REST API (JPA, OAuth2 resource server)
- PostgreSQL via `docker-compose.yml`

## 1. Set up Auth0 (one time)

In the [Auth0 dashboard](https://manage.auth0.com):

1. **Applications → APIs → Create API.** Name it `Networker API` and set the Identifier to `https://networker-api`. This is the audience.
2. **Applications → Applications → Create Application → Single Page Application.** In its settings, set:
   - Allowed Callback URLs: `http://localhost:4200`
   - Allowed Logout URLs: `http://localhost:4200`
   - Allowed Web Origins: `http://localhost:4200`
3. Copy the app's **Domain** and **Client ID** into `networkerUI/networker/src/environments/environment.ts`.

## 2. Run it

```bash
# Database
docker compose up -d

# API on :8080 (the trailing slash on the issuer matters)
cd networkerAPI
AUTH0_ISSUER=https://YOUR_TENANT.us.auth0.com/ ./mvnw spring-boot:run

# Frontend on :4200
cd networkerUI/networker
npm install
npm start
```

Open http://localhost:4200.

On first start, the API seeds five sample people who accept connection requests right away. That gives you someone to connect with, write notes about and invite to meetings. Set `DEMO_DATA=false` to turn this off.

## Configuration

API environment variables (defaults in `application.yaml`):

| Variable | Default |
| --- | --- |
| `AUTH0_ISSUER` | `https://YOUR_TENANT.us.auth0.com/` |
| `AUTH0_AUDIENCE` | `https://networker-api` |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/networker` / `networker` / `networker` |
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
| `GET /api/meetings?from=&to=`, `POST/PUT/DELETE /api/meetings/{id}`, `GET /api/people/{id}/meetings` | Calendar |

## Tests

```bash
cd networkerAPI && ./mvnw test           # API flow tests against in-memory H2
cd networkerUI/networker && npx ng test   # frontend unit tests
```
