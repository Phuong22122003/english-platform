package com.english.learning_service.dto.request;

import com.english.enums.Level;
import com.english.enums.StudyTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanIntentRequest {
    Integer target;
    String description;
    Level level;
    StudyTime studyTime;
    String jwt;
}
