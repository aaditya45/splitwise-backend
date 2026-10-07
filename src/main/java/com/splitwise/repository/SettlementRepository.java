package com.splitwise.repository;

import com.splitwise.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findByGroupGroupId(Long groupId);
    List<Settlement> findByDebtorUserIdAndStatus(Long userId, String status);
    List<Settlement> findByCreditorUserIdAndStatus(Long userId, String status);
    List<Settlement> findByDebtorUserIdAndGroupGroupId(Long userId, Long groupId);
    List<Settlement> findByCreditorUserIdAndGroupGroupId(Long userId, Long groupId);
}
