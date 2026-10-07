# Quick Start Guide - Splitwise Backend

## 5-Minute Setup

### Step 1: Prerequisites Check
Ensure you have:
- Java 17+ installed: `java -version`
- MySQL running on `localhost:3306`
- Git (optional)

### Step 2: Database Setup (1 minute)
Open MySQL and run:
```sql
CREATE DATABASE splitwise_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Step 3: Environment Configuration (1 minute)
Edit `.env` file in project root:
```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=splitwise_db
DB_USERNAME=root
DB_PASSWORD=root                    # Change if needed

APP_PORT=8080
JWT_SECRET=splitwise_secret_key_2024
JWT_EXPIRATION=86400000
```

### Step 4: Build & Run (3 minutes)

**Windows:**
```bash
gradlew.bat clean build
gradlew.bat bootRun
```

**Linux/Mac:**
```bash
./gradlew clean build
./gradlew bootRun
```

### Step 5: Verify Installation
Open browser and go to:
```
http://localhost:8080/api/auth/login
```

If you see "401 Unauthorized", the server is running!

---

## First API Call - Register & Login

### Register a User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice",
    "email": "alice@example.com",
    "password": "password123",
    "phone": "9876543210"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "type": "Bearer",
    "userId": 1,
    "email": "alice@example.com",
    "name": "Alice"
  },
  "statusCode": 201
}
```

**Save the token!** You'll need it for other requests.

### Register Another User (Friend)
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Bob",
    "email": "bob@example.com",
    "password": "password123",
    "phone": "9876543211"
  }'
```

---

## Create Your First Group & Expense

### 1. Create a Group
```bash
TOKEN="your_token_from_registration"

curl -X POST http://localhost:8080/api/groups \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Movie Night",
    "description": "Expense tracking for movie and dinner",
    "memberIds": [2]
  }'
```

Save the `groupId` from response!

### 2. Add an Expense
Alice paid ₹300 for 2 people (Alice & Bob):

```bash
TOKEN="alice_token"

curl -X POST http://localhost:8080/api/expenses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "groupId": 1,
    "amount": 300,
    "description": "Movie tickets and dinner",
    "participantIds": [1, 2],
    "splitType": "EQUAL"
  }'
```

**What happens automatically:**
- ✅ Expense recorded: Alice paid ₹300
- ✅ Split calculated: ₹150 each (equal split)
- ✅ Settlement created: Bob owes Alice ₹150
- ✅ Database updated

### 3. Check Settlement
Bob can see he owes ₹150:

```bash
BOB_TOKEN="bob_token"

curl -X GET "http://localhost:8080/api/settlements/group/1/user/pending" \
  -H "Authorization: Bearer $BOB_TOKEN"
```

**Response:**
```json
{
  "success": true,
  "message": "Pending settlements fetched successfully",
  "data": [
    {
      "settlementId": 1,
      "groupId": 1,
      "debtor": {
        "userId": 2,
        "name": "Bob",
        "email": "bob@example.com"
      },
      "creditor": {
        "userId": 1,
        "name": "Alice",
        "email": "alice@example.com"
      },
      "amount": 150,
      "status": "PENDING"
    }
  ],
  "statusCode": 200
}
```

### 4. Bob Pays Back
Bob marks the settlement as paid:

```bash
BOB_TOKEN="bob_token"

curl -X POST http://localhost:8080/api/settlements/1/pay \
  -H "Authorization: Bearer $BOB_TOKEN"
```

**Result:** Settlement status changes to "PAID" ✅

---

## Project Structure Overview

```
splitwise-backend/
├── src/main/java/com/splitwise/
│   ├── controller/          ← REST endpoints
│   ├── service/             ← Business logic
│   ├── model/               ← Database entities
│   ├── repository/          ← Data access
│   ├── dto/                 ← Request/Response objects
│   ├── security/            ← JWT & Auth
│   ├── config/              ← Spring config
│   └── exception/           ← Error handling
├── src/main/resources/
│   └── application.properties
├── build.gradle             ← Dependencies
├── gradlew & gradlew.bat    ← Build scripts
├── .env                     ← Configuration
├── README.md                ← Full docs
└── API_DOCUMENTATION.md     ← API reference
```

---

## Common Commands

### Build
```bash
# Clean and build
./gradlew clean build

# Build without tests
./gradlew build -x test
```

### Run
```bash
# Run application
./gradlew bootRun

# Run with specific port
./gradlew bootRun --args='--server.port=9090'
```

### Clean
```bash
# Remove build artifacts
./gradlew clean
```

### Dependencies
```bash
# Show dependency tree
./gradlew dependencies

# Update dependencies
./gradlew build --refresh-dependencies
```

---

## Troubleshooting

### Database Connection Error
**Error:** `Cannot connect to database`

**Solution:**
1. Check MySQL is running: `mysql -u root -p`
2. Verify `.env` credentials match MySQL setup
3. Check port 3306 is accessible

### Port 8080 Already in Use
**Error:** `Address already in use`

**Solution:**
1. Kill process: `lsof -ti:8080 | xargs kill -9` (Mac/Linux)
2. Or change port in `.env`: `APP_PORT=9090`

### JWT Token Invalid
**Error:** `401 Unauthorized`

**Solution:**
1. Make sure token is still valid (check expiration time)
2. Use "Bearer " prefix: `Authorization: Bearer <token>`
3. Get new token by logging in again

### Build Fails
**Error:** `Gradle build failed`

**Solution:**
```bash
# Clear gradle cache
./gradlew clean

# Use wrapper
rm -rf .gradle
./gradlew build --refresh-dependencies
```

---

## Next Steps

1. **Read Full Documentation:** See [README.md](README.md)
2. **API Reference:** See [API_DOCUMENTATION.md](API_DOCUMENTATION.md)
3. **Database Schema:** Check entity models in `src/main/java/com/splitwise/model/`
4. **Add Frontend:** Create React/Angular frontend and connect to these APIs
5. **Deploy:** Deploy to AWS/Heroku with production `.env` values

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                    Client/Frontend                  │
└────────────────────┬────────────────────────────────┘
                     │ HTTP/REST
┌────────────────────▼────────────────────────────────┐
│              Spring Boot Application                │
├─────────────────────────────────────────────────────┤
│  Controllers → Services → Repositories → Database   │
├─────────────────────────────────────────────────────┤
│     JWT Security | Exception Handling | CORS        │
└────────────────────┬────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────┐
│              MySQL Database                         │
│  ┌─────────────┬──────────┬──────────┬────────────┐ │
│  │   users     │  groups  │ expenses │ settlements│ │
│  └─────────────┴──────────┴──────────┴────────────┘ │
└─────────────────────────────────────────────────────┘
```

---

## Database Schema (Auto-Generated)

The application automatically creates these tables:
- `users` - User accounts
- `groups` - Expense groups
- `group_members` - Group membership (many-to-many)
- `expenses` - Individual expenses
- `expense_splits` - Split details for each expense
- `expense_participants` - Participants in each expense
- `settlements` - Payment settlements between users

---

## Support

- Check logs for errors: Look at console output
- Database issues: Connect to MySQL directly and check tables
- API issues: See API_DOCUMENTATION.md for request/response formats
- Security: Never expose JWT_SECRET in `.env.local` or version control

Happy expense tracking! 🎉

test commit
<!-- test commit -->