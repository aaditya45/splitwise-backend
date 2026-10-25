package com.splitwise.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

public record ExpenseDTO(Long expenseId, Long groupId, UserDTO paidBy, BigDecimal amount, String description,
                         LocalDateTime expenseDate, Set<UserDTO> participants, Set<ExpenseSplitDTO> splits,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
}
