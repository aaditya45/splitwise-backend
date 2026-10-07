# Splitwise Backend - Spring Boot Application

A complete Spring Boot backend implementation of a Splitwise-like expense sharing application with MVC architecture.

## Project Structure

```
├── src/
│   └── main/
│       ├── java/com/splitwise/
│       │   ├── SplitwiseApplication.java          # Main application class
│       │   ├── controller/                        # REST Controllers
│       │   │   ├── AuthController.java
│       │   │   ├── UserController.java
│       │   │   ├── GroupController.java
│       │   │   ├── ExpenseController.java
│       │   │   └── SettlementController.java
│       │   ├── service/                           # Business Logic Services
│       │   │   ├── UserService.java
│       │   │   ├── GroupService.java
│       │   │   ├── ExpenseService.java
│       │   │   ├── SettlementService.java
│       │   │   └── SplitCalculatorService.java
│       │   ├── model/                             # JPA Entities
│       │   │   ├── User.java
│       │   │   ├── Group.java
│       │   │   ├── Expense.java
│       │   │   ├── ExpenseSplit.java
│       │   │   └── Settlement.java
│       │   ├── repository/                        # Data Access Layer
│       │   │   ├── UserRepository.java
│       │   │   ├── GroupRepository.java
│       │   │   ├── ExpenseRepository.java
│       │   │   ├── SettlementRepository.java
│       │   │   └── ExpenseSplitRepository.java
│       │   ├── dto/                               # Data Transfer Objects
│       │   │   ├── UserDTO.java
│       │   │   ├── GroupDTO.java
│       │   │   ├── ExpenseDTO.java
│       │   │   ├── ExpenseSplitDTO.java
│       │   │   ├── SettlementDTO.java
│       │   │   ├── request/
│       │   │   │   ├── RegisterRequest.java
│       │   │   │   ├── LoginRequest.java
│       │   │   │   ├── CreateExpenseRequest.java
│       │   │   │   └── CreateGroupRequest.java
│       │   │   └── response/
│       │   │       ├── AuthResponse.java
│       │   │       └── ApiResponse.java
│       │   ├── security/                          # Authentication & JWT
│       │   │   ├── JwtTokenProvider.java
│       │   │   ├── UserDetailsImpl.java
│       │   │   ├── CustomUserDetailsService.java
│       │   │   └── JwtAuthenticationFilter.java
│       │   ├── config/                            # Configuration Classes
│       │   │   └── SecurityConfig.java
│       │   └── exception/                         # Exception Handling
│       │       └── GlobalExceptionHandler.java
│       └── resources/
│           └── application.properties             # Application Configuration
├── build.gradle                                   # Gradle Build Configuration
├── settings.gradle                                # Gradle Settings
├── gradlew                                        # Gradle Wrapper (Unix)
├── gradlew.bat                                    # Gradle Wrapper (Windows)
└── .env                                           # Environment Variables

```

## Prerequisites

- Java 17 or higher
- MySQL Database
- Git

## Setup Instructions

### 1. Clone/Setup Project
```bash
# Navigate to project directory
cd splitwise-backend
```

### 2. Database Setup
Create a MySQL database:
```sql
CREATE DATABASE splitwise_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Environment Configuration
Update `.env` file with your database credentials:
```env
DB_HOST=localhost
DB_PORT=3306
DB_NAME=splitwise_db
DB_USERNAME=root
DB_PASSWORD=your_password

APP_PORT=8080
JWT_SECRET=your_super_secret_jwt_key_change_in_production
JWT_EXPIRATION=86400000
```

### 4. Build Project
Using Gradle Wrapper:

**On Linux/Mac:**
```bash
./gradlew clean build
```

**On Windows:**
```bash
gradlew.bat clean build
```

### 5. Run Application
```bash
# Linux/Mac
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

The application will start on `http://localhost:8080`

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user

### Users
- `GET /api/users/profile` - Get current user profile
- `GET /api/users/{userId}` - Get user by ID

### Groups
- `POST /api/groups` - Create new group
- `GET /api/groups/{groupId}` - Get group details
- `GET /api/groups/user/groups` - Get user's groups
- `POST /api/groups/{groupId}/members/{userId}` - Add member to group

### Expenses
- `POST /api/expenses` - Create expense
- `GET /api/expenses/{expenseId}` - Get expense details
- `GET /api/expenses/group/{groupId}` - Get group expenses
- `DELETE /api/expenses/{expenseId}` - Delete expense

### Settlements
- `GET /api/settlements/group/{groupId}/user/pending` - Get pending settlements
- `POST /api/settlements/{settlementId}/pay` - Mark settlement as paid
- `GET /api/settlements/group/{groupId}/summary` - Get group settlement summary

## Key Features

### Authentication & Security
- JWT-based authentication
- Password encryption using BCrypt
- CORS support
- Role-based access control ready

### Business Logic
- **Split Calculator**: Equal split, percentage-based splits
- **Settlement Engine**: Automatic settlement calculation and optimization
- **Group Management**: Create groups, add members, track expenses
- **Expense Tracking**: Add expenses with multiple participants and split methods

### Database
- JPA/Hibernate ORM
- Automatic schema generation
- MySQL 8.0+ compatible
- Transaction management

### API
- RESTful endpoints with proper HTTP methods
- Consistent response format using ApiResponse wrapper
- Comprehensive error handling with GlobalExceptionHandler
- Input validation using Jakarta validation

## Workflow Example

### Adding an Expense
1. User creates group with friends
2. User adds expense: "Paid ₹300 for 3 people"
3. System automatically calculates splits (₹100 per person)
4. Settlement records are generated
5. Friends can see they owe ₹100 each

### Payment Settlement
1. User views pending settlements
2. User marks payment as done
3. Settlement status updates to PAID

## Development

### Adding New Endpoints
1. Create request/response DTOs in `dto/`
2. Create service method in `service/`
3. Create controller endpoint in `controller/`
4. Add repository method if needed in `repository/`

### Environment Variables
All sensitive data is stored in `.env` file and loaded via dotenv-java library:
- Database credentials
- JWT secret
- API ports
- Log levels

## Build & Packaging

### Create JAR
```bash
./gradlew build
# JAR will be in build/libs/
```

### Run JAR
```bash
java -jar build/libs/splitwise-*.jar
```

## Testing
```bash
./gradlew test
```

## Troubleshooting

### Connection Refused
- Ensure MySQL is running
- Check database credentials in `.env`

### Port Already in Use
- Change `APP_PORT` in `.env`
- Or kill process using port 8080

### Gradle Issues
- Clear cache: `./gradlew clean`
- Update dependencies: `./gradlew build --refresh-dependencies`

## Future Enhancements
- [ ] Payment method integration (Stripe, PayPal)
- [ ] User notifications (Email, Push)
- [ ] Recurring expenses
- [ ] Expense categories & analytics
- [ ] Multi-currency support
- [ ] Unit & Integration tests
- [ ] API documentation (Swagger/OpenAPI)

## License
MIT License

## Support
For issues or questions, please create an issue in the repository.
