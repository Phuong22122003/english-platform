package com.english.dto.response;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToeicTestResponse {
    private String id;
    private String name;
    private Integer totalCompletion;
    List<String> partAudios;
    private LocalDateTime createdAt;
    private List<ToeicTestQuestionGroupResponse> questionGroups;

    public List<String> getPartAudios(){
        return List.of("https://zenlishtoeic.vn/wp-content/uploads/2022/08/DIRECTION-PART-1.mp3",
                "https://zenlishtoeic.vn/wp-content/uploads/2022/08/DIRECTION-PART-2.mp3",
                "https://zenlishtoeic.vn/wp-content/uploads/2022/08/DIRECTION-PART-3.mp3",
                "https://zenlishtoeic.vn/wp-content/uploads/2022/08/DIRECTION-PART-4.mp3");
    }

}
