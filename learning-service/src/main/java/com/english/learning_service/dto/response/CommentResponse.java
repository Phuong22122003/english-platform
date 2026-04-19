package com.english.learning_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CommentResponse {
    private String id;
    private String username;
    private String avatarUrl;
    private String content;
    private String parentId;
    private List<CommentResponse> replyComments;
    private LocalDateTime commentedAt;
}
