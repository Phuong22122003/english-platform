package com.english.user_service.dto.request;


import com.english.user_service.enums.StudyTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class UserProfileUpdateRequest {
    Integer target; // target eg 550
    StudyTime studyTime; // morning, afternoon, evening, night
    String level; // beginner, intermediate, advanced
    String fullname;
}
