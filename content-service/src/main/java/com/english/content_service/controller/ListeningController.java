package com.english.content_service.controller;

import com.english.content_service.dto.request.ListeningRequest;
import com.english.content_service.dto.request.ListeningTestRequest;
import com.english.content_service.dto.request.ListeningTopicRequest;
import com.english.dto.response.*;
import com.english.content_service.service.ListeningService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/listening")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class ListeningController {

    ListeningService listeningService;

    // ========================= TOPIC =========================
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String q, @RequestParam Integer page, @RequestParam Integer limit){
        return ResponseEntity.ok(listeningService.search(q,page,limit));
    }

    @GetMapping("/topics")
    public ResponseEntity<?> getTopics(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(listeningService.getTopics(page, size));
    }

    @PostMapping("/topics")
    public ResponseEntity<ListeningTopicResponse> addTopic(
            @RequestPart("topic") ListeningTopicRequest request,
            @RequestPart("image") MultipartFile image) {
        return ResponseEntity.ok(listeningService.addTopic(request, image));
    }
    @PostMapping("/topics/file")
    public ResponseEntity<ListeningTopicResponse> addTopic(
            @RequestPart("topic") ListeningTopicRequest topicRequest,
            @RequestPart("topic_image") MultipartFile topicImage,
            @RequestPart("listening") List<ListeningRequest> listeningRequests,
            @RequestPart(value = "listening_images", required = false) List<MultipartFile> listeningImageFiles,
            @RequestPart(value = "listening_audios", required = false) List<MultipartFile> listeningAudioFiles
    ){
        ListeningTopicResponse listeningTopicResponse = listeningService.addTopic(topicRequest,topicImage);
        listeningTopicResponse.setListenings(listeningService.addListeningList(listeningTopicResponse.getId(),listeningRequests,listeningImageFiles,listeningAudioFiles));
        return  ResponseEntity.ok(listeningTopicResponse);
    }
    @PutMapping("/topics/{id}")
    public ResponseEntity<ListeningTopicResponse> updateTopic(
            @PathVariable("id") String topicId,
            @RequestPart("topic") ListeningTopicRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.ok(listeningService.updateTopic(topicId, request, image));
    }

    @GetMapping("/topics/ids")
    public ResponseEntity<?> getTopicsByIds(@RequestParam("ids") List<String> ids) {
        return ResponseEntity.ok(listeningService.getTopicsByIds(ids));
    }

    @DeleteMapping("/topics/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTopic(@PathVariable("id") String topicId) {
        listeningService.deleteTopic(topicId);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .message("Deleted topic successfully")
                .build());
    }

    // ========================= LISTENING =========================

    @GetMapping("/topics/{topic_id}/listenings")
    public ResponseEntity<?> getListeningsByTopic(@PathVariable("topic_id") String topicId) {
        return ResponseEntity.ok(listeningService.getListeningByTopic(topicId));
    }

    @PostMapping("/topics/{topic_id}/listenings")
    public ResponseEntity<?> addListeningList(
            @PathVariable("topic_id") String topicId,
            @RequestPart("requests") List<ListeningRequest> requests,
            @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles,
            @RequestPart(value = "audios", required = false) List<MultipartFile> audioFiles) {
        return ResponseEntity.ok(listeningService.addListeningList(topicId, requests, imageFiles, audioFiles));
    }
    @PostMapping("/topics/{topic_id}/file-listening")
    public ResponseEntity<?> addListeningsByExcelFile(
            @PathVariable("topic_id") String topicId,
            @RequestPart("listening_file") MultipartFile excelFile,
            @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles,
            @RequestPart(value = "audios", required = false) List<MultipartFile> audioFiles) {
        return ResponseEntity.ok(listeningService.addListeningList(topicId, excelFile, imageFiles, audioFiles));
    }

    @PutMapping("/listenings")
    public ResponseEntity<?> updateListeningList(
            @RequestPart("requests") List<ListeningRequest> requests,
            @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles,
            @RequestPart(value = "audios", required = false) List<MultipartFile> audioFiles) {
        return ResponseEntity.ok(listeningService.updateListening(requests, imageFiles, audioFiles));
    }

    @DeleteMapping("/listenings/{id}")
    public ResponseEntity<ApiResponse<String>> deleteListening(@PathVariable("id") String listeningId) {
        listeningService.deleteListening(listeningId);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .message("Deleted listening successfully")
                .build());
    }

    // ========================= TEST =========================

    @GetMapping("/topics/{topic_id}/tests")
    public ResponseEntity<?> getTestsByTopic(@PathVariable("topic_id") String topicId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(listeningService.getTestsByTopic(topicId, page, size));
    }

    @PostMapping("/topics/{topic_id}/tests")
    public ResponseEntity<ListeningTestReponse> addTest(
            @PathVariable("topic_id") String topicId,
            @RequestPart("request") ListeningTestRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles,
            @RequestPart(value = "audios", required = false) List<MultipartFile> audioFiles) {
        return ResponseEntity.ok(listeningService.addTest(topicId, request, imageFiles, audioFiles));
    }
    @PostMapping("/topics/{topic_id}/file-tests")
    public ResponseEntity<ListeningTestReponse> addTestByExcelFile(
            @PathVariable("topic_id") String topicId,
            @RequestPart("test_file") MultipartFile excelFile,
            @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles,
            @RequestPart(value = "audios", required = false) List<MultipartFile> audioFiles) {
        return ResponseEntity.ok(listeningService.addTest(topicId, excelFile, imageFiles, audioFiles));
    }

    @GetMapping("/tests/{test_id}")
    public ResponseEntity<ListeningTestReponse> getTestDetail(@PathVariable("test_id") String testId) {
        return ResponseEntity.ok(listeningService.getTestDetail(testId));
    }

    @GetMapping("/tests")
    public ResponseEntity<?> getTestsByIds(@RequestParam("ids") List<String> ids) {
        return ResponseEntity.ok(listeningService.getTestByIds(ids));
    }

    @PutMapping("/tests/{test_id}")
    public ResponseEntity<ListeningTestReponse> updateTest(
            @PathVariable("test_id") String testId,
            @RequestPart("request") ListeningTestRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> imageFiles,
            @RequestPart(value = "audios", required = false) List<MultipartFile> audioFiles) {
        return ResponseEntity.ok(listeningService.updateTest(testId, request, imageFiles, audioFiles));
    }

    @DeleteMapping("/tests/{test_id}")
    public ResponseEntity<ApiResponse<String>> deleteTest(@PathVariable("test_id") String testId) {
        listeningService.deleteTest(testId);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .message("Deleted test successfully")
                .build());
    }
}
