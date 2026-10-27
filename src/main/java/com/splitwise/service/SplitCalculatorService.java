package com.splitwise.service;

import com.splitwise.model.Expense;
import com.splitwise.model.ExpenseSplit;
import com.splitwise.model.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SplitCalculatorService {

    public static final String EQUAL = "EQUAL";

    /**
     * Splits an amount equally, working in whole cents so the parts always add up to the total.
     * Any leftover cents go one each to the first participants (ordered by user id), so
     * 100.00 / 3 gives 33.34, 33.33, 33.33 instead of losing a cent.
     */
    public Map<User, BigDecimal> equalSplit(BigDecimal amount, Set<User> participants) {
        Map<User, BigDecimal> splits = new LinkedHashMap<>();
        if (participants.isEmpty()) {
            return splits;
        }

        BigInteger totalCents = amount.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toBigInteger();
        BigInteger count = BigInteger.valueOf(participants.size());
        BigInteger[] quotientAndRemainder = totalCents.divideAndRemainder(count);
        long baseCents = quotientAndRemainder[0].longValueExact();
        long extraCents = quotientAndRemainder[1].longValueExact();

        List<User> ordered = new ArrayList<>(participants);
        ordered.sort(Comparator.comparing(User::getUserId));

        for (int i = 0; i < ordered.size(); i++) {
            long cents = baseCents + (i < extraCents ? 1 : 0);
            splits.put(ordered.get(i), BigDecimal.valueOf(cents, 2));
        }
        return splits;
    }

    /** Builds the split rows for an expense from calculated per-user amounts. */
    public List<ExpenseSplit> createSplits(Expense expense, Map<User, BigDecimal> splits, String splitType) {
        List<ExpenseSplit> expenseSplits = new ArrayList<>();

        for (Map.Entry<User, BigDecimal> entry : splits.entrySet()) {
            ExpenseSplit split = new ExpenseSplit();
            split.setExpense(expense);
            split.setUser(entry.getKey());
            split.setAmount(entry.getValue());
            split.setSplitType(splitType);
            expenseSplits.add(split);
        }

        return expenseSplits;
    }
}
