package com.english.user_service.dto.request;

import com.english.user_service.enums.StreamAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserStreamRequest {
    private String id;
    private String username;
    private String avatarUrl;
    private LocalDateTime requestTime;
    private StreamAction action;
}
