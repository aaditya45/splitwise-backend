package com.splitwise.repository;

import com.splitwise.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    
    @Query("SELECT g FROM Group g JOIN g.members m WHERE m.userId = :userId")
    List<Group> findGroupsByUserId(@Param("userId") Long userId);
    
    List<Group> findByCreatedByUserId(Long userId);
    
    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.members WHERE g.groupId = ?1")
    Optional<Group> findByIdWithMembers(Long groupId);
}
