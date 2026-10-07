

graph TB
    subgraph Views["🎨 VIEWS (Presentation Layer)"]
        V1["Dashboard<br/>- View Groups<br/>- View Expenses<br/>- Show Balances"]
        V2["Add Expense Form<br/>- Select Group<br/>- Enter Amount<br/>- Select Participants<br/>- Split Method"]
        V3["Group View<br/>- Group Members<br/>- Group Expenses<br/>- Who Owes What"]
        V4["Settlement View<br/>- Pending Payments<br/>- Mark as Paid"]
    end

    subgraph Controllers["⚙️ CONTROLLERS (Business Logic Layer)"]
        C1["User Controller<br/>- register()<br/>- login()<br/>- getUser()"]
        C2["Expense Controller<br/>- createExpense()<br/>- getExpenses()<br/>- deleteExpense()"]
        C3["Group Controller<br/>- createGroup()<br/>- addMember()<br/>- getGroupData()"]
        C4["Settlement Controller<br/>- settlePayment()<br/>- getBalances()"]
    end

    subgraph Models["📊 MODELS (Data Layer)"]
        M1["User<br/>- userId<br/>- name<br/>- email"]
        M2["Group<br/>- groupId<br/>- name<br/>- members"]
        M3["Expense<br/>- expenseId<br/>- groupId<br/>- paidBy<br/>- amount<br/>- participants<br/>- date"]
        M4["Settlement<br/>- settlementId<br/>- groupId<br/>- from<br/>- to<br/>- amount<br/>- status"]
    end

    subgraph Services["🔧 SERVICES (Business Logic)"]
        S1["Split Calculator<br/>- equalSplit()<br/>- calculateOwed()"]
        S2["Settlement Engine<br/>- calculateBalances()<br/>- generatePayments()"]
    end

    subgraph Database["💾 DATABASE"]
        DB1["users"]
        DB2["groups"]
        DB3["group_members"]
        DB4["expenses"]
        DB5["settlements"]
    end

    %% View to Controller
    V1 --> C4
    V1 --> C3
    V2 --> C2
    V3 --> C3
    V3 --> C2
    V4 --> C4

    %% Controller to Services
    C2 --> S1
    C4 --> S2

    %% Services to Models
    S1 --> M3
    S2 --> M4

    %% Controllers to Models
    C1 --> M1
    C2 --> M3
    C3 --> M2
    C4 --> M4

    %% Models to Database
    M1 --> DB1
    M2 --> DB2
    M2 --> DB3
    M3 --> DB4
    M4 --> DB5

    %% Relationships
    M3 -->|belongs to| M2
    M3 -->|paid by| M1
    M4 -->|group| M2

    style Views fill:#e1f5ff
    style Controllers fill:#fff3e0
    style Models fill:#f3e5f5
    style Services fill:#e8f5e9
    style Database fill:#fce4ec
