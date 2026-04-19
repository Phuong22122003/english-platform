package com.english.learning_service.dto.request;

import com.english.learning_service.enums.StreamAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequest {
    private String id;
    private String username;
    private String avatarUrl;
    private LocalDateTime requestTime;
    private StreamAction action;
}
