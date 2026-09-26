Tech stack:
Angular typescript frontend
Java Springboot backend
Postgresql database

App:
Networking web app where you sign into your profile and can connect with other profiles. The focus of the app is to build your network similar to LinkedIn but in a much more personable way. You should be able to write notes about your connections and schedule meetings in a calender. Use Auth0 to create a login for users. The app should feel professional and handmade.



~~~
I've built the Networker app with Angular, Spring Boot, Postgres and Auth0, as `claude.md` asks. The backend tests (4) and frontend tests (2) pass, and the frontend builds. I haven't run the whole thing in a browser: Auth0 login needs your tenant details, and Docker wasn't running, so I couldn't try it against real Postgres. The backend tests use mock logins and an in-memory database.

**What it does**
- **Sign in** with Auth0. There's a welcome page for signed-out visitors, and your profile is created from your Auth0 name, email and photo the first time you log in.
- **Connections**: search people, send requests, and accept, decline or withdraw them. Email addresses are only shown to people you're connected with.
- **Private notes** on anyone. They look like index cards, only you can see them, and you can edit or delete them.
- **Calendar**: a month view with the selected day's meetings beside it. You can schedule a meeting with a connection, and it shows up on their calendar too; only you, as organizer, can change it.
- **Home page** with upcoming meetings, requests waiting on you, your connections and recent notes. There's also a profile editor with a live preview.
- **Sample data**: the API adds five sample people who accept requests right away, so a fresh install has someone to try things with. Set `DEMO_DATA=false` to turn this off.

For the "professional and handmade" feel, I used warm paper colours, a Fraunces serif for headings, a hand-drawn underline on the active nav link, and small handwritten labels.

**Where things are**
- **Backend** (`networkerAPI`): the main code is in `domain/`, `service/`, `web/` and `config/`, under `src/main/java/com/goat/demo/`. [ApiFlowTests.java](networkerAPI/src/test/java/com/goat/demo/ApiFlowTests.java) tests connecting, notes, meetings and privacy.
- **Frontend** (`networkerUI/networker/src/app`): shared services in `core/`, one folder per screen in `pages/`, the avatar and meeting dialog in `shared/`, and the layout in [shell.html](networkerUI/networker/src/app/shell.html). Colours and fonts are in [styles.css](networkerUI/networker/src/styles.css).
- [docker-compose.yml](docker-compose.yml) runs Postgres, and [README.md](README.md) has the setup steps.

**To run it**
1. In Auth0, create an API with the identifier `https://networker-api`, and a Single Page Application with `http://localhost:4200` as its callback, logout and web-origin URL.
2. Put the app's domain and client ID in [environment.ts](networkerUI/networker/src/environments/environment.ts).
3. Start Postgres with `docker compose up -d`.
4. Start the API with `AUTH0_ISSUER=https://<tenant>/ ./mvnw spring-boot:run`, keeping the trailing slash.
5. Start the frontend with `npm start`.

**Other changes**
- I deleted the placeholder Angular page and its test.
- I raised the bundle size warning in `angular.json` from 500kB to 700kB, because the Auth0 library pushes the app over the old limit.
- The database tables are created automatically at startup, which is fine for a hackathon but not for production.

Nothing is committed yet.