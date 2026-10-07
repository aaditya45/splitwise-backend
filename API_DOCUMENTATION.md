# Splitwise API Documentation

## Base URL
```
http://localhost:8080/api
```

## Authentication
All endpoints (except `/auth/**`) require Bearer token in Authorization header:
```
Authorization: Bearer <jwt_token>
```

---

## Authentication Endpoints

### 1. Register User
```
POST /auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "securePassword123",
  "phone": "+91-9876543210"
}

Response (201 Created):
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "type": "Bearer",
    "userId": 1,
    "email": "john@example.com",
    "name": "John Doe"
  },
  "statusCode": 201
}
```

### 2. Login User
```
POST /auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "securePassword123"
}

Response (200 OK):
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "type": "Bearer",
    "userId": 1,
    "email": "john@example.com",
    "name": "John Doe"
  },
  "statusCode": 200
}
```

---

## User Endpoints

### 1. Get Current User Profile
```
GET /users/profile
Authorization: Bearer <token>

Response (200 OK):
{
  "success": true,
  "message": "User profile fetched successfully",
  "data": {
    "userId": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "phone": "+91-9876543210",
    "profilePicture": null
  },
  "statusCode": 200
}
```

### 2. Get User by ID
```
GET /users/{userId}
Authorization: Bearer <token>

Response (200 OK):
{
  "success": true,
  "message": "User fetched successfully",
  "data": {
    "userId": 2,
    "name": "Jane Smith",
    "email": "jane@example.com",
    "phone": "+91-9876543211",
    "profilePicture": null
  },
  "statusCode": 200
}
```

---

## Group Endpoints

### 1. Create Group
```
POST /groups
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Trip to Goa",
  "description": "Summer vacation expense group",
  "groupImage": null,
  "memberIds": [2, 3, 4]
}

Response (201 Created):
{
  "success": true,
  "message": "Group created successfully",
  "data": {
    "groupId": 1,
    "name": "Trip to Goa",
    "description": "Summer vacation expense group",
    "groupImage": null,
    "createdBy": {
      "userId": 1,
      "name": "John Doe",
      "email": "john@example.com",
      "phone": "+91-9876543210",
      "profilePicture": null
    },
    "members": [
      {
        "userId": 1,
        "name": "John Doe",
        "email": "john@example.com",
        "phone": "+91-9876543210",
        "profilePicture": null
      },
      {
        "userId": 2,
        "name": "Jane Smith",
        "email": "jane@example.com",
        "phone": "+91-9876543211",
        "profilePicture": null
      }
    ],
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  },
  "statusCode": 201
}
```

### 2. Get Group Details
```
GET /groups/{groupId}
Authorization: Bearer <token>

Response (200 OK):
{
  "success": true,
  "message": "Group fetched successfully",
  "data": {
    "groupId": 1,
    "name": "Trip to Goa",
    "description": "Summer vacation expense group",
    "groupImage": null,
    "createdBy": { ... },
    "members": [ ... ],
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  },
  "statusCode": 200
}
```

### 3. Get User's Groups
```
GET /groups/user/groups
Authorization: Bearer <token>

Response (200 OK):
{
  "success": true,
  "message": "User groups fetched successfully",
  "data": [
    {
      "groupId": 1,
      "name": "Trip to Goa",
      ...
    },
    {
      "groupId": 2,
      "name": "Monthly Groceries",
      ...
    }
  ],
  "statusCode": 200
}
```

### 4. Add Member to Group
```
POST /groups/{groupId}/members/{userId}
Authorization: Bearer <token>

Response (200 OK):
{
  "success": true,
  "message": "Member added successfully",
  "statusCode": 200
}
```

---

## Expense Endpoints

