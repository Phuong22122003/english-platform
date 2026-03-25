package com.english.learning_service.dto.response;

import com.english.learning_service.enums.ItemTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExamHistoryResponse {
    private String id;
    private ItemTypeEnum testType;
    private String testId;
    private int score;
    private String name;
    private Integer duration;
    private LocalDateTime takenAt;
    private LocalDateTime submittedAt;
    private List<UserAnswerGroupResponse> answerGroups;
}
