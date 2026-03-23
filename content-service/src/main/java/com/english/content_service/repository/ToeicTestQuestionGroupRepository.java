package com.english.content_service.repository;

import com.english.content_service.entity.ToeicTestQuestionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface ToeicTestQuestionGroupRepository extends JpaRepository<ToeicTestQuestionGroup, String> {
    List<ToeicTestQuestionGroup> findByTestId(String testId);

    @Modifying
    public void deleteByTestId(String testId);
}
