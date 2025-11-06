package com.english.content_service.repository;

import com.english.content_service.entity.TopicViewStatistic;
import com.english.enums.TopicType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TopicViewStatisticRepository extends JpaRepository<TopicViewStatistic, Integer> {
    @Query("""
        SELECT s.topicId AS topicId,
               s.topicType AS topicType,
               SUM(s.viewCount) AS totalViews
        FROM TopicViewStatistic s
        GROUP BY s.topicId, s.topicType
        ORDER BY SUM(s.viewCount) DESC
        """)
    List<TopicViewSummary> findTopTopics(Pageable pageable);
    List<TopicViewStatistic> findByViewDateBetween(LocalDate startDate, LocalDate endDate);

    Optional<TopicViewStatistic> findByTopicIdAndTopicTypeAndViewDateAndViewHour(
            String topicId,
            TopicType topicType,
            LocalDate viewDate,
            int viewHour
    );
}
