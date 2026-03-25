package com.english.learning_service.repository;

import com.english.learning_service.entity.UserAnswerGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAnswerGroupRepository extends JpaRepository<UserAnswerGroup,String> {
    List<UserAnswerGroup> findByExamHistoryId(String examHistoryId);
}
