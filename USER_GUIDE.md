# Splitwise Application - User Guide

A complete guide on how to use the Splitwise application to manage shared expenses and settle payments between group members.

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [User Registration](#user-registration)
3. [User Login](#user-login)
4. [Creating a Group](#creating-a-group)
5. [Adding Members to Group](#adding-members-to-group)
6. [Creating an Expense](#creating-an-expense)
7. [Viewing Settlements](#viewing-settlements)
8. [Paying a Settlement](#paying-a-settlement)
9. [Complete Workflow Example](#complete-workflow-example)
10. [Troubleshooting](#troubleshooting)

---

## Getting Started

The Splitwise application is running on `http://localhost:8088` (or the configured APP_PORT).

### What is Splitwise?

Splitwise helps you track shared expenses within groups and calculates who owes whom. Key features:

- **Create Groups**: Form groups with friends to track shared expenses
- **Add Expenses**: Record who paid and who participated
- **Automatic Calculations**: System auto-calculates how much each person owes
- **Settlements**: Track and mark payments as settled

---

## User Registration

### How to Register

**Endpoint:** `POST /api/auth/register`

Use any REST client (Postman, curl, or your frontend application) to register.

**Request:**
```json
{
  "name": "Alice",
  "email": "alice@example.com",
  "password": "password123",
  "phone": "9876543210"
}
```

**Response:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "userId": 1,
    "email": "alice@example.com",
    "name": "Alice",
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "type": "Bearer"
  },
  "statusCode": 201
}
```

### Registration Requirements

- **Name**: Cannot be empty
- **Email**: Must be valid and unique (cannot register with same email twice)
- **Password**: Cannot be empty
- **Phone**: Optional

### After Registration

You'll receive a JWT token. Keep this token for all future requests!

---

## User Login

### How to Login

**Endpoint:** `POST /api/auth/login`

**Request:**
```json
{
  "email": "alice@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "User logged in successfully",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "type": "Bearer",
    "userId": 1,
    "email": "alice@example.com",
    "name": "Alice"
  },
  "statusCode": 200
}
```

### Authentication Header

For all subsequent requests, include your token:
```
Authorization: Bearer <your_token_here>
```

---

## Creating a Group

### How to Create a Group

**Endpoint:** `POST /api/groups`

**Request:**
```json
{
  "name": "College Friends Trip",
  "description": "Trip to Goa 2024",
  "groupImage": "https://example.com/image.jpg"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Group created successfully",
  "data": {
    "groupId": 5,
    "name": "College Friends Trip",
    "description": "Trip to Goa 2024",
    "groupImage": "https://example.com/image.jpg",
    "createdBy": 1,
    "createdAt": "2024-10-04T10:30:00",
    "updatedAt": "2024-10-04T10:30:00"
  },
  "statusCode": 201
}
```

### Group Details

- **Name**: Required - name of your group
- **Description**: Optional - what the group is for
- **groupImage**: Optional - URL to group image
- **Creator**: Automatically set to the logged-in user

---

## Adding Members to Group

### How to Add a Member

**Endpoint:** `POST /api/groups/{groupId}/members/{userId}`

**Example:**
```
POST /api/groups/5/members/2
```

This adds user with ID 2 to group with ID 5.

**Response:**
```json
{
  "success": true,
  "message": "Member added to group successfully",
  "data": {
    "groupId": 5,
    "userId": 2,
    "name": "Bob"
  },
  "statusCode": 200
}
```

### Steps to Add Members

1. Get the `groupId` (from group creation response)
2. Get the `userId` of the person you want to add
3. Call the endpoint with both IDs
4. Person must already be registered in the system

### View Group Members

**Endpoint:** `GET  /api/groups/{groupId}`

**Response includes all members:**
```json
{
  "success": true,
  "message": "Group retrieved successfully",
  "data": {
    "groupId": 5,
    "name": "College Friends Trip",
    "members": [
      { "userId": 1, "name": "Alice" },
      { "userId": 2, "name": "Bob" }
    ]
  },
  "statusCode": 200
}
```

---

## Creating an Expense

### How to Create an Expense

**Endpoint:** `POST /api/expenses`

**Request:**
```json
{
  "groupId": 5,
  "amount": 300.00,
  "description": "Lunch for the group",
  "participantIds": [1, 2],
  "splitType": "EQUAL"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Expense created and settlements calculated successfully",
  "data": {
    "expenseId": 10,
    "groupId": 5,
    "paidBy": 1,
    "amount": 300.00,
    "description": "Lunch for the group",
    "expenseDate": "2024-10-04",
    "splits": [
      {
        "splitId": 1,
        "userId": 1,
        "amount": 150.00,
        "splitType": "EQUAL"
      },
      {
        "splitId": 2,
        "userId": 2,
        "amount": 150.00,
        "splitType": "EQUAL"
      }
    ]
  },
  "statusCode": 201
}
```

### Expense Parameters

- **groupId**: ID of the group (required)
- **amount**: Total expense amount in decimal (required)
- **description**: What the expense is for (optional)
- **participantIds**: Array of user IDs sharing this expense (required)
- **splitType**: How to split the expense (required)
  - `EQUAL`: Divide equally among all participants
  - `PERCENTAGE`: Divide by percentages
  - `ITEMIZE`: Custom amounts for each person

### Who Pays?

The logged-in user is automatically set as the person who paid (`paidBy`).

### Example Scenarios

**Scenario 1: Equal Split**
- Alice pays ₹300 for lunch
- Participants: Alice, Bob
- Split: ₹150 each

**Scenario 2: Unequal Split**
- Alice pays ₹500 for hotel
- Participants: Alice, Bob, Charlie (3 people)
- Alice contributed ₹200, Bob ₹150, Charlie ₹150
- Each pays their own part to Alice

---

## Viewing Settlements

### What is a Settlement?

A settlement records who owes money to whom in a group.

### Get Pending Settlements

**Endpoint:** `GET /api/settlements/group/{groupId}/user/pending`

**Example:**
```
GET /api/settlements/group/5/user/pending
```

**Response:**
```json
{
  "success": true,
  "message": "Pending settlements retrieved successfully",
  "data": [
    {
      "settlementId": 1,
      "groupId": 5,
      "debtor": {
        "userId": 2,
        "name": "Bob"
      },
      "creditor": {
        "userId": 1,
        "name": "Alice"
      },
      "amount": 150.00,
      "status": "PENDING",
      "createdAt": "2024-10-04T10:35:00"
    }
  ],
  "statusCode": 200
}
```

### Settlement Status Types

- **PENDING**: Payment still needs to be made
- **PAID**: Payment has been marked as completed
- **SETTLED**: Settlement fully resolved

### Group Settlement Summary

**Endpoint:** `GET /api/settlements/group/{groupId}/summary`

See all settlements in a group at once.

---

## Paying a Settlement

### How to Mark a Payment

**Endpoint:** `POST /api/settlements/{settlementId}/pay`

**Example:**
```
POST /api/settlements/1/pay
```

**Response:**
```json
{
  "success": true,
  "message": "Settlement marked as paid successfully",
  "data": {
    "settlementId": 1,
    "status": "PAID",
    "updatedAt": "2024-10-04T11:00:00"
  },
  "statusCode": 200
}
```

### Important Notes

- Only the debtor (person who owes) can mark a settlement as paid
- Once marked as paid, it's recorded with timestamp
- Review your pending settlements regularly

---

## Complete Workflow Example

Here's a step-by-step example of using the application:

### Step 1: Register Two Users

**User 1 - Alice:**
```json
{
  "name": "Alice",
  "email": "alice@example.com",
  "password": "alice123",
  "phone": "9876543210"
}
```

**User 2 - Bob:**
```json
{
  "name": "Bob",
  "email": "bob@example.com",
  "password": "bob123",
  "phone": "9876543211"
}
```

Get tokens for both users (you'll need them for next steps).

### Step 2: Create a Group (Alice logs in)

```json
{
  "name": "Weekend Getaway",
  "description": "Trip to Goa"
}
```

Get `groupId: 1`

### Step 3: Add Bob to the Group (Alice)

```
POST /api/groups/1/members/2
```

### Step 4: Alice Creates an Expense

Alice pays ₹1000 for hotel, shared equally with Bob.

```json
{
  "groupId": 1,
  "amount": 1000.00,
  "description": "Hotel booking",
  "participantIds": [1, 2],
  "splitType": "EQUAL"
}
```

**Result:**
- Alice paid ₹1000
- Alice's share: ₹500
- Bob's share: ₹500
- **Bob owes Alice ₹500**

### Step 5: View Pending Settlements (Bob logs in)

```
GET /api/settlements/group/1/user/pending
```

Bob sees he owes Alice ₹500.

### Step 6: Bob Pays the Settlement

Bob uses UPI/bank transfer to pay Alice ₹500 in real life.

Then marks in app:
```
POST /api/settlements/1/pay
```

**Settlement is now marked as PAID** ✅

---

## Troubleshooting

### Issue: "Email already exists"

**Problem:** You're trying to register with an email that's already in use.

**Solution:** Use a different email address or login with existing account.

### Issue: "User not found"

**Problem:** The user ID you're trying to add to a group doesn't exist.

**Solution:** Make sure the user is registered first, then use correct user ID.

### Issue: "Invalid token"

**Problem:** Your authentication token is expired or incorrect.

**Solution:** Login again to get a new token.

### Issue: "JWT signing key size error"

**Problem:** Application has invalid JWT configuration.

**Solution:** Check that JWT_SECRET in .env is at least 512 characters long.

### Issue: "Database connection failed"

**Problem:** Cannot connect to PostgreSQL database.

**Solution:** 
- Check PostgreSQL is running
- Verify connection details in .env file
- Ensure database `splitwise_db` exists

### Issue: "Group not found"

**Problem:** The group ID doesn't exist.

**Solution:** Create a new group or use correct group ID.

---

## API Quick Reference

| Action | Method | Endpoint | Auth Required |
|--------|--------|----------|----------------|
| Register | POST | `/api/auth/register` | No |
| Login | POST | `/api/auth/login` | No |
| Create Group | POST | `/api/groups` | Yes |
| Get Group | GET | `/api/groups/{groupId}` | Yes |
| Add Member | POST | `/api/groups/{groupId}/members/{userId}` | Yes |
| Create Expense | POST | `/api/expenses` | Yes |
| Get Expense | GET | `/api/expenses/{expenseId}` | Yes |
| Get Group Expenses | GET | `/api/expenses/group/{groupId}` | Yes |
| View Settlements | GET | `/api/settlements/group/{groupId}/user/pending` | Yes |
| Pay Settlement | POST | `/api/settlements/{settlementId}/pay` | Yes |
| Get Settlement Summary | GET | `/api/settlements/group/{groupId}/summary` | Yes |

---

## Tips & Best Practices

1. **Keep Tokens Safe**: Never share your JWT token with others
2. **Verify Amounts**: Double-check expense amounts before creating
3. **Regular Settlements**: Don't let too many pending settlements accumulate
4. **Group Organization**: Create separate groups for different trips/purposes
5. **Communication**: Inform group members about expenses so they're not surprised
6. **Real Payment**: Always make real payments (UPI/Bank) before marking as paid in app

---

## Support

For technical issues, check the API_DOCUMENTATION.md or QUICKSTART.md files.

For questions about the application logic, refer to the README.md.

---

**Happy splitting! 🎉**
