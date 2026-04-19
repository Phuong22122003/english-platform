package com.english.learning_service.service;

import com.english.learning_service.dto.request.CommentRequest;
import com.english.learning_service.dto.response.CommentResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Service
public interface CommentService {
    public List<CommentResponse> getCommentsOfTest(String testId);
    public CommentResponse addComment(CommentRequest comment);
    public SseEmitter getSse(String testId);
    public void deleteComment(String commentId);
    public void deleteCommentByAdmin(String commentId);
}
