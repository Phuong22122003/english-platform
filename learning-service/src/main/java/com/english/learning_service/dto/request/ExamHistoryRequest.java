package com.english.learning_service.dto.request;

import com.english.learning_service.entity.UserAnswerGroup;
import com.english.learning_service.enums.ItemTypeEnum;
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
public class ExamHistoryRequest {
    private String testId;
    private ItemTypeEnum testType;
    private String name;
    private int score;
    private int duration;
    private List<UserAnswerGroupRequest> answerGroups;
    private LocalDateTime takenAt;
    private LocalDateTime submittedAt;
}
