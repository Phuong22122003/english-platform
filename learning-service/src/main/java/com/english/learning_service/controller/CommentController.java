package com.english.learning_service.controller;

import com.english.learning_service.dto.request.CommentRequest;
import com.english.learning_service.dto.response.CommentResponse;
import com.english.learning_service.service.CommentService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentController {

    CommentService commentService;

    @GetMapping("/tests/{test_id}")
    public ResponseEntity<List<CommentResponse>> getCommentsByTest(@PathVariable(name = "test_id") String testId) {
        return ResponseEntity.ok(commentService.getCommentsOfTest(testId));
    }

    @GetMapping("/tests/{test_id}/sse")
    public ResponseEntity<SseEmitter> getSSE(@PathVariable(name = "test_id") String testId){
        return ResponseEntity.ok(commentService.getSse(testId));
    }
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(@RequestBody @Valid CommentRequest request) {
        return ResponseEntity.ok(commentService.addComment(request));
    }
    @DeleteMapping("/{comment_id}")
    public void deleteComment(@PathVariable(name = "comment_id") String commentId){
        commentService.deleteComment(commentId);
    }
    @DeleteMapping("/admin/{comment_id}")
    public void deleteCommentByAdmin(@PathVariable(name = "comment_id") String commentId){
        commentService.deleteCommentByAdmin(commentId);
    }
}