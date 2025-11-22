package com.english.content_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToeicTestGroupRequest {
    private String name;
    private LocalDateTime releaseDate;
}
