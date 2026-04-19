package com.english.content_service.repository;

import com.english.content_service.entity.ToeicTestQuestionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ToeicTestQuestionGroupRepository extends JpaRepository<ToeicTestQuestionGroup, String> {
    @Query("""
           select g 
           from ToeicTestQuestionGroup g 
           left join fetch g.questions 
           where g.test.id = :testId
           order by g.groupOrder asc
        """)
    List<ToeicTestQuestionGroup> findByTestIdOrderByGroupOrderAsc(String testId);

    @Modifying
    public void deleteByTestId(String testId);
}
