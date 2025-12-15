package com.english.content_service.controller;

import com.english.content_service.dto.request.GrammarRequest;
import com.english.content_service.dto.request.GrammarTestRequest;
import com.english.content_service.dto.request.GrammarTopicRequest;
import com.english.dto.response.*;
import com.english.content_service.service.GrammarService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/grammar")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class GrammarController {

    GrammarService grammarService;

    // ========================= TOPIC =========================
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String q, @RequestParam Integer page, @RequestParam Integer limit){
        return ResponseEntity.ok(grammarService.search(q,page,limit));
    }

    @GetMapping("/topics")
    public ResponseEntity<?> getTopics(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(grammarService.getTopics(page, size));
    }

    @PostMapping("/topics")
    public ResponseEntity<GrammarTopicResponse> addTopic(
            @RequestPart("topic") GrammarTopicRequest request,
            @RequestPart("image") MultipartFile image) {
        return ResponseEntity.ok(grammarService.addTopic(request, image));
    }

    @PutMapping("/topics/{id}")
    public ResponseEntity<GrammarTopicResponse> updateTopic(
            @PathVariable String id,
            @RequestPart("topic") GrammarTopicRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.ok(grammarService.updateTopic(id, request, image));
    }

    @GetMapping("/topics/ids")
    public ResponseEntity<?> getTopicsByIds(@RequestParam("ids") List<String> ids) {
        return ResponseEntity.ok(grammarService.getTopicsByIds(ids));
    }

    @DeleteMapping("/topics/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTopicById(@PathVariable String id) {
        grammarService.deleteTopicById(id);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .message("Deleted topic successfully")
                .build());
    }

    // ========================= GRAMMAR =========================

    @GetMapping("/topics/{topic_id}/grammars")
    public ResponseEntity<?> getGrammarsByTopicId(@PathVariable("topic_id") String topicId) {
        return ResponseEntity.ok(grammarService.getGrammarsByTopicId(topicId));
    }

    @PostMapping("/topics/{topic_id}/grammars")
    public ResponseEntity<?> addGrammar(@PathVariable("topic_id") String topicId,
                                        @RequestBody GrammarRequest grammarRequest) {
        return ResponseEntity.ok(grammarService.addGrammar(topicId, grammarRequest));
    }

    @PutMapping("/grammars/{grammar_id}")
    public ResponseEntity<?> updateGrammar(@PathVariable("grammar_id") String grammarId,
                                           @RequestBody GrammarRequest request) {
        return ResponseEntity.ok(grammarService.updateGrammar(grammarId, request));
    }

    @DeleteMapping("/grammars/{grammar_id}")
    public ResponseEntity<ApiResponse<String>> deleteGrammarById(@PathVariable("grammar_id") String grammarId) {
        grammarService.deleteGrammarById(grammarId);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .message("Deleted grammar successfully")
                .build());
    }

    // ========================= TEST =========================

    @GetMapping("/grammars/{grammar_id}/tests")
    public ResponseEntity<?> getTestsByGrammarId(@PathVariable("grammar_id") String grammarId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(grammarService.getTestsByGrammarId(grammarId, page, size));
    }

    @GetMapping("/tests/{test_id}/questions")
    public ResponseEntity<?> getTestQuestionsByTestId(@PathVariable("test_id") String testId) {
        return ResponseEntity.ok(grammarService.getTestQuestionsByTestId(testId));
    }

    @PostMapping("/grammars/{grammar_id}/tests")
    public ResponseEntity<?> addTest(@PathVariable("grammar_id") String grammarId,
                                     @RequestBody GrammarTestRequest testRequest) {
        return ResponseEntity.ok(grammarService.addTest(grammarId, testRequest));
    }
    @PostMapping("/topics/{grammar_id}/file-tests")
    public ResponseEntity<?> addTestByExcelFile(@PathVariable("grammar_id") String grammarId,
                                                @RequestPart("test_file") MultipartFile excelFile) {
        return ResponseEntity.ok(grammarService.addTest(grammarId, excelFile));
    }
    @PutMapping("/tests/{test_id}")
    public ResponseEntity<?> updateGrammarTest(@PathVariable("test_id") String testId,
                                               @RequestBody GrammarTestRequest request) {
        return ResponseEntity.ok(grammarService.updateGrammarTest(testId, request));
    }

    @DeleteMapping("/tests/{test_id}")
    public ResponseEntity<ApiResponse<String>> deleteTestById(@PathVariable("test_id") String testId) {
        grammarService.deleteTestById(testId);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .message("Deleted test successfully")
                .build());
    }

    @GetMapping("/tests")
    public ResponseEntity<?> getTestsByIds(@RequestParam("ids") List<String> ids) {
        return ResponseEntity.ok(grammarService.getTestsByIds(ids));
    }
}
