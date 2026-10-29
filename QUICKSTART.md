# Quick Start — macOS and Windows

Get the backend running locally, make your first API calls, and optionally start the Expo frontend against it. Each step has a **macOS** (Terminal, zsh) and a **Windows** (PowerShell) version.

For what's happening under the hood, see [docs/FLOWS.md](docs/FLOWS.md); for every endpoint, [API_DOCUMENTATION.md](API_DOCUMENTATION.md).

## 1. Install the prerequisites

You need **JDK 17 or newer**, **Docker Desktop** and **Git**. **Node.js 20+** is only needed for the frontend. You don't need to install Gradle: the repo's wrapper (`gradlew` / `gradlew.bat`) downloads the right version itself.

**macOS** (with [Homebrew](https://brew.sh)):

```bash
brew install --cask temurin@21 docker
brew install git node
```

**Windows** (PowerShell, using winget, which ships with Windows 10/11):

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install Docker.DockerDesktop
winget install Git.Git
winget install OpenJS.NodeJS.LTS
```

Open a **new** terminal afterwards so `PATH` picks up the new tools, then check:

```bash
java -version
docker --version
```

Start **Docker Desktop** and wait until it says *Engine running*. On Windows, accept the WSL 2 setup if it asks, and restart if prompted.

## 2. Start PostgreSQL

Pick a database password; you'll put it in `.env` next. The command is the same on both systems.

```bash
docker run -d --name splitwise-postgres -p 5432:5432 -e POSTGRES_DB=splitwise_db -e POSTGRES_PASSWORD=<choose-a-password> postgres:16
```

This only needs to run once. After a reboot, start the existing container with:

```bash
docker start splitwise-postgres
```

You don't create any tables: Liquibase does that when the app first starts.

## 3. Create `.env`

In the project root (the folder with `build.gradle`), create a file named exactly `.env`:

```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=splitwise_db
DB_USERNAME=postgres
DB_PASSWORD=<the password from step 2>

APP_PORT=8088
APP_ENV=development

JWT_SECRET=<a random string of at least 64 characters>
JWT_EXPIRATION=86400000

NOTIFICATION_ENABLED=false

LOG_LEVEL=DEBUG
```

To generate a `JWT_SECRET`:

**macOS**

```bash
openssl rand -base64 64 | tr -d '\n'; echo
```

**Windows**

```powershell
[Convert]::ToBase64String([byte[]](1..64 | ForEach-Object { Get-Random -Maximum 256 }))
```

> **Windows:** if you create the file in Notepad, choose *Save as type: All files* so it isn't saved as `.env.txt`, and keep the encoding *UTF-8*. To check, run `Get-ChildItem -Force .env` in the project folder.

`NOTIFICATION_ENABLED=false` is fine for local work: it just skips the "expense added" notifications, which need the separate notification service on port 8090.

## 4. Run the backend

From the project root (the app reads `.env` from the folder you start it in):

**macOS**

```bash
./gradlew bootRun
```

**Windows**

```powershell
.\gradlew.bat bootRun
```

The first run downloads Gradle and the dependencies, so it takes a few minutes. It's ready when you see:

```
Started SplitwiseApplication in … seconds
```

The terminal stays busy while the server runs; press **Ctrl+C** to stop it. Use a second terminal for the next steps.

**From IntelliJ IDEA instead** (both systems): *File → Open* the project folder as a Gradle project, set *Project Structure → SDK* and *Settings → Build Tools → Gradle → Gradle JVM* to your JDK 21, reload Gradle, then run `SplitwiseApplication`. Keep the run configuration's working directory at `$PROJECT_DIR$`.

## 5. Check it's up

Send a login with a wrong password; a `401 Invalid credentials` means the API and database are both working.

**macOS**

```bash
curl -i -X POST http://localhost:8088/api/auth/login -H 'Content-Type: application/json' -d '{"email":"x@y.z","password":"wrong"}'
```

**Windows**

```powershell
try {
  Invoke-RestMethod -Method Post -Uri http://localhost:8088/api/auth/login -ContentType 'application/json' -Body '{"email":"x@y.z","password":"wrong"}'
} catch { $_.Exception.Response.StatusCode.value__ }
```

Expected output on Windows: `401`.

> **Windows:** in PowerShell, `curl` is an alias for `Invoke-WebRequest` and doesn't accept these flags. Use `Invoke-RestMethod` as shown, or `curl.exe` explicitly.

## 6. Your first flow

Two users split a ₹1000 hotel bill, and Bob settles up.

**macOS** (needs `jq`: `brew install jq`)

```bash
API=http://localhost:8088

A=$(curl -s -X POST $API/api/auth/register -H 'Content-Type: application/json' -d '{"name":"Alice","email":"alice@example.com","password":"secret123"}')
B=$(curl -s -X POST $API/api/auth/register -H 'Content-Type: application/json' -d '{"name":"Bob","email":"bob@example.com","password":"secret123"}')
TA=$(echo "$A" | jq -r .data.token); AID=$(echo "$A" | jq .data.userId)
TB=$(echo "$B" | jq -r .data.token); BID=$(echo "$B" | jq .data.userId)

GID=$(curl -s -X POST $API/api/groups -H "Authorization: Bearer $TA" -H 'Content-Type: application/json' -d "{\"name\":\"Goa trip\",\"memberIds\":[$BID]}" | jq .data.groupId)

curl -s -X POST $API/api/expenses -H "Authorization: Bearer $TA" -H 'Content-Type: application/json' -d "{\"groupId\":$GID,\"amount\":1000,\"description\":\"Hotel\",\"participantIds\":[$AID,$BID]}" | jq '.data.splits[] | {user: .user.name, amount}'

SID=$(curl -s $API/api/settlements/group/$GID/user/pending -H "Authorization: Bearer $TB" | jq '.data[0].settlementId')
curl -s -X POST $API/api/settlements/$SID/pay -H "Authorization: Bearer $TB" | jq .message

curl -s $API/api/settlements/group/$GID/summary -H "Authorization: Bearer $TA" | jq '.data.balances[] | {user: .user.name, balance}'
```

**Windows** (PowerShell; no extra tools needed)

```powershell
$api = 'http://localhost:8088'
function Call($method, $path, $token, $body) {
  $headers = @{}; if ($token) { $headers.Authorization = "Bearer $token" }
  $json = if ($body) { $body | ConvertTo-Json -Depth 5 } else { $null }
  (Invoke-RestMethod -Method $method -Uri "$api$path" -Headers $headers -ContentType 'application/json' -Body $json).data
}

$alice = Call Post /api/auth/register $null @{ name = 'Alice'; email = 'alice@example.com'; password = 'secret123' }
$bob   = Call Post /api/auth/register $null @{ name = 'Bob';   email = 'bob@example.com';   password = 'secret123' }

$group = Call Post /api/groups $alice.token @{ name = 'Goa trip'; memberIds = @($bob.userId) }

$expense = Call Post /api/expenses $alice.token @{ groupId = $group.groupId; amount = 1000; description = 'Hotel'; participantIds = @($alice.userId, $bob.userId) }
$expense.splits | ForEach-Object { "$($_.user.name): $($_.amount)" }

$pending = Call Get "/api/settlements/group/$($group.groupId)/user/pending" $bob.token
Call Post "/api/settlements/$($pending[0].settlementId)/pay" $bob.token

(Call Get "/api/settlements/group/$($group.groupId)/summary" $alice.token).balances | ForEach-Object { "$($_.user.name): $($_.balance)" }
```

Both versions end with Alice and Bob at a balance of `0`: the bill was split 500 / 500, and Bob's 500 is marked paid. Running it twice fails at register with `Email already registered`; change the emails to try again.

## 7. Run the frontend (optional)

The Expo app lives in `splitwise-app` next to this repo. The backend only accepts browser requests from `localhost:3000` and `localhost:8080`, so run the web build on port 3000.

**macOS**

```bash
cd ../splitwise-app
printf 'EXPO_PUBLIC_API_URL=http://localhost:8088\nEXPO_PUBLIC_CURRENCY_CODE=INR\n' > .env.local
npm install
npx expo start --web --port 3000
```

**Windows**

```powershell
cd ..\splitwise-app
Set-Content -Path .env.local -Value "EXPO_PUBLIC_API_URL=http://localhost:8088`nEXPO_PUBLIC_CURRENCY_CODE=INR"
npm install
npx expo start --web --port 3000
```

Open `http://localhost:3000`, create an account, and you're in. On a phone with Expo Go, use your computer's LAN IP instead of `localhost` in `EXPO_PUBLIC_API_URL`; the phone and computer must be on the same Wi-Fi.

## Everyday commands

| Task | macOS | Windows |
|---|---|---|
| Start the database | `docker start splitwise-postgres` | same |
| Stop the database | `docker stop splitwise-postgres` | same |
| Run the backend | `./gradlew bootRun` | `.\gradlew.bat bootRun` |
| Build a jar | `./gradlew bootJar` | `.\gradlew.bat bootJar` |
| Run the jar | `java -jar build/libs/splitwise-1.0.0.jar` | `java -jar build\libs\splitwise-1.0.0.jar` |
| Open a DB shell | `docker exec -it splitwise-postgres psql -U postgres -d splitwise_db` | same |
| Find what's on port 8088 | `lsof -i :8088` | `netstat -ano \| findstr :8088` |
| Kill it | `kill <PID>` | `taskkill /PID <PID> /F` |

## Troubleshooting

| Problem | Fix |
|---|---|
| `Connection to localhost:5432 refused` | Docker Desktop isn't running, or the container is stopped: `docker start splitwise-postgres` |
| `docker: command not found` / `docker` isn't recognised | Docker Desktop isn't installed or hasn't finished starting; on Windows open a new terminal after installing |
| `docker start` just hangs | The Docker engine is still starting; wait for *Engine running* in Docker Desktop |
| `Could not find or load main class org.gradle.wrapper.GradleWrapperMain` | Your checkout predates the wrapper fix; pull the latest code (it includes `gradle/wrapper/gradle-wrapper.jar`) |
| macOS: `permission denied: ./gradlew` | Same cause; pull the latest code, or run `chmod +x gradlew` once |
| Windows: `.\gradlew.bat` "is not recognised" | You're not in the project root; `cd` to the folder with `build.gradle` |
| `Port 8088 was already in use` | Another copy is running; find and kill it (see *Everyday commands*) |
| Startup fails reading `.env`, or the DB password is wrong | Check `.env` is in the project root, named exactly `.env` (not `.env.txt`), and you started the app from that folder |
| `password authentication failed for user "postgres"` | `DB_PASSWORD` doesn't match the container's. If you changed it, recreate the container: `docker rm -f splitwise-postgres`, then step 2 again (this deletes local data) |
| Gradle sync fails with `URISyntaxException … C:\Users\…` | Pull the latest `build.gradle`; it now uses `mavenLocal()` |
| Every API call returns `401` | Missing `Authorization: Bearer <token>`, the token expired (24 h), or `JWT_SECRET` changed; log in again |
| `403 You are not a member of this group` | Expected: a member has to add that user to the group first |
| Browser shows a CORS error | The frontend isn't on `localhost:3000`; start it with `--port 3000` |
| Windows: `curl` complains about `-X` or `-H` | Use `curl.exe` or `Invoke-RestMethod` (see step 5) |
