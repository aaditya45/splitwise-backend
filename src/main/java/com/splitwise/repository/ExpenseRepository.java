package com.splitwise.repository;

import com.splitwise.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByGroupGroupId(Long groupId);
    List<Expense> findByPaidByUserId(Long userId);
}
