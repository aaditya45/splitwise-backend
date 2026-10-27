# Deployment Guide — From Local Project to AWS Production

This guide takes the Splitwise backend (Spring Boot + PostgreSQL, built with Gradle) from
"runs on my laptop" to a dev and prod deployment on AWS. Each section says **what** you do,
**why** it exists, and **how**. Read Part 1 first: it explains the ideas the rest depends on.

---

## Part 1 — The big picture (concepts first)

### 1.1 What changes between "local" and "production"?

| On your laptop | In production |
|---|---|
| You run `./gradlew bootRun` | The app runs as a packaged, immutable **artifact** (a Docker image) |
| Postgres runs in a Docker container you started | Postgres is a **managed database** (AWS RDS) with backups |
| Settings live in `.env` | Settings come from **environment variables** and a **secrets store** |
| `http://localhost:8088` | `https://api.yourdomain.com` behind a load balancer |
| One copy, if it crashes it stays down | Several copies, restarted automatically |
| You watch the terminal | Logs are collected centrally (CloudWatch) |

Everything below is about closing those gaps.

### 1.2 The tools, and what each is for

| Tool / service | Purpose (in one line) |
|---|---|
| **Gradle** | Compiles the code and packages it into one runnable file: `splitwise-1.0.0.jar` |
| **Docker** | Wraps the jar + Java into an **image** that runs identically everywhere |
| **Spring profiles** | Lets one codebase behave differently in `dev` vs `prod` (logging, DB, etc.) |
| **Liquibase** | Creates/updates database tables automatically on app start (already in the project) |
| **AWS ECR** | A private storage place for your Docker images |
| **AWS ECS Fargate** | Runs your container(s) for you, restarts them if they fail. No servers to manage |
| **AWS RDS (PostgreSQL)** | A managed Postgres: backups, patching, failover handled by AWS |
| **AWS Secrets Manager** | Stores passwords and the JWT secret safely, injects them into the container |
| **AWS ALB (load balancer)** | Single public entry point; spreads traffic across containers; does HTTPS |
| **AWS ACM** | Free TLS (HTTPS) certificates for the load balancer |
| **Route 53** | DNS: maps `api.yourdomain.com` to the load balancer |
| **CloudWatch** | Collects logs and metrics; used for alarms |
| **VPC + Security Groups** | Your private network and its firewall rules |
| **IAM** | Permissions: who/what may do what in AWS |
| **GitHub Actions** | Automation: on each push, build, test, push image, deploy |

### 1.3 The flow in one picture

```
Developer ──git push──> GitHub ──Actions──> build jar ─> Docker image ─> ECR
                                                                          │
                                                       ECS pulls image ◄──┘
Internet ─HTTPS─> Route53 ─> ALB ─> ECS Fargate (2+ containers of your app)
                                         │            │
                              Secrets Manager     RDS PostgreSQL (private)
                                         │
                                  CloudWatch (logs)
```

### 1.4 Key ideas to understand

- **Build once, deploy everywhere.** The *same* Docker image goes to dev and prod. Only
  configuration (environment variables) differs. Never rebuild a different image for prod.
- **Config is not code.** Passwords, DB hosts, and secrets are never baked into the image or
  committed to git. The app reads them from environment variables at start-up.
- **Containers are disposable.** A container can be killed and replaced at any time, so the
  app must keep no important state on its own disk. This app is **stateless** (JWT, no
  sessions), and all data lives in Postgres, so it is a good fit.
- **Schema changes go through Liquibase**, not Hibernate (`ddl-auto: none` in this project).
  New tables or columns = a new SQL file in `db/changelog/` plus an entry in
  `changelog-master.yml`. Never edit an already-applied changelog file.

---

## Part 2 — Prepare the code (do this before any AWS work)

These fixes matter because the project was written for a laptop. Each one has a reason.

### 2.1 Stop `.env` leaking and crashing

