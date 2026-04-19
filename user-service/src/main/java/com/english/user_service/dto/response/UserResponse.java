package com.english.user_service.dto.response;

import java.time.LocalDateTime;


import com.english.enums.Level;
import com.english.enums.StudyTime;
import com.english.user_service.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class UserResponse {
    String id;
    String username;
    String email;
    UserRole role;
    String fullname;
    String avatarUrl;
    Integer target; // target study time per day (minutes)
    StudyTime studyTime; // morning, afternoon, evening, night
    Level level; // beginner, intermediate, advanced
    LocalDateTime createdAt; 
}
