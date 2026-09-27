# Networker

A personable take on professional networking. Sign in, connect with people you actually know, keep private notes about them, and schedule meetings on a shared calendar.

- `networkerUI/networker`: Angular 21 frontend (Tailwind, Auth0 SPA SDK)
- `networkerAPI`: Spring Boot 4 REST API (JPA, OAuth2 resource server)
- PostgreSQL: a managed database on DigitalOcean, with the schema created by Flyway migrations

## Auth0

The app is already configured for the Auth0 tenant `dev-meac52y4dgsxm7hb.us.auth0.com`:

- API `Networker API`, identifier (audience) `https://networker-api`
- Single Page Application `Networker`, with `http://localhost:4200` as its callback, logout and web-origin URL

The domain and client ID are in `networkerUI/networker/src/environments/environment.ts`, and the issuer is the default in `application.yaml`. Neither is secret.

To manage these settings, ask the tenant owner to add you in the [Auth0 dashboard](https://manage.auth0.com). To use your own tenant instead, create the same API and app there, then update `environment.ts` and set `AUTH0_ISSUER`.

## Database

Everyone shares one remote Postgres database on DigitalOcean (database `networks`, port 25060, SSL required). Before you run the API for the first time:

1. **Get `secrets.yaml`.** Ask a teammate for it privately, never through the repo or a public channel. Put it at `networkerAPI/src/main/resources/secrets.yaml`. It's gitignored and looks like this:

   ```yaml
   datasource:
       host: <database host>
       username: <database user>
       password: <database password>
   ```

2. **Add your IP to the trusted sources.** In DigitalOcean, go to **Databases → the cluster → Settings → Trusted sources**. Without this, the connection times out.

Anything in `src/main/resources` is packaged into the built jar, so don't share or deploy a jar built with `secrets.yaml` in place.

### Schema changes

Flyway owns the schema. Migrations live in `networkerAPI/src/main/resources/db/migration/` and run automatically when the API starts; each one runs once per database, and Flyway records it in the `flyway_schema_history` table. Hibernate only checks that the tables match the entities (`ddl-auto: validate`), and the API won't start if they don't.

When you change an entity, add a new migration named with the next version, e.g. `V2__add_meeting_notes.sql`. Never edit a migration that has already run; Flyway checksums applied migrations and refuses to start if one changes.

## Run it

Run each part in its own terminal, starting from the repo root:

```bash
# 1. API on :8080 (on Windows PowerShell, use .\mvnw.cmd instead of ./mvnw)
cd networkerAPI
./mvnw spring-boot:run

# 2. Frontend on :4200
cd networkerUI/networker
npm install
npm start
```

Open http://localhost:4200.

On first start, the API seeds five sample people who accept connection requests right away. That gives you someone to connect with, write notes about and invite to meetings. Set `DEMO_DATA=false` to turn this off.

## Configuration

Database settings come from `secrets.yaml` (see [Database](#database)). API environment variables (defaults in `application.yaml`):

| Variable | Default |
| --- | --- |
| `AUTH0_ISSUER` | `https://dev-meac52y4dgsxm7hb.us.auth0.com/` (keep the trailing slash) |
| `AUTH0_AUDIENCE` | `https://networker-api` |
| `CORS_ORIGINS` | `http://localhost:4200` |
| `DEMO_DATA` | `true` |

## API overview

All endpoints need an Auth0 access token for the API audience.

| | |
| --- | --- |
| `GET /api/me`, `PUT /api/me`, `POST /api/me/sync` | Your profile (created on first visit) |
| `PUT /api/me/resume` (multipart `file`), `DELETE /api/me/resume`, `GET /api/people/{id}/resume` | Your resume: one PDF up to 5 MB, visible only to you and your connections |
| `GET /api/people?q=`, `GET /api/people/{id}` | Search and view people |
| `GET/POST /api/connections`, `POST /api/connections/{id}/accept`, `DELETE /api/connections/{id}` | Connection requests |
| `GET/POST /api/people/{id}/notes`, `PUT/DELETE /api/notes/{id}`, `GET /api/notes/recent` | Private notes |
| `GET /api/meetings?from=&to=`, `POST /api/meetings`, `PUT/DELETE /api/meetings/{id}`, `GET /api/people/{id}/meetings` | Calendar |
| `GET /api/meetings/invitations`, `POST /api/meetings/{id}/accept`, `POST /api/meetings/{id}/decline` | Meeting invitations. Meetings can have several attendees (`attendeeIds`); each answers on their own. Declining takes you off the meeting, and if you were the last attendee it's cancelled |
| `GET /api/notifications`, `POST /api/notifications/read`, `POST /api/notifications/{id}/read` | Meeting changes made by the other person: invites, time/place/title changes, accepts, declines and cancellations |

## Tests

```bash
cd networkerAPI && ./mvnw test           # API flow tests against in-memory H2
cd networkerUI/networker && npx ng test   # frontend unit tests
```

## Deploying (networksconnect.online)

The site and API run on [Render](https://render.com), set up by [render.yaml](render.yaml). The database is the team's DigitalOcean Postgres.

| Address | Render service |
| --- | --- |
| `networksconnect.online`, `www.networksconnect.online` | `networker-web` (static Angular build) |
| `api.networksconnect.online` | `networker-api` (Docker, [networkerAPI/Dockerfile](networkerAPI/Dockerfile)) |

1. In Render, choose **New > Blueprint** and pick this repo. When asked, enter `DATASOURCE_HOST`, `DATASOURCE_USERNAME` and `DATASOURCE_PASSWORD` (the same values as `secrets.yaml`).
2. In DigitalOcean, if the database has trusted sources turned on, add the outbound IPs listed on the `networker-api` service's **Connect** tab.
3. In Porkbun DNS for `networksconnect.online`, delete the parking records and add:
   - `ALIAS` record, host blank, pointing to `networker-web.onrender.com` (or an `A` record to `216.24.57.1`)
   - `CNAME` record, host `www`, pointing to `networker-web.onrender.com`
   - `CNAME` record, host `api`, pointing to `networker-api.onrender.com`

   Use the actual `*.onrender.com` names Render shows, since it adds a suffix if a name is taken. Render issues the HTTPS certificates once DNS resolves.

Production builds read the API address from `networkerUI/networker/src/environments/environment.production.ts`. Auth0 already allows the domain.
