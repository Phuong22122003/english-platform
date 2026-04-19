package com.english.learning_service.controller;

import com.english.learning_service.dto.request.ExamHistoryRequest;
import com.english.learning_service.dto.response.ExamHistoryResponse;
import com.english.learning_service.dto.response.RankingResponse;
import com.english.learning_service.enums.FilterType;
import com.english.learning_service.service.ExamHistoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/exam/history")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamHistoryController {
    ExamHistoryService examHistoryService;
    @PostMapping()
    public ResponseEntity<?> addExamHistory(@RequestBody ExamHistoryRequest request){
        return ResponseEntity.ok().body(examHistoryService.addExamHistory(request));
    }

    @GetMapping()
    public ResponseEntity<Page<ExamHistoryResponse>> getExamHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "ALL") FilterType filterType
    ) {
        Page<ExamHistoryResponse> histories = examHistoryService.getExamHistories(page, limit, filterType);
        return ResponseEntity.ok(histories);
    }
    @GetMapping("/{id}")
    public ResponseEntity<ExamHistoryResponse> getExamHistoryDetail(@PathVariable("id") String examHistoryId) {
        ExamHistoryResponse response = examHistoryService.getExamHistoryDetail(examHistoryId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/toeic/{id}/rankings")
    public ResponseEntity<List<RankingResponse>> getToeicRankings(@PathVariable(name = "id") String toeicId){
        return ResponseEntity.ok(examHistoryService.getToeicRankings(toeicId));
    }
}
