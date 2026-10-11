package com.english.content_service.repository;

import com.english.content_service.entity.Vocabulary;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VocabularyRepository extends JpaRepository<Vocabulary, String> {
    // Add custom query methods here if needed
    public List<Vocabulary> findByTopicId(String topicId);

    @Modifying
    public void deleteByTopicId(String topicId);

    @Query("FROM Vocabulary WHERE lower(word) LIKE lower(concat(:key, '%'))")
    public List<Vocabulary> lookup(@Param("key") String key);
}
