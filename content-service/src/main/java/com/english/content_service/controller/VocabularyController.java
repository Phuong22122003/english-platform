package com.english.content_service.controller;

import com.english.content_service.dto.request.VocabularyRequest;
import com.english.content_service.dto.request.VocabularyTestRequest;
import com.english.content_service.dto.request.VocabTopicRequest;
import com.english.content_service.service.VocabularyService;
import com.english.dto.response.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/vocabulary")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class VocabularyController {

    VocabularyService vocabularyService;

    // ========================= TOPIC =========================

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String q, @RequestParam Integer page, @RequestParam Integer limit){
        return ResponseEntity.ok(vocabularyService.search(q,page,limit));
    }

    @GetMapping("/topics")
    public ResponseEntity<?> getAllTopics(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(vocabularyService.getTopics(page, size));
    }

    @GetMapping("/topics/ids")
    public ResponseEntity<?> getTopicsByIds(@RequestParam("ids") List<String> ids) {
        return ResponseEntity.ok(vocabularyService.getTopicsByIds(ids));
    }

    @PostMapping("/topics")
    public ResponseEntity<VocabTopicResponse> createTopic(
            @RequestPart("topic") VocabTopicRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        return ResponseEntity.ok(vocabularyService.addTopic(request, imageFile));
    }

    @PutMapping("/topics/{topic_id}")
    public ResponseEntity<VocabTopicResponse> updateTopic(
            @PathVariable("topic_id") String topicId,
            @RequestPart("topic") VocabTopicRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        return ResponseEntity.ok(vocabularyService.updateTopic(topicId, request, imageFile));
    }

    @DeleteMapping("/topics/{topic_id}")
    public ResponseEntity<ApiResponse<String>> deleteTopic(@PathVariable("topic_id") String topicId) {
        vocabularyService.deleteTopic(topicId);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .message("Deleted topic successfully")
                .build());
    }

    // ========================= VOCABULARY =========================

    @GetMapping("/topics/{topic_id}/vocabularies")
    public ResponseEntity<?> getVocabulariesByTopic(@PathVariable("topic_id") String topicId) {
        return ResponseEntity.ok(vocabularyService.getVocabulariesByTopicId(topicId));
    }

    @PostMapping("/topics/{topic_id}/vocabularies")
    public ResponseEntity<?> addVocabularies(
            @PathVariable("topic_id") String topicId,
            @RequestPart("vocabularies") List<VocabularyRequest> requests,
            @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles,
            @RequestPart(value = "audios", required = false) List<MultipartFile> audioFiles) {
        return ResponseEntity.ok(vocabularyService.addVocabularies(topicId, requests, imageFiles, audioFiles));
    }
    @PostMapping("/topics/{topic_id}/file-vocabularies")
    public ResponseEntity<?> addVocabulariesByFile(
            @PathVariable("topic_id") String topicId,
            @RequestPart("vocabulary_file") MultipartFile excelFile,
            @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles,
            @RequestPart(value = "audios", required = false) List<MultipartFile> audioFiles) {
        return ResponseEntity.ok(vocabularyService.addVocabularies(topicId, excelFile, imageFiles, audioFiles));
    }

    @PutMapping("/topics/{topic_id}/vocabularies")
    public ResponseEntity<?> updateVocabularies(
            @PathVariable("topic_id") String topicId,
            @RequestPart("vocabularies") List<VocabularyRequest> requests,
            @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles,
            @RequestPart(value = "audios", required = false) List<MultipartFile> audioFiles) {
        return ResponseEntity.ok(vocabularyService.updateVocabularies(topicId, requests, imageFiles, audioFiles));
    }

    @PutMapping("/vocabularies/{vocab_id}")
    public ResponseEntity<VocabularyResponse> updateVocabulary(
            @PathVariable("vocab_id") String vocabId,
            @RequestPart("request") VocabularyRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            @RequestPart(value = "audio", required = false) MultipartFile audioFile) {
        return ResponseEntity.ok(vocabularyService.updateVocabulary(vocabId, request, imageFile, audioFile));
    }

    @DeleteMapping("/vocabularies/{vocab_id}")
    public ResponseEntity<ApiResponse<String>> deleteVocabulary(@PathVariable("vocab_id") String vocabId) {
        vocabularyService.deleteVocabulary(vocabId);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .message("Deleted vocabulary successfully")
                .build());
    }

    // ========================= TEST =========================

    @GetMapping("/topics/{topic_id}/tests")
    public ResponseEntity<?> getTestsByTopic(@PathVariable("topic_id") String topicId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(vocabularyService.getTestsByTopicId(topicId, page, size));
    }

    @PostMapping("/topics/{topic_id}/tests")
    public ResponseEntity<VocabularyTestResponse> addTest(
            @PathVariable("topic_id") String topicId,
            @RequestPart("test") VocabularyTestRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles) {
        return ResponseEntity.ok(vocabularyService.addTest(topicId, request, imageFiles));
    }

    @PutMapping("/tests/{test_id}")
    public ResponseEntity<VocabularyTestResponse> updateTest(
            @PathVariable("test_id") String testId,
            @RequestPart("test") VocabularyTestRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles) {
        return ResponseEntity.ok(vocabularyService.updateTest(testId, request, imageFiles));
    }

    @GetMapping("/tests/{test_id}/questions")
    public ResponseEntity<?> getTestQuestions(@PathVariable("test_id") String testId) {
        return ResponseEntity.ok(vocabularyService.getTestQuestionsByTestId(testId));
    }

    @GetMapping("/tests")
    public ResponseEntity<?> getTestsByIds(@RequestParam("ids") List<String> ids) {
        return ResponseEntity.ok(vocabularyService.getTestsByIds(ids));
    }

    @DeleteMapping("/tests/{test_id}")
    public ResponseEntity<ApiResponse<String>> deleteTest(@PathVariable("test_id") String testId) {
        vocabularyService.deleteTest(testId);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .message("Deleted test successfully")
                .build());
    }
}
