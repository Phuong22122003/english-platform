package com.english.learning_service.dto.request;

import com.english.enums.Level;
import com.english.enums.StudyTime;
import com.english.learning_service.entity.ExamHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;



@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoRequest {
    String userId;
    String description;
    String target;
    Level level;
    List<ExamHistory> recentExamHistory;
    String studyTime;
}
