package com.english.content_service.repository;

import com.english.content_service.entity.ToeicTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToeicTestRepository extends JpaRepository<ToeicTest, String> {
    List<ToeicTest> findByGroupId(String groupId);
}
