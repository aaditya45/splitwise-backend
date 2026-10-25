package com.splitwise.event;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseCreatedEvent(
        Long expenseId,
        Long groupId,
        String groupName,
        String description,
        BigDecimal amount,
        Long actorUserId,
        String actorName,
        List<Recipient> recipients) {

    public record Recipient(Long userId, String email, String name) {
    }
}