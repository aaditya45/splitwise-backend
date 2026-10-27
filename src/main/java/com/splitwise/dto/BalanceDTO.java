package com.splitwise.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Net balance of one member in a group: positive = is owed money, negative = owes money. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceDTO {
    private UserDTO user;
    private BigDecimal balance;
}
