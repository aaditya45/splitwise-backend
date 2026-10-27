package com.splitwise.service;

import com.splitwise.model.Expense;
import com.splitwise.model.ExpenseSplit;
import com.splitwise.model.Group;
import com.splitwise.model.Settlement;
import com.splitwise.model.SettlementStatus;
import com.splitwise.model.User;
import com.splitwise.exception.BadRequestException;
import com.splitwise.exception.ForbiddenException;
import com.splitwise.repository.ExpenseRepository;
import com.splitwise.repository.SettlementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock private SettlementRepository settlementRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private GroupService groupService;
    @Mock private UserService userService;

    private SettlementService service;
    private User alice;
    private User bob;
    private Group group;

    @BeforeEach
    void setUp() {
        service = new SettlementService(settlementRepository, expenseRepository, groupService, userService);

        alice = user(1L, "Alice");
        bob = user(2L, "Bob");
        group = new Group();
        group.setGroupId(10L);
        group.setMembers(new HashSet<>(Set.of(alice, bob)));

        lenient().when(settlementRepository.findByGroupGroupIdAndStatus(anyLong(), eq(SettlementStatus.PENDING)))
                .thenReturn(List.of());
        lenient().when(settlementRepository.findByGroupGroupIdAndStatus(anyLong(), eq(SettlementStatus.PAID)))
                .thenReturn(List.of());
        lenient().when(settlementRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static User user(long id, String name) {
        User user = new User();
        user.setUserId(id);
        user.setName(name);
        return user;
    }

    /** An expense paid by one person and split equally between two. */
    private Expense expense(User paidBy, String amount, User a, User b) {
        Expense expense = new Expense();
        expense.setGroup(group);
        expense.setPaidBy(paidBy);
        expense.setAmount(new BigDecimal(amount));
        BigDecimal half = new BigDecimal(amount).divide(BigDecimal.valueOf(2));
        for (User u : List.of(a, b)) {
            ExpenseSplit split = new ExpenseSplit();
            split.setExpense(expense);
            split.setUser(u);
            split.setAmount(half);
            expense.getSplits().add(split);
        }
        return expense;
    }

    private Settlement settlement(Long id, User debtor, User creditor, String amount, SettlementStatus status) {
        Settlement s = new Settlement();
        s.setSettlementId(id);
        s.setGroup(group);
        s.setDebtor(debtor);
        s.setCreditor(creditor);
        s.setAmount(new BigDecimal(amount));
        s.setStatus(status);
        return s;
    }

    @Test
    void payerIsCreditorAndOtherParticipantIsDebtor() {
        when(expenseRepository.findByGroupGroupId(10L)).thenReturn(List.of(expense(alice, "300.00", alice, bob)));

        List<Settlement> result = service.generateSettlements(group);

        assertEquals(1, result.size());
        assertEquals(bob, result.get(0).getDebtor());
        assertEquals(alice, result.get(0).getCreditor());
        assertEquals(0, new BigDecimal("150.00").compareTo(result.get(0).getAmount()));
        assertEquals(SettlementStatus.PENDING, result.get(0).getStatus());
    }

    @Test
    void alreadyPaidSettlementIsNotOwedAgain() {
        when(expenseRepository.findByGroupGroupId(10L)).thenReturn(List.of(expense(alice, "300.00", alice, bob)));
        when(settlementRepository.findByGroupGroupIdAndStatus(10L, SettlementStatus.PAID))
                .thenReturn(List.of(settlement(1L, bob, alice, "150.00", SettlementStatus.PAID)));

        List<Settlement> result = service.generateSettlements(group);

        assertTrue(result.isEmpty(), "Bob already paid his 150, so nothing should be pending");
    }

    @Test
    void newExpenseAfterPaymentOnlyChargesTheNewAmount() {
        when(expenseRepository.findByGroupGroupId(10L)).thenReturn(List.of(
                expense(alice, "300.00", alice, bob),
                expense(alice, "100.00", alice, bob)));
        when(settlementRepository.findByGroupGroupIdAndStatus(10L, SettlementStatus.PAID))
                .thenReturn(List.of(settlement(1L, bob, alice, "150.00", SettlementStatus.PAID)));

        List<Settlement> result = service.generateSettlements(group);

        assertEquals(1, result.size());
        assertEquals(0, new BigDecimal("50.00").compareTo(result.get(0).getAmount()));
    }

    @Test
    void offsettingExpensesNetToASinglePayment() {
        when(expenseRepository.findByGroupGroupId(10L)).thenReturn(List.of(
                expense(alice, "100.00", alice, bob),
                expense(bob, "40.00", alice, bob)));

        List<Settlement> result = service.generateSettlements(group);

        assertEquals(1, result.size());
        assertEquals(bob, result.get(0).getDebtor());
        assertEquals(0, new BigDecimal("30.00").compareTo(result.get(0).getAmount()));
    }

    @Test
    void strangerCannotSettlePayment() {
        Settlement pending = settlement(5L, bob, alice, "50.00", SettlementStatus.PENDING);
        when(settlementRepository.findById(5L)).thenReturn(Optional.of(pending));

        assertThrows(ForbiddenException.class, () -> service.settlePayment(5L, 99L));
        assertEquals(SettlementStatus.PENDING, pending.getStatus());
    }

    @Test
    void debtorCanSettleAndItCannotBeSettledTwice() {
        Settlement pending = settlement(5L, bob, alice, "50.00", SettlementStatus.PENDING);
        when(settlementRepository.findById(5L)).thenReturn(Optional.of(pending));

        service.settlePayment(5L, bob.getUserId());

        assertEquals(SettlementStatus.PAID, pending.getStatus());
        assertThrows(BadRequestException.class, () -> service.settlePayment(5L, bob.getUserId()));
    }
}
