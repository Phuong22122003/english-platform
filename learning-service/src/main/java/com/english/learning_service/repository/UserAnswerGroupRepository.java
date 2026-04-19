package com.english.learning_service.repository;

import com.english.learning_service.entity.UserAnswerGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserAnswerGroupRepository extends JpaRepository<UserAnswerGroup,String> {
    @Query("""
        SELECT g
        FROM UserAnswerGroup g
        LEFT JOIN FETCH g.answers a
        WHERE g.examHistory.id = :examHistoryId
        ORDER BY g.part asc, a.createdDate
        """)
    List<UserAnswerGroup> findByExamHistoryId(String examHistoryId);
}
