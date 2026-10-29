package com.splitwise.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateExpenseRequest(
	@NotNull(message = "Group ID is required") Long groupId,
	@NotNull(message = "Amount is required")
	@DecimalMin(value = "0.01", message = "Amount must be greater than 0")
	@Digits(integer = 17, fraction = 2, message = "Amount can have at most 2 decimal places") BigDecimal amount,
	String description,
	@NotEmpty(message = "At least one participant is required") List<Long> participantIds,
	String splitType
) {
}
