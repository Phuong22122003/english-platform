package com.english.content_service.repository;

import com.english.content_service.entity.ToeicTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ToeicTestRepository extends JpaRepository<ToeicTest, String> {
    List<ToeicTest> findByGroupId(String groupId);

    @Query(value = "SELECT * FROM toeic_test WHERE group_id = :groupId ORDER BY created_at ASC",nativeQuery = true)
    List<ToeicTest> findByGroupIdOrderByCreatedAtAsc(String groupId);

}
