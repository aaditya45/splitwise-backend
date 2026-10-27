package com.splitwise.service;

import com.splitwise.model.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SplitCalculatorServiceTest {

    private final SplitCalculatorService calculator = new SplitCalculatorService();

    private static User user(long id) {
        User user = new User();
        user.setUserId(id);
        user.setEmail("user" + id + "@example.com");
        return user;
    }

    private static BigDecimal sum(Map<User, BigDecimal> splits) {
        return splits.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void splitsEvenlyWhenDivisible() {
        Set<User> participants = new LinkedHashSet<>(Set.of(user(1), user(2)));

        Map<User, BigDecimal> splits = calculator.equalSplit(new BigDecimal("300.00"), participants);

        assertEquals(2, splits.size());
        splits.values().forEach(v -> assertEquals(new BigDecimal("150.00"), v));
    }

    @Test
    void neverLosesACentWhenNotDivisible() {
        Set<User> participants = new LinkedHashSet<>(Set.of(user(3), user(1), user(2)));

        Map<User, BigDecimal> splits = calculator.equalSplit(new BigDecimal("100.00"), participants);

        assertEquals(new BigDecimal("100.00"), sum(splits));
        // leftover cent goes to the lowest user id, deterministically
        assertEquals(new BigDecimal("33.34"), splits.get(user(1)));
        assertEquals(new BigDecimal("33.33"), splits.get(user(2)));
        assertEquals(new BigDecimal("33.33"), splits.get(user(3)));
    }

    @Test
    void handlesSmallAmountsAcrossManyPeople() {
        Set<User> participants = new LinkedHashSet<>();
        for (long id = 1; id <= 7; id++) {
            participants.add(user(id));
        }

        Map<User, BigDecimal> splits = calculator.equalSplit(new BigDecimal("0.05"), participants);

        assertEquals(new BigDecimal("0.05"), sum(splits));
        assertTrue(splits.values().stream().allMatch(v -> v.signum() >= 0));
    }

    @Test
    void emptyParticipantsGiveEmptySplits() {
        assertTrue(calculator.equalSplit(new BigDecimal("10.00"), Set.of()).isEmpty());
    }
}
