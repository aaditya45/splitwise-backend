package com.splitwise.service;

import com.splitwise.dto.BalanceDTO;
import com.splitwise.exception.BadRequestException;
import com.splitwise.exception.ForbiddenException;
import com.splitwise.exception.ResourceNotFoundException;
import com.splitwise.model.Expense;
import com.splitwise.model.Group;
import com.splitwise.model.Settlement;
import com.splitwise.model.SettlementStatus;
import com.splitwise.model.User;
import com.splitwise.repository.ExpenseRepository;
import com.splitwise.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final ExpenseRepository expenseRepository;
    private final GroupService groupService;
    private final UserService userService;

    /** A member together with the amount they still owe (debtor) or are still owed (creditor). */
    private static final class Party {
        private final User user;
        private BigDecimal amount;

        private Party(User user, BigDecimal amount) {
            this.user = user;
            this.amount = amount;
        }
    }

    /**
     * Net balance per member of the group (positive = is owed money, negative = owes money).
     * Includes settlements already marked PAID, so money that was paid back is not owed twice.
     */
    @Transactional(readOnly = true)
    public Map<User, BigDecimal> calculateBalances(Group group) {
        Map<Long, User> usersById = new LinkedHashMap<>();
        Map<Long, BigDecimal> balances = new LinkedHashMap<>();
        for (User member : group.getMembers()) {
            usersById.put(member.getUserId(), member);
            balances.put(member.getUserId(), BigDecimal.ZERO);
        }

        for (Expense expense : expenseRepository.findByGroupGroupId(group.getGroupId())) {
            User paidBy = expense.getPaidBy();
            usersById.putIfAbsent(paidBy.getUserId(), paidBy);
            balances.merge(paidBy.getUserId(), expense.getAmount(), BigDecimal::add);

            expense.getSplits().forEach(split -> {
                User user = split.getUser();
                usersById.putIfAbsent(user.getUserId(), user);
                balances.merge(user.getUserId(), split.getAmount().negate(), BigDecimal::add);
            });
        }

        // A payment from debtor to creditor reduces what the debtor owes and what the creditor is owed.
        for (Settlement paid : settlementRepository.findByGroupGroupIdAndStatus(group.getGroupId(), SettlementStatus.PAID)) {
            balances.merge(paid.getDebtor().getUserId(), paid.getAmount(), BigDecimal::add);
            balances.merge(paid.getCreditor().getUserId(), paid.getAmount().negate(), BigDecimal::add);
        }

        Map<User, BigDecimal> result = new LinkedHashMap<>();
        balances.forEach((id, balance) -> result.put(usersById.get(id), balance));
        return result;
    }

    /**
     * Rebuilds the PENDING settlements of a group from the current balances, using as few
     * payments as possible. PAID settlements are history and are never touched.
     */
    public List<Settlement> generateSettlements(Group group) {
        Map<User, BigDecimal> balances = calculateBalances(group);

        List<Party> debtors = new ArrayList<>();
        List<Party> creditors = new ArrayList<>();
        balances.forEach((user, balance) -> {
            if (balance.signum() < 0) {
                debtors.add(new Party(user, balance.negate()));
            } else if (balance.signum() > 0) {
                creditors.add(new Party(user, balance));
            }
        });
        Comparator<Party> largestFirst = Comparator
                .comparing((Party p) -> p.amount).reversed()
                .thenComparing(p -> p.user.getUserId());
        debtors.sort(largestFirst);
        creditors.sort(largestFirst);

        settlementRepository.deleteAll(
                settlementRepository.findByGroupGroupIdAndStatus(group.getGroupId(), SettlementStatus.PENDING));
        settlementRepository.flush();

        List<Settlement> settlements = new ArrayList<>();
        int d = 0;
        int c = 0;
        while (d < debtors.size() && c < creditors.size()) {
            Party debtor = debtors.get(d);
            Party creditor = creditors.get(c);
            BigDecimal payment = debtor.amount.min(creditor.amount);

            Settlement settlement = new Settlement();
            settlement.setGroup(group);
            settlement.setDebtor(debtor.user);
            settlement.setCreditor(creditor.user);
            settlement.setAmount(payment);
            settlement.setStatus(SettlementStatus.PENDING);
            settlements.add(settlement);

            debtor.amount = debtor.amount.subtract(payment);
            creditor.amount = creditor.amount.subtract(payment);
            if (debtor.amount.signum() == 0) {
                d++;
            }
            if (creditor.amount.signum() == 0) {
                c++;
            }
        }

        log.debug("Group {}: generated {} pending settlements", group.getGroupId(), settlements.size());
        return settlementRepository.saveAll(settlements);
    }

    /** Pending settlements in a group that involve the user, either as debtor or creditor. */
    @Transactional(readOnly = true)
    public List<Settlement> getUserPendingSettlements(Long userId, Long groupId) {
        groupService.assertMember(groupId, userId);
        return settlementRepository.findForUserInGroup(userId, groupId, SettlementStatus.PENDING);
    }

    /** Net balance of every member of a group, for the group summary. */
    @Transactional(readOnly = true)
    public List<BalanceDTO> getGroupBalances(Long groupId, Long userId) {
        Group group = groupService.getGroupForMember(groupId, userId);
        List<BalanceDTO> balances = new ArrayList<>();
        calculateBalances(group).forEach((user, balance) ->
                balances.add(new BalanceDTO(userService.convertToDTO(user), balance)));
        return balances;
    }

    /** Marks a settlement as paid. Only the two people involved in it may do so. */
    public void settlePayment(Long settlementId, Long userId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found with id: " + settlementId));

        boolean involved = settlement.getDebtor().getUserId().equals(userId)
                || settlement.getCreditor().getUserId().equals(userId);
        if (!involved) {
            throw new ForbiddenException("Only the debtor or the creditor can settle this payment");
        }
        if (settlement.getStatus() == SettlementStatus.PAID) {
            throw new BadRequestException("Settlement is already paid");
        }

        settlement.setStatus(SettlementStatus.PAID);
        settlementRepository.save(settlement);
    }
}
