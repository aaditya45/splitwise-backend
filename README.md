# Splitwise Backend

A Spring Boot REST API for sharing group expenses: users register, create groups, add expenses split equally between members, and settle up. It backs the Expo app in `splitwise-app` and sends "expense added" events to a separate notification service.

| Doc | What it covers |
|---|---|
| [QUICKSTART.md](QUICKSTART.md) | Step-by-step setup on **macOS and Windows**, a first API flow, and running the frontend |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layers, request lifecycle, security, data model, configuration, notifications |
| [docs/FLOWS.md](docs/FLOWS.md) | Every business flow step by step, with sequence diagrams and the settlement algorithm |
| [API_DOCUMENTATION.md](API_DOCUMENTATION.md) | Every endpoint: request, response, access rules, errors |

## Tech stack

- Java 17+ (tested on 21), Spring Boot 3.5 (Web, Security, Data JPA, Validation)
- PostgreSQL 16, schema managed by Liquibase
- JWT auth (jjwt 0.12, HS512), BCrypt passwords
- Lombok, dotenv-java for `.env` loading
- Gradle 8.5 via the wrapper (no Gradle install needed); the build has no Gradle 9 deprecations

## Quick start

### 1. Start PostgreSQL

```bash
docker run -d --name splitwise-postgres -p 5432:5432 -e POSTGRES_DB=splitwise_db -e POSTGRES_PASSWORD=<choose-a-password> postgres:16
```

After the first run, start it again with `docker start splitwise-postgres`. Tables are created by Liquibase when the app starts; you don't run any SQL yourself.

### 2. Create `.env` in the project root

```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=splitwise_db
DB_USERNAME=postgres
DB_PASSWORD=<the password from step 1>

APP_PORT=8088

# At least 64 characters (HS512)
JWT_SECRET=<long random string>
JWT_EXPIRATION=86400000

# Notification service (optional for local work, see below)
NOTIFICATION_ENABLED=false
NOTIFICATION_BASE_URL=http://localhost:8090
NOTIFICATION_INTERNAL_API_KEY=<key shared with the notification service>
```

The app reads `.env` from the directory it is started in, so always start it from the project root.

### 3. Run

```bash
./gradlew bootRun
```

Wait for `Started SplitwiseApplication`; the API is on `http://localhost:8088`.

On Windows use `.\gradlew.bat bootRun`. [QUICKSTART.md](QUICKSTART.md) has every step for both systems.

**From IntelliJ:** open the folder as a Gradle project, set the project SDK and Gradle JVM to JDK 17+, reload Gradle, then run `SplitwiseApplication.main`. Keep the run configuration's working directory at `$PROJECT_DIR$` so `.env` is found.

### 4. Check it's up

```bash
curl -i -X POST http://localhost:8088/api/auth/login -H 'Content-Type: application/json' -d '{"email":"x@y.z","password":"wrong"}'
```

`401` with `"Invalid credentials"` means the API and database are both working.

### Running with the frontend

The backend allows browser requests (CORS) from `http://localhost:3000` and `http://localhost:8080` only. Run the Expo app's web build on port 3000 with `EXPO_PUBLIC_API_URL=http://localhost:8088`:

```bash
npx expo start --web --port 3000
```

A phone running Expo Go doesn't go through CORS, but needs your computer's LAN IP instead of `localhost`.

## Project structure

```
src/main/java/com/splitwise/
├── SplitwiseApplication.java   entry point; loads .env into system properties
├── config/                     security (JWT filter chain, CORS, 401 handler), notification client + async executor, .env loader
├── controller/                 REST endpoints: Auth, User, Group, Expense, Settlement
├── service/                    business rules: membership checks, splits, balances/settlements, notification client
├── repository/                 Spring Data JPA repositories
├── model/                      JPA entities: User, Group, Expense, ExpenseSplit, Settlement
├── dto/                        response records; request/ (validated inputs); response/ (ApiResponse envelope, AuthResponse)
├── security/                   JwtTokenProvider, JwtAuthenticationFilter, UserDetails implementation
├── event/                      ExpenseCreatedEvent + async listener that notifies group members
└── exception/                  GlobalExceptionHandler, NotFoundException (404), ForbiddenException (403)

src/main/resources/
├── application.yml             datasource, JPA, Liquibase, JWT, notification and logging settings
└── db/changelog/               Liquibase master changelog + P0/ddl (tables) and P0/dml (optional seed users, disabled)
```

## Key behaviour

- **Every endpoint except `/api/auth/**` needs `Authorization: Bearer <token>`.** Missing or invalid tokens get `401`.
- **Groups are private to their members.** Viewing a group, its expenses or settlements, adding a member, and adding an expense all require the caller to already be a member (`403` otherwise).
- **Splits are equal only.** Leftover paise from rounding go to participants in user-ID order, so splits always add up to the expense amount. Any `splitType` other than `EQUAL` is rejected.
- **Settlements are recomputed from scratch** after each expense is added or deleted: pending ones are deleted and regenerated from current balances; paid ones are kept and counted.
- **Only the debtor or creditor** can mark a settlement paid, and only once.
- **Notifications are fire-and-forget.** After an expense commits, other group members are notified asynchronously; a slow or down notification service never fails or slows the request.

## Build

```bash
./gradlew build           # compile + package
./gradlew bootJar         # executable jar in build/libs/
java -jar build/libs/splitwise-1.0.0.jar
```

There are currently no automated tests (`src/test` doesn't exist).

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `Connection to localhost:5432 refused` on startup | Docker or the Postgres container isn't running: `docker start splitwise-postgres` |
| `Port 8088 already in use` | Another copy is running: `lsof -i :8088`, stop it |
| Every request returns `401` | Token missing/expired, or `JWT_SECRET` changed since the token was issued; log in again |
| Browser shows a CORS error | Frontend isn't on `localhost:3000` or `localhost:8080`; see [Running with the frontend](#running-with-the-frontend) |
| `403 You are not a member of this group` | Expected: a member has to add the user first |
| Gradle sync fails with `URISyntaxException ... C:\Users\...` | Old `build.gradle`; current version uses `mavenLocal()` |
| Lombok getters "cannot find symbol" in IntelliJ | Enable annotation processing (Settings → Build → Compiler → Annotation Processors) |

## Known limitations

- Only equal splits; no percentage or itemised splits yet.
- No endpoint to remove a member, leave a group, edit an expense, or delete a group.
- Settlements are per creditor in a simple greedy pass, not minimised across the group.