### 1. Create Expense
```
POST /expenses
Authorization: Bearer <token>
Content-Type: application/json

{
  "groupId": 1,
  "amount": 300,
  "description": "Hotel booking",
  "participantIds": [1, 2, 3],
  "splitType": "EQUAL"
}

Response (201 Created):
{
  "success": true,
  "message": "Expense created successfully",
  "data": {
    "expenseId": 1,
    "groupId": 1,
    "paidBy": {
      "userId": 1,
      "name": "John Doe",
      "email": "john@example.com",
      "phone": "+91-9876543210",
      "profilePicture": null
    },
    "amount": 300,
    "description": "Hotel booking",
    "expenseDate": "2024-01-15T10:35:00",
    "participants": [
      {
        "userId": 1,
        "name": "John Doe",
        ...
      },
      {
        "userId": 2,
        "name": "Jane Smith",
        ...
      },
      {
        "userId": 3,
        "name": "Mike Johnson",
        ...
      }
    ],
    "splits": [
      {
        "splitId": 1,
        "user": { "userId": 1, ... },
        "amount": 100,
        "splitType": "EQUAL"
      },
      {
        "splitId": 2,
        "user": { "userId": 2, ... },
        "amount": 100,
        "splitType": "EQUAL"
      },
      {
        "splitId": 3,
        "user": { "userId": 3, ... },
        "amount": 100,
        "splitType": "EQUAL"
      }
    ],
    "createdAt": "2024-01-15T10:35:00",
    "updatedAt": "2024-01-15T10:35:00"
  },
  "statusCode": 201
}
```

### 2. Get Expense Details
```
GET /expenses/{expenseId}
Authorization: Bearer <token>

Response (200 OK):
Same as Create Expense response
```

### 3. Get Group Expenses
```
GET /expenses/group/{groupId}
Authorization: Bearer <token>

Response (200 OK):
{
  "success": true,
  "message": "Group expenses fetched successfully",
  "data": [
    { ... expense 1 ... },
    { ... expense 2 ... }
  ],
  "statusCode": 200
}
```

### 4. Delete Expense
```
DELETE /expenses/{expenseId}
Authorization: Bearer <token>

Response (200 OK):
{
  "success": true,
  "message": "Expense deleted successfully",
  "statusCode": 200
}
```

---

## Settlement Endpoints

### 1. Get Pending Settlements
```
GET /settlements/group/{groupId}/user/pending
Authorization: Bearer <token>

Response (200 OK):
{
  "success": true,
  "message": "Pending settlements fetched successfully",
  "data": [
    {
      "settlementId": 1,
      "groupId": 1,
      "debtor": {
        "userId": 2,
        "name": "Jane Smith",
        ...
      },
      "creditor": {
        "userId": 1,
        "name": "John Doe",
        ...
      },
      "amount": 100,
      "status": "PENDING",
      "createdAt": "2024-01-15T10:35:00",
      "updatedAt": "2024-01-15T10:35:00",
      "settledAt": null
    }
  ],
  "statusCode": 200
}
```

### 2. Mark Settlement as Paid
```
POST /settlements/{settlementId}/pay
Authorization: Bearer <token>

Response (200 OK):
{
  "success": true,
  "message": "Payment settled successfully",
  "statusCode": 200
}
```

### 3. Get Group Settlement Summary
```
GET /settlements/group/{groupId}/summary
Authorization: Bearer <token>

Response (200 OK):
{
  "success": true,
  "message": "Group summary fetched successfully",
  "data": {
    "message": "Group settlement summary",
    "groupId": 1
  },
  "statusCode": 200
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "amount": "Amount must be greater than 0",
    "participantIds": "Participant IDs are required"
  },
  "statusCode": 400
}
```

### 401 Unauthorized
```json
{
  "success": false,
  "message": "Invalid credentials",
  "statusCode": 401
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "Group not found with id: 999",
  "statusCode": 404
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "message": "An unexpected error occurred",
  "statusCode": 500
}
```

---

## Testing with cURL

### Register User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "securePassword123",
    "phone": "+91-9876543210"
  }'
```

### Login User
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "securePassword123"
  }'
```

### Get Profile (with token)
```bash
curl -X GET http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer <your_token_here>"
```

### Create Group
```bash
curl -X POST http://localhost:8080/api/groups \
  -H "Authorization: Bearer <your_token_here>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Trip to Goa",
    "description": "Summer vacation",
    "memberIds": [2, 3]
  }'
```

### Create Expense
```bash
curl -X POST http://localhost:8080/api/expenses \
  -H "Authorization: Bearer <your_token_here>" \
  -H "Content-Type: application/json" \
  -d '{
    "groupId": 1,
    "amount": 300,
    "description": "Hotel booking",
    "participantIds": [1, 2, 3],
    "splitType": "EQUAL"
  }'
```

---

## Response Status Codes
- `200 OK` - Request successful
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid input
- `401 Unauthorized` - Missing/invalid authentication
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error
