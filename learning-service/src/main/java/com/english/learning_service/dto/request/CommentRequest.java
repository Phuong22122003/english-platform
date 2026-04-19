package com.english.learning_service.dto.request;

import lombok.Data;

@Data
public class CommentRequest {
    private String parentId;
    private String replyId;
    private String content;
    private String testId;
}