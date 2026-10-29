# API Reference

Base URL: `http://localhost:8088` (the port is `APP_PORT` in `.env`).

Examples on this page are real responses from the running backend, with emails shortened and tokens replaced by `<jwt>`. For what each endpoint does internally, see [docs/FLOWS.md](docs/FLOWS.md).

## Conventions

**Auth.** Every endpoint except `/api/auth/**` needs:

```
Authorization: Bearer <jwt>
```

Missing, malformed or expired tokens get `401`.

**Envelope.** Every response has this shape; fields that are `null` are left out (so a user with no phone has no `phone` key):

```json
{ "success": true, "message": "…", "data": { }, "statusCode": 200 }
```

**Money** is a JSON number with up to 2 decimals. **Dates** are ISO-8601 local date-times without a zone, e.g. `2026-10-28T01:02:04.664683`.

**Status codes**

| Code | Meaning |
|---|---|
| 200 / 201 | OK / created |
| 400 | Validation failed (`data` maps field → message) or a business rule was broken (`message` says which) |
| 401 | Not logged in, or wrong email/password at login |
| 403 | Logged in, but not allowed: not a group member, not the payer, not a party to the settlement |
| 404 | The group, expense, settlement or user doesn't exist |
| 500 | Unexpected server error |

## Access rules

| Endpoint | Who may call it |
|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | anyone |
| `GET /api/users/profile`, `GET /api/users/{userId}` | any logged-in user |
| `POST /api/groups`, `GET /api/groups/user/groups` | any logged-in user |
| `GET /api/groups/{groupId}` | members of the group |
| `POST /api/groups/{groupId}/members/{userId}` | members of the group |
| `POST /api/expenses` | members of the group; every participant must be a member too |
| `GET /api/expenses/{expenseId}`, `GET /api/expenses/group/{groupId}` | members of the group |
| `DELETE /api/expenses/{expenseId}` | the payer only |
| `GET /api/settlements/group/{groupId}/user/pending`, `…/summary` | members of the group |
| `POST /api/settlements/{settlementId}/pay` | the settlement's debtor or creditor |

## Endpoints

