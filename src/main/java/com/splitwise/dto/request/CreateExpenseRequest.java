package com.splitwise.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateExpenseRequest(
	@NotNull(message = "Group ID is required") Long groupId,
	@NotNull(message = "Amount is required")
	@DecimalMin(value = "0.01", message = "Amount must be greater than 0") BigDecimal amount,
	String description,
	@NotNull(message = "Participant IDs are required") List<Long> participantIds,
	String splitType
) {
}
