package com.splitwise.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseDTO {
    private Long expenseId;
    private Long groupId;
    private UserDTO paidBy;
    private BigDecimal amount;
    private String description;
    private LocalDateTime expenseDate;
    private Set<UserDTO> participants;
    private Set<ExpenseSplitDTO> splits;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
