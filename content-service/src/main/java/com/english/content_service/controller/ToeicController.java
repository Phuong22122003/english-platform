package com.english.content_service.controller;

import com.english.content_service.dto.request.ToeicTestGroupRequest;
import com.english.content_service.dto.request.ToeicTestRequest;
import com.english.content_service.service.ToeicService;
import com.english.dto.response.ToeicTestGroupResponse;
import com.english.dto.response.ToeicTestResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/toeic")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class ToeicController {

    ToeicService toeicService;

    // =============================================================
    // GROUP
    // =============================================================

    @GetMapping("/groups")
    public ResponseEntity<Page<ToeicTestGroupResponse>> getGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(toeicService.getGroups(page, limit));
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<ToeicTestGroupResponse> getGroups(@PathVariable String id) {
        return ResponseEntity.ok(toeicService.getGroupById(id));
    }

    @PostMapping("/groups")
    public ResponseEntity<ToeicTestGroupResponse> addGroup(@RequestBody ToeicTestGroupRequest request) {
        return ResponseEntity.ok(toeicService.addTestGroup(request));
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<ToeicTestGroupResponse> updateGroup(
            @PathVariable String id,
            @RequestBody ToeicTestGroupRequest request
    ) {
        return ResponseEntity.ok(toeicService.updateTestGroup(id, request));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<String> deleteGroup(@PathVariable String id) {
        toeicService.deleteTestGroup(id);
        return ResponseEntity.ok("Deleted group successfully");
    }

    // =============================================================
    // TEST
    // =============================================================

    @GetMapping("/tests/{test_id}")
    public ResponseEntity<ToeicTestResponse> getTestById(@PathVariable(name = "test_id") String id) {
        return ResponseEntity.ok(toeicService.getTestById(id));
    }

    @GetMapping("/tests")
    public ResponseEntity<List<ToeicTestResponse>> getTestByIds(@RequestParam(name = "ids") List<String> ids) {
        return ResponseEntity.ok(toeicService.getTestByIds(ids));
    }
    @PatchMapping("/tests/{test_id}")
    public ResponseEntity<String> updateTotalCompletion(@PathVariable("test_id") String testId){
        toeicService.updateTotalComplete(testId);
        return ResponseEntity.ok("Updated successfully");
    }

    @PostMapping(
            value = "/groups/{groupId}/test",
            consumes = {"multipart/form-data"}
    )
    public ResponseEntity<ToeicTestResponse> addTest(
            @PathVariable String groupId,
            @RequestPart("data") ToeicTestRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "audios", required = false) List<MultipartFile> audios
    ) {
        return ResponseEntity.ok(toeicService.addTest(groupId, request, images, audios));
    }
    @PostMapping(
            value = "/groups/{groupId}/file-tests",
            consumes = {"multipart/form-data"}
    )
    public ResponseEntity<ToeicTestResponse> addTestByFile(
            @PathVariable String groupId,
            @RequestPart("data") ToeicTestRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "audios", required = false) List<MultipartFile> audios
    ) {
        return ResponseEntity.ok(toeicService.addTest(groupId, request, images, audios));
    }
    @PutMapping(
            value = "/tests/{id}",
            consumes = {"multipart/form-data"}
    )
    public ToeicTestResponse updateTest(
            @PathVariable String id,
            @RequestPart("data") ToeicTestRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "audios", required = false) List<MultipartFile> audios
    ) {
        return toeicService.updateTest(id, request, images, audios);
    }

    @DeleteMapping("/tests/{id}")
    public ResponseEntity<String> deleteTest(@PathVariable String id) {
        toeicService.deleteTest(id);
        return ResponseEntity.ok("Deleted test successfully");
    }
}
