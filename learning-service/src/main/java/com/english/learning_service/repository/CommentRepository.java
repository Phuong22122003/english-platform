package com.english.learning_service.repository;

import com.english.learning_service.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String> {
    public List<Comment> findByTestIdOrderByCommentedAtDesc(String testId);
}
