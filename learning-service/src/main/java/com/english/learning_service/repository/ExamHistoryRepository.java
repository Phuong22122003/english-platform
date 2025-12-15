package com.english.learning_service.repository;

import com.english.learning_service.entity.ExamHistory;
import com.english.learning_service.enums.ItemTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExamHistoryRepository extends JpaRepository<ExamHistory, String> {
    public Page<ExamHistory> findByUserId(String userId, Pageable pageable);
    public Page<ExamHistory> findByUserIdAndTestType(String userId, ItemTypeEnum testType, Pageable pageable);
    List<ExamHistory> findTop5ByUserIdOrderByTakenAtDesc(String userId);
    List<ExamHistory> findByTakenAtBetween(LocalDateTime start, LocalDateTime end);
    List<ExamHistory> findByUserIdAndTakenAtBetween(String userId,LocalDateTime start, LocalDateTime end);
}
