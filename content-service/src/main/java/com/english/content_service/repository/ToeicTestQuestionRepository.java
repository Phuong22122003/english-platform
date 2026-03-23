package com.english.content_service.repository;

import com.english.content_service.entity.ToeicTestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToeicTestQuestionRepository extends JpaRepository<ToeicTestQuestion, String> {
//    List<ToeicTestQuestion> findByTestId(String testId);

//    void deleteByTestId(String testId);
}