**Problem A:** `SplitwiseApplication.java` calls `Dotenv.load()`. If there is no `.env` file
(there won't be in Docker/AWS), the app **crashes at start-up**.
**Problem B:** it copies `.env` values into JVM system properties, and Spring gives system
properties **priority over real environment variables**. A stray `.env` could silently
override your prod settings.
**Problem C:** `.env` is committed to git with a real DB password and JWT secret.

Fix:
1. In `SplitwiseApplication.java` use `Dotenv.configure().ignoreIfMissing().load()`
   (or remove the `.env` loading and use plain environment variables everywhere).
2. Add `.env` to `.gitignore`, run `git rm --cached .env`, and commit a `.env.example`
   containing fake values only.
3. **Rotate** the exposed secrets: anything that was in git history must be treated as
   public. Use a new DB password and a new JWT secret in every real environment.

### 2.2 Spring profiles: one codebase, two behaviours

Create two files next to `src/main/resources/application.yml`:

`application-dev.yml`
```yaml
spring:
  jpa:
    show-sql: true
logging:
  level:
    com.splitwise: DEBUG
```

`application-prod.yml`
```yaml
spring:
  jpa:
    show-sql: false
logging:
  level:
    root: WARN
    com.splitwise: INFO
```

Select a profile with the environment variable `SPRING_PROFILES_ACTIVE=dev` or `prod`.
Then remove `show-sql: true` and the DEBUG level from the base `application.yml`, and
remove the **default value of `JWT_SECRET`** there so the app refuses to start if the
secret is missing (safer than silently using a public default).

### 2.3 Add a health endpoint

The load balancer needs a URL to ask "is this container alive?".
1. Add to `build.gradle`: `implementation 'org.springframework.boot:spring-boot-starter-actuator'`
2. In `SecurityConfig.java`, allow it: `.requestMatchers("/api/auth/**", "/actuator/health").permitAll()`
3. In `application.yml`, expose only health:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
```

### 2.4 Fix CORS for real frontends

`SecurityConfig` currently allows only `http://localhost:3000` and `http://localhost:8080`.
A deployed frontend would be blocked by the browser. Read the allowed origins from an
environment variable (for example `CORS_ALLOWED_ORIGINS`) instead of hard-coding them, and
set it to your real frontend URL per environment. Never use `*` together with credentials.

### 2.5 Housekeeping
- `h2` is declared `runtimeOnly`. Production uses Postgres, so remove it (or limit it to tests).
- Build uses Java 17 (`sourceCompatibility = '17'`). The Docker image should use Java 17 too.
- Remove the leftover Windows path `maven { url = uri('C:\\Users\\...') }` from `build.gradle`.

### 2.6 Dockerfile

Create `Dockerfile` in the project root:

```dockerfile
# ---- Stage 1: build the jar ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY . .
RUN ./gradlew clean bootJar -x test --no-daemon

# ---- Stage 2: small runtime image ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd -r -u 1001 appuser
COPY --from=build /src/build/libs/splitwise-1.0.0.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","app.jar"]
```

Why this way:
- **Multi-stage**: the final image holds only the JRE and the jar, not the JDK or source
  code. Smaller and safer.
- **Non-root user**: if the app is compromised, the attacker has fewer rights.
- **`MaxRAMPercentage`**: makes the JVM respect the container's memory limit.
- Add a `.dockerignore` with `build/`, `.git/`, `.gradle/`, `.env` so secrets and junk never
  enter the image.

(`bootJar` makes the runnable jar; the `-plain.jar` next to it is not runnable.)

### 2.7 Test it locally the way prod will run it

```bash
docker build -t splitwise-backend .
docker run --rm -p 8088:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e DB_HOST=host.docker.internal -e DB_PORT=5433 \
  -e DB_NAME=splitwise_db -e DB_USERNAME=postgres -e DB_PASSWORD=<local password> \
  -e JWT_SECRET=<any long random string> \
  splitwise-backend
```
If this works, you've proven the image does not depend on your laptop.

---

## Part 3 — AWS setup, step by step

Use one region throughout (for example `ap-south-1`, Mumbai). Use the AWS Console for the
first time to learn what each piece is; later you can codify it (Terraform / CloudFormation).

> **Do not disturb other projects.** If this AWS account also hosts other work, give every
> Splitwise resource a distinct name and the tag `project=splitwise`, and create its own
> security groups. Never edit shared resources.

### Step 1 — Account hygiene (IAM)
- Don't work as the root user. Create an IAM user/role with only the access you need and
  turn on MFA.
- **Why:** root can do anything, including billing and deleting the account. A mistake or
  stolen key at root level is catastrophic.
- Set a **billing budget alert** (Billing → Budgets) so a mistake can't run up a big bill.

### Step 2 — Network (VPC and security groups)
- The default VPC is fine to start. Know that a VPC is your private network, split into
  **subnets** across at least two availability zones (separate data centres).
- Create three **security groups** (firewalls):
  | Group | Allows inbound |
  |---|---|
  | `splitwise-alb-sg` | 443 (and 80 for redirect) from anywhere |
  | `splitwise-app-sg` | 8080 **only from** `splitwise-alb-sg` |
  | `splitwise-db-sg` | 5432 **only from** `splitwise-app-sg` |
- **Why:** this chain means the database is unreachable from the internet and the app is
  reachable only through the load balancer. Layered defence.

### Step 3 — Database (RDS PostgreSQL)
- Create an RDS instance: engine PostgreSQL 16, size `db.t4g.micro` for dev (bigger for prod),
  **Public access: No**, security group `splitwise-db-sg`, automated backups on
  (7+ days for prod), encryption on, initial database name `splitwise_db`.
- Prod: enable **Multi-AZ** (a standby copy in another zone; automatic failover).
- Create **separate** instances for dev and prod. Never let dev touch prod data.
- **Why RDS rather than Postgres in a container:** the container's disk is disposable.
  RDS gives durable storage, backups, point-in-time restore, and patching.
- The tables are created automatically by Liquibase the first time the app starts.

### Step 4 — Secrets (Secrets Manager)
- Create secrets for each environment, for example `splitwise/prod/db-password` and
  `splitwise/prod/jwt-secret`.
- Generate the JWT secret locally: `openssl rand -base64 64`.
- **Why:** secrets stay out of git, images, and task definitions. IAM controls who can read
  them, and you can rotate them without rebuilding.

### Step 5 — Image registry (ECR)
- Create a **private** repository `splitwise-backend`.
- Push the image (replace `ACCOUNT` and `REGION`):
```bash
aws ecr get-login-password --region REGION | docker login --username AWS --password-stdin ACCOUNT.dkr.ecr.REGION.amazonaws.com
docker build --platform linux/amd64 -t splitwise-backend .
docker tag splitwise-backend:latest ACCOUNT.dkr.ecr.REGION.amazonaws.com/splitwise-backend:v1
docker push ACCOUNT.dkr.ecr.REGION.amazonaws.com/splitwise-backend:v1
```
- **Important on Apple Silicon:** your Mac is arm64. Fargate defaults to x86-64, so either
  build with `--platform linux/amd64` (above) or set the Fargate task to ARM64.
- **Tag images with a version or git commit**, not just `latest`, so you can roll back.

### Step 6 — Run the container (ECS Fargate)
- Create an ECS **cluster**, then a **task definition** (the recipe for your container):
  - Image: your ECR image and tag. Port: 8080.
  - CPU/memory: 0.5 vCPU / 1 GB to start.
  - Environment variables: `SPRING_PROFILES_ACTIVE=prod`, `DB_HOST` (RDS endpoint),
    `DB_PORT=5432`, `DB_NAME`, `DB_USERNAME`, `JWT_EXPIRATION`, `CORS_ALLOWED_ORIGINS`.
  - Secrets (from Secrets Manager): `DB_PASSWORD`, `JWT_SECRET`.
  - Logs: send to CloudWatch (`awslogs` driver).
- **Roles:** the *task execution role* lets ECS pull from ECR and read the secrets; the
  *task role* is what your app itself may do in AWS (none needed now).
- Create a **service**: desired count 2 for prod (1 for dev), attached to the private/app
  subnets and `splitwise-app-sg`. The service replaces failed containers automatically.

### Step 7 — Load balancer and HTTPS
- Create an **Application Load Balancer** in the public subnets with `splitwise-alb-sg`.
- Request a free certificate in **ACM** for `api.yourdomain.com` (validate via DNS).
- Listener 443 (HTTPS) forwards to a **target group** (port 8080, health check path
  `/actuator/health`). Listener 80 redirects to 443.
- **Why:** HTTPS protects passwords and JWT tokens in transit; the ALB also lets you add
  containers without changing the public address.

### Step 8 — DNS (Route 53)
- Create an A (alias) record `api.yourdomain.com` → the ALB.
- Use different names per environment: `api-dev.yourdomain.com`, `api.yourdomain.com`.

### Step 9 — Verify the deployment
```bash
curl https://api.yourdomain.com/actuator/health          # expect {"status":"UP"}
curl -X POST https://api.yourdomain.com/api/auth/register -H 'Content-Type: application/json' \
  -d '{"name":"Test","email":"test@example.com","password":"<test password>","phone":"9000000000"}'
```
Then log in, run the group → expense → settlement flow once, check the logs in CloudWatch,
and delete the test data.

---

## Part 4 — Dev vs Prod environments

| | Dev | Prod |
|---|---|---|
| Purpose | Try changes safely, share with testers | Real users |
| Profile | `dev` | `prod` |
| Database | Own small RDS, may be reset | Own RDS, Multi-AZ, backups, deletion protection |
| Containers | 1 | 2 or more |
| Secrets | Separate values | Separate, stronger values |
| Domain | `api-dev.…` | `api.…` |
| Deploy trigger | Automatic on push | Manual approval |
| Logging | Verbose | INFO/WARN |

Rules: same image in both; different config; different data; different secrets. Promote a
change by testing in dev, then deploying the *same image tag* to prod.

---

## Part 5 — CI/CD (automate it)

Doing Steps 5–6 by hand every release is slow and error-prone. Use GitHub Actions.

Pipeline on each push to `main`:
1. Check out code, set up Java 17.
2. `./gradlew build` (runs tests; fails the pipeline if broken).
3. Log in to AWS using **OIDC** (a temporary role GitHub assumes). **Do not** store
   long-lived AWS access keys in GitHub.
4. Build the image, tag it with the commit SHA, push to ECR.
5. Update the ECS task definition with the new image and deploy to **dev**.
6. A manual approval step (GitHub *Environments* with required reviewers), then deploy to **prod**.

ECS performs a **rolling deployment**: it starts new containers, waits for the health check
to pass, then stops the old ones, so there is no downtime. A failed health check means the
new version never receives traffic.

---

## Part 6 — Operating it after launch

- **Logs:** CloudWatch Logs → your log group. Search for errors by time.
- **Alarms:** CPU/memory high, ALB 5xx errors, unhealthy targets, RDS free storage low,
  RDS connections high. Send them to email or Slack.
- **Rollback:** redeploy the previous image tag (this is why tags matter). Remember database
  changes from Liquibase are *not* undone by redeploying old code; design migrations to be
  backward compatible.
- **Backups:** RDS automated backups; practise a restore at least once.
- **Scaling:** add an ECS service auto-scaling rule on CPU; scale RDS vertically first.
- **Updates:** keep dependencies patched (Spring Boot, base image) and rebuild regularly.
- **Cost control:** stop or delete dev when unused; use budgets; right-size after measuring.

---

## Part 7 — Production checklist

- [ ] No secrets in git; old exposed secrets rotated
- [ ] `.env` untracked; `.env.example` committed
- [ ] `Dotenv` no longer crashes without `.env`
- [ ] Prod profile active; SQL logging off; no default JWT secret
- [ ] CORS limited to the real frontend origin
- [ ] Health endpoint works and is the ALB check
- [ ] Database private, encrypted, backed up, deletion protection on
- [ ] Security groups follow the ALB → app → DB chain
- [ ] HTTPS only; HTTP redirects to HTTPS
- [ ] Image tagged by version; rollback tested
- [ ] Alarms and log access in place
- [ ] Separate dev and prod stacks and secrets

---

## Part 8 — Cheaper / simpler alternatives (for learning)

- **One EC2 instance**: install Docker, run the container, put Nginx or Caddy in front for
  HTTPS, use RDS for the database. Fewer AWS services to learn, but you patch and monitor
  the server yourself and have no automatic failover.
- **Elastic Beanstalk**: upload the jar, AWS sets up instances and a load balancer. Less
  control, faster to start.
- **App Runner**: point it at the ECR image; it handles HTTPS and scaling. Simplest option
  for a small API.

Learning path suggestion: Part 2 locally → deploy to a single dev stack by hand in the
Console (Part 3) → add CI/CD (Part 5) → only then build prod.

---

## Glossary
- **Image**: a frozen package of your app + runtime. **Container**: a running image.
- **Task / Service (ECS)**: a task is one running container group; a service keeps N tasks alive.
- **Profile**: a named set of Spring settings (`dev`, `prod`).
- **Health check**: a URL the platform polls to decide if a container may receive traffic.
- **Rolling deployment**: replacing containers gradually so the app stays up.
- **OIDC**: lets GitHub prove its identity to AWS without stored passwords.
- **Subnet / AZ**: a slice of the network / a separate physical data-centre area.
