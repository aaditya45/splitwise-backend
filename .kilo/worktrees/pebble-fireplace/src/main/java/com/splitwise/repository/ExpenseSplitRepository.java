package com.splitwise.repository;

import com.splitwise.model.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {
    List<ExpenseSplit> findByExpenseExpenseId(Long expenseId);
    List<ExpenseSplit> findByUserUserId(Long userId);
}
