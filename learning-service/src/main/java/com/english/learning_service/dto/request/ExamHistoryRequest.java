package com.english.learning_service.dto.request;

import com.english.learning_service.entity.UserAnswerGroup;
import com.english.learning_service.enums.ItemTypeEnum;
import jakarta.validation.constraints.NotNull;
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
    @NotNull
    private String testId;
    @NotNull
    private ItemTypeEnum testType;
    @NotNull
    private String name;
    @NotNull
    private Integer score;
    @NotNull
    private Integer duration;
    @NotNull
    private List<UserAnswerGroupRequest> answerGroups;
    private LocalDateTime takenAt;
    private LocalDateTime submittedAt;
}
