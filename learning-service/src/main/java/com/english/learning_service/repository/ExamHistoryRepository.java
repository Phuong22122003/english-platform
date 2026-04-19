package com.english.learning_service.repository;

import com.english.learning_service.entity.ExamHistory;
import com.english.learning_service.enums.ItemTypeEnum;
import com.english.learning_service.repository.projections.RankingProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExamHistoryRepository extends JpaRepository<ExamHistory, String> {
    public Page<ExamHistory> findByUserIdOrderByTakenAtDesc(String userId, Pageable pageable);
    public Page<ExamHistory> findByUserIdAndTestType(String userId, ItemTypeEnum testType, Pageable pageable);
    List<ExamHistory> findTop5ByUserIdOrderByTakenAtDesc(String userId);
    List<ExamHistory> findByTakenAtBetween(LocalDateTime start, LocalDateTime end);
    List<ExamHistory> findByUserIdAndTakenAtBetween(String userId,LocalDateTime start, LocalDateTime end);

    @Query(value = """
        select h.user_id as userId, h.score as maxScore, h.attempts
        from(
                select e.score,
                       e.user_id,
                       e.taken_at,
                        row_number() over (partition by e.user_id order by e.score desc, e.taken_at asc) as no,
                        COUNT(*) OVER (PARTITION BY e.user_id) as attempts
                from exam_history e 
                where e.test_id = :toeicId
                ) as h
        where h.no = 1
        order by h.score, h.taken_at
        """, nativeQuery = true)
    public List<RankingProjection> findToeicScoreRanking(String toeicId);
}
