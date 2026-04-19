package com.english.learning_service.dto.response;

import lombok.Data;

@Data
public class RankingResponse {
    private Integer maxScore;
    private Integer attempts;
    private UserResponse user;
}