| Method | Path | Summary |
|---|---|---|
| POST | [`/api/auth/register`](#post-apiauthregister) | Create an account and get a token |
| POST | [`/api/auth/login`](#post-apiauthlogin) | Get a token |
| GET | [`/api/users/profile`](#get-apiusersprofile) | Your own profile |
| GET | [`/api/users/{userId}`](#get-apiusersuserid) | Look up a user |
| POST | [`/api/groups`](#post-apigroups) | Create a group |
| GET | [`/api/groups/user/groups`](#get-apigroupsusergroups) | Groups you belong to |
| GET | [`/api/groups/{groupId}`](#get-apigroupsgroupid) | One group with members |
| POST | [`/api/groups/{groupId}/members/{userId}`](#post-apigroupsgroupidmembersuserid) | Add a member |
| POST | [`/api/expenses`](#post-apiexpenses) | Add an expense |
| GET | [`/api/expenses/{expenseId}`](#get-apiexpensesexpenseid) | One expense with splits |
| GET | [`/api/expenses/group/{groupId}`](#get-apiexpensesgroupgroupid) | All expenses in a group |
| DELETE | [`/api/expenses/{expenseId}`](#delete-apiexpensesexpenseid) | Delete an expense you paid |
| GET | [`/api/settlements/group/{groupId}/user/pending`](#get-apisettlementsgroupgroupiduserpending) | Your pending settlements in a group |
| POST | [`/api/settlements/{settlementId}/pay`](#post-apisettlementssettlementidpay) | Mark a settlement paid |
| GET | [`/api/settlements/group/{groupId}/summary`](#get-apisettlementsgroupgroupidsummary) | Everyone's balances and all pending settlements |

---

### Auth

#### `POST /api/auth/register`

| Field | Type | Rules |
|---|---|---|
| `name` | string | required |
| `email` | string | required, valid email, unique |
| `password` | string | required |
| `phone` | string | optional, max 20 characters |

```bash
curl -X POST http://localhost:8088/api/auth/register -H 'Content-Type: application/json' -d '{"name":"Alice","email":"alice@example.com","password":"secret123","phone":"9876543210"}'
```

`201`

```json
{
  "success": true,
  "message": "User registered successfully",
  "data": { "token": "<jwt>", "type": "Bearer", "userId": 29, "email": "alice@example.com", "name": "Alice" },
  "statusCode": 201
}
```

Errors: `400 "Email already registered"`; `400 "Validation failed"`:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": { "password": "Password is required", "name": "Name is required", "email": "Email should be valid" },
  "statusCode": 400
}
```

#### `POST /api/auth/login`

Body: `{ "email": "…", "password": "…" }` (both required).

`200`: same `data` as register (`token`, `type`, `userId`, `email`, `name`), with message `"Login successful"`.

Errors: `401 "Invalid credentials"` for a wrong password or unknown email.

---

### Users

`UserDTO`: `{ userId, name, email, phone?, profilePicture? }`. The password is never returned.

#### `GET /api/users/profile`

`200`: the caller's `UserDTO`.

#### `GET /api/users/{userId}`

`200`: that user's `UserDTO`. Errors: `404` if unknown.

---

### Groups

`GroupDTO`: `{ groupId, name, description?, groupImage?, createdBy: UserDTO, members: UserDTO[], createdAt, updatedAt }`

#### `POST /api/groups`

| Field | Type | Rules |
|---|---|---|
| `name` | string | required |
| `description` | string | optional |
| `groupImage` | string | optional (URL) |
| `memberIds` | number[] | optional; each must be an existing user. The caller is always added |

```bash
curl -X POST http://localhost:8088/api/groups -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"name":"Goa trip","description":"Dec 2026","memberIds":[30]}'
```

`201`

```json
{
  "success": true,
  "message": "Group created successfully",
  "data": {
    "groupId": 11,
    "name": "Goa trip",
    "description": "Dec 2026",
    "createdBy": { "userId": 29, "name": "Alice", "email": "alice@example.com", "phone": "9876543210" },
    "members": [
      { "userId": 29, "name": "Alice", "email": "alice@example.com", "phone": "9876543210" },
      { "userId": 30, "name": "Bob", "email": "bob@example.com" }
    ],
    "createdAt": "2026-10-28T01:02:04.655542",
    "updatedAt": "2026-10-28T01:02:04.655555"
  },
  "statusCode": 201
}
```

Errors: `400` blank name; `400 "User not found with id: …"` for an unknown member.

#### `GET /api/groups/user/groups`

`200`: `GroupDTO[]`, every group the caller is a member of.

#### `GET /api/groups/{groupId}`

`200`: `GroupDTO`. Errors: `404` no such group; `403 "You are not a member of this group"`.

#### `POST /api/groups/{groupId}/members/{userId}`

No body. Adds `userId` to the group; adding an existing member is a no-op.

`200 "Member added successfully"`. Errors: `404` group or user unknown; `403` caller not a member.

---

### Expenses

`ExpenseDTO`: `{ expenseId, groupId, paidBy: UserDTO, amount, description?, expenseDate, participants: UserDTO[], splits: SplitDTO[], createdAt, updatedAt }`
`SplitDTO`: `{ splitId, user: UserDTO, amount, splitType }`

#### `POST /api/expenses`

The caller is recorded as the payer.

| Field | Type | Rules |
|---|---|---|
| `groupId` | number | required; caller must be a member |
| `amount` | number | required, ≥ 0.01, at most 2 decimals |
| `description` | string | optional |
| `participantIds` | number[] | required, not empty; every ID must be a group member. The payer can be left out |
| `splitType` | string | optional; only `"EQUAL"` (the default) is accepted |

The amount is split equally; leftover paise go to the participants with the lowest user IDs, so splits always add up to `amount`. Adding an expense recalculates the group's pending settlements and notifies the other members.

```bash
curl -X POST http://localhost:8088/api/expenses -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"groupId":11,"amount":1000,"description":"Hotel","participantIds":[29,30],"splitType":"EQUAL"}'
```

`201`

```json
{
  "success": true,
  "message": "Expense created successfully",
  "data": {
    "expenseId": 21,
    "groupId": 11,
    "paidBy": { "userId": 29, "name": "Alice", "email": "alice@example.com", "phone": "9876543210" },
    "amount": 1000,
    "description": "Hotel",
    "expenseDate": "2026-10-28T01:02:04.664683",
    "participants": [
      { "userId": 29, "name": "Alice", "email": "alice@example.com", "phone": "9876543210" },
      { "userId": 30, "name": "Bob", "email": "bob@example.com" }
    ],
    "splits": [
      { "splitId": 51, "user": { "userId": 29, "name": "Alice", "email": "alice@example.com", "phone": "9876543210" }, "amount": 500.0, "splitType": "EQUAL" },
      { "splitId": 52, "user": { "userId": 30, "name": "Bob", "email": "bob@example.com" }, "amount": 500.0, "splitType": "EQUAL" }
    ]
  },
  "statusCode": 201
}
```

Errors:

| Status | When |
|---|---|
| 400 | `Validation failed` (amount, empty `participantIds`, missing `groupId`) |
| 400 | `Unsupported split type: PERCENTAGE. Only EQUAL is supported` |
| 400 | `Participant not found with id: …` / `Participant … is not a member of this group` |
| 403 | Caller isn't a member of the group |
| 404 | Group doesn't exist |

#### `GET /api/expenses/{expenseId}`

`200`: `ExpenseDTO`. Errors: `404` unknown; `403` caller not in the expense's group.

#### `GET /api/expenses/group/{groupId}`

`200`: `ExpenseDTO[]` for the group. Errors: `404` no such group; `403` not a member.

#### `DELETE /api/expenses/{expenseId}`

Deletes the expense and its splits, then recalculates the group's pending settlements.

`200 "Expense deleted successfully"`. Errors: `404` unknown; `403 "Only the person who paid can delete this expense"`.

---

### Settlements

`SettlementDTO`: `{ settlementId, groupId, debtor: UserDTO, creditor: UserDTO, amount, status, createdAt, updatedAt }`. The debtor owes the creditor `amount`; `status` is `PENDING` or `PAID`.

Pending settlements are regenerated every time an expense is added or deleted, so **settlement IDs change**. Always fetch fresh IDs before paying.

#### `GET /api/settlements/group/{groupId}/user/pending`

`200`: the caller's `PENDING` settlements in the group, both the ones they owe and the ones owed to them.

```json
{
  "success": true,
  "message": "Pending settlements fetched successfully",
  "data": [
    {
      "settlementId": 45,
      "groupId": 11,
      "debtor": { "userId": 30, "name": "Bob", "email": "bob@example.com" },
      "creditor": { "userId": 29, "name": "Alice", "email": "alice@example.com", "phone": "9876543210" },
      "amount": 500.0,
      "status": "PENDING",
      "createdAt": "2026-10-28T01:02:04.675806",
      "updatedAt": "2026-10-28T01:02:04.675812"
    }
  ],
  "statusCode": 200
}
```

Errors: `404` no such group; `403` not a member.

#### `POST /api/settlements/{settlementId}/pay`

No body. Records that the debtor paid the creditor outside the app; nothing else is recalculated. Paid settlements are kept and counted in all later balances.

`200 "Payment settled successfully"`. Errors: `404` unknown; `403 "Only the debtor or creditor can settle this payment"`; `400 "Settlement is already PAID"`.

#### `GET /api/settlements/group/{groupId}/summary`

`200`: every member's net balance (positive = is owed, negative = owes; they add up to 0), sorted by user ID, plus all `PENDING` settlements in the group.

```json
{
  "success": true,
  "message": "Group summary fetched successfully",
  "data": {
    "groupId": 11,
    "balances": [
      { "user": { "userId": 29, "name": "Alice", "email": "alice@example.com", "phone": "9876543210" }, "balance": 500.0 },
      { "user": { "userId": 30, "name": "Bob", "email": "bob@example.com" }, "balance": -500.0 }
    ],
    "settlements": [ { "settlementId": 45, "debtor": { "userId": 30 }, "creditor": { "userId": 29 }, "amount": 500.0, "status": "PENDING" } ]
  },
  "statusCode": 200
}
```

(`settlements` entries are full `SettlementDTO`s, shortened here.)

Errors: `404` no such group; `403` not a member.

---

## End-to-end with curl

Alice and Bob split a hotel bill, and Bob settles up. `jq` extracts fields.

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

The final summary shows both balances at `0`: the hotel was split 500 / 500, and Bob's 500 is paid.
