package com.splitwise.repository;

import com.splitwise.model.Settlement;
import com.splitwise.model.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findByGroupGroupIdAndStatus(Long groupId, SettlementStatus status);

    @Query("SELECT s FROM Settlement s " +
           "JOIN FETCH s.debtor JOIN FETCH s.creditor " +
           "WHERE s.group.groupId = :groupId AND s.status = :status " +
           "AND (s.debtor.userId = :userId OR s.creditor.userId = :userId) " +
           "ORDER BY s.settlementId")
    List<Settlement> findForUserInGroup(@Param("userId") Long userId,
                                        @Param("groupId") Long groupId,
                                        @Param("status") SettlementStatus status);
}
