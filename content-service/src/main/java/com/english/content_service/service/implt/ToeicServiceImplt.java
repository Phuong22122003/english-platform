package com.english.content_service.service.implt;

import com.english.content_service.dto.request.ToeicTestGroupRequest;
import com.english.content_service.dto.request.ToeicTestQuestionGroupRequest;
import com.english.content_service.dto.request.ToeicTestQuestionRequest;
import com.english.content_service.dto.request.ToeicTestRequest;
import com.english.content_service.entity.*;
import com.english.content_service.mapper.ToeicMapper;
import com.english.content_service.repository.ToeicTestGroupRepository;
import com.english.content_service.repository.ToeicTestQuestionGroupRepository;
import com.english.content_service.repository.ToeicTestQuestionRepository;
import com.english.content_service.repository.ToeicTestRepository;
import com.english.content_service.service.ToeicService;
import com.english.dto.response.FileResponse;
import com.english.dto.response.ToeicTestGroupResponse;
import com.english.dto.response.ToeicTestResponse;
import com.english.exception.NotFoundException;
import com.english.service.FileService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ToeicServiceImplt implements ToeicService {

    ToeicTestRepository toeicTestRepository;
    ToeicTestGroupRepository toeicTestGroupRepository;
    ToeicTestQuestionRepository toeicTestQuestionRepository;
    ToeicTestQuestionGroupRepository toeicTestQuestionGroupRepository;
    ToeicMapper toeicMapper;
    FileService fileService;

    // ========================================================================
    // GROUP
    // ========================================================================

    @Override
    public Page<ToeicTestGroupResponse> getGroups(int page, int limit) {
        Page<ToeicTestGroup> groups = toeicTestGroupRepository.findAll(PageRequest.of(page, limit));
        var responses = toeicMapper.toGroupResponses(groups.getContent());
        List<ToeicTest> tests;
        for(var response: responses){
            tests = toeicTestRepository.findByGroupId(response.getId());
            response.setTests(toeicMapper.toTestResponses(tests));
        }
        return new PageImpl<>(responses, PageRequest.of(page, limit), groups.getTotalElements());
    }
    @Override
    public ToeicTestGroupResponse getGroupById(String id) {
        ToeicTestGroup groups = toeicTestGroupRepository.findById(id).orElseThrow(() -> new NotFoundException("Can not found this group"));
        var response = toeicMapper.toGroupResponse(groups);
        List<ToeicTest> tests;
        tests = toeicTestRepository.findByGroupId(response.getId());
        response.setTests(toeicMapper.toTestResponses(tests));
        return response;
    }
    @Override
    @Transactional
    public ToeicTestGroupResponse addTestGroup(ToeicTestGroupRequest request) {
        ToeicTestGroup group = ToeicTestGroup.builder()
                .name(request.getName())
                .releaseDate(request.getReleaseDate())
                .createdAt(LocalDateTime.now())
                .build();

        toeicTestGroupRepository.save(group);
        return toeicMapper.toGroupResponse(group);
    }

    @Override
    @Transactional
    public void deleteTestGroup(String id) {
        List<ToeicTest> tests = toeicTestRepository.findByGroupId(id);
        for (var t : tests) {
            deleteTest(t.getId());
        }
        toeicTestGroupRepository.deleteById(id);
    }

    @Override
    @Transactional
    public ToeicTestGroupResponse updateTestGroup(String id,ToeicTestGroupRequest request) {
        ToeicTestGroup group = toeicTestGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Test group not found"));

        group.setName(request.getName());
        group.setReleaseDate(request.getReleaseDate());

        toeicTestGroupRepository.save(group);
        return toeicMapper.toGroupResponse(group);
    }

    @Override
    public void updateTotalComplete(String testId) {
        ToeicTest test = toeicTestRepository.findById(testId).orElseThrow(()->new NotFoundException("Test not found"));
        test.setTotalCompletion(test.getTotalCompletion()+1);
        toeicTestRepository.save(test);
    }

    @Override
    public List<ToeicTestResponse> getTestByIds(List<String> ids) {
        List<ToeicTest> tests = toeicTestRepository.findAllById(ids);
        return toeicMapper.toTestResponses(tests);
    }

    @Override
    public ToeicTestResponse getTestById(String id) {
        ToeicTest test = toeicTestRepository.findById(id)
                .orElseThrow(()->new NotFoundException("Test not found"));
        List<ToeicTestQuestionGroup> questionGroups = toeicTestQuestionGroupRepository.findByTestId(test.getId());
        ToeicTestResponse response = toeicMapper.toTestResponse(test);
        response.setQuestionGroups(toeicMapper.toQuestionGroupResponses(questionGroups));
        return response;
    }


    // ========================================================================
    // TEST
    // ========================================================================
//    Toeic Test
//    NAME	ETS 2024
//    PART 1
//    GROUP QUESTION	A	B	C	D	CORRECT ANSWER	EXPLANATION	IMAGE NAME	AUDIO NAME
//    PART 2
//    GROUP QUESTION	A	B	C	D	CORRECT ANSWER	EXPLANATION	IMAGE NAME	AUDIO NAME
//    PART 3
//    GROUP QUESTION	A	B	C	D	CORRECT ANSWER	EXPLANATION	IMAGE NAME	AUDIO NAME
//    PART 4
//    GROUP QUESTION	A	B	C	D	CORRECT ANSWER	EXPLANATION	IMAGE NAME	AUDIO NAME
//    PART 5
//    GROUP QUESTION	A	B	C	D	CORRECT ANSWER	EXPLANATION	IMAGE NAME	AUDIO NAME
//    PART 6
//    GROUP QUESTION	A	B	C	D	CORRECT ANSWER	EXPLANATION	IMAGE NAME	AUDIO NAME
//    PART 7
//    GROUP QUESTION	A	B	C	D	CORRECT ANSWER	EXPLANATION	IMAGE NAME	AUDIO NAME

    @Override
    @Transactional
    public ToeicTestResponse addTest(
            String groupId,
            MultipartFile excelFile,
            List<MultipartFile> imageFiles,
            List<MultipartFile> audioFiles) {

        int rowAt = 0, columnAt = 0;

        log.info("📝 [ADD TEST EXCEL] groupId={}, fileName={}, size={}",
                groupId,
                excelFile != null ? excelFile.getOriginalFilename() : "null",
                excelFile != null ? excelFile.getSize() : 0);

        try (InputStream is = excelFile.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            ToeicTestRequest request = new ToeicTestRequest();
            request.setName(getCellValueAsString(sheet.getRow(1).getCell(1)));
            log.info("📌 Test name from Excel (row=2,col=B): '{}'",
                    request.getName());

            request.setQuestionGroups(new ArrayList<>());
            ToeicTestQuestionGroupRequest questionGroup = null;
            int group = 0;
            for (int i = 2; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);

                int currentGroup = Integer.valueOf(getCellValueAsString(row.getCell(0)));
                if(currentGroup!=group){
                    group = currentGroup;
                    questionGroup = new ToeicTestQuestionGroupRequest();

                    columnAt = 8;
                    questionGroup.setImageName(getCellValueAsString(row.getCell(8)));

                    columnAt = 9;
                    questionGroup.setAudioName(getCellValueAsString(row.getCell(9)));

                    columnAt = 10;
                    if (i < 9) {
                        questionGroup.setPart(1);
                    } else if (i < 35) {
                        questionGroup.setPart(2);
                    } else if (i < 75) {
                        questionGroup.setPart(3);
                    } else if (i < 106) {
                        questionGroup.setPart(4);
                    } else if (i < 137) {
                        questionGroup.setPart(5);
                    } else if (i < 154) {
                        questionGroup.setPart(6);
                    } else {
                        questionGroup.setPart(7);
                    }
                    questionGroup.setQuestions(new ArrayList<>());
                    request.getQuestionGroups().add(questionGroup);
                }

                ToeicTestQuestionRequest question = new ToeicTestQuestionRequest();

                columnAt = 1;
                question.setQuestion(getCellValueAsString(row.getCell(1)));

                Options options = new Options();

                columnAt = 2;
                options.setA(getCellValueAsString(row.getCell(2)));
                columnAt = 3;
                options.setB(getCellValueAsString(row.getCell(3)));
                columnAt = 4;
                options.setC(getCellValueAsString(row.getCell(4)));
                columnAt = 5;
                options.setD(getCellValueAsString(row.getCell(5)));

                question.setOptions(options);

                columnAt = 6;
                question.setCorrectAnswer(getCellValueAsString(row.getCell(6)));

                columnAt = 7;
                question.setExplanation(getCellValueAsString(row.getCell(7)));

                questionGroup.getQuestions().add(question);
            }

            return addTest(groupId, request, imageFiles, audioFiles);

        } catch (Exception e) {
            log.error("❌ Error while parsing Excel at rowAt={}, columnAt={}, excelRow={}, excelCol={} -> {}",
                    rowAt,
                    columnAt,
                    (rowAt + 1),
                    (columnAt + 1),
                    e.getMessage(),
                    e);

            throw new RuntimeException("{row:" + rowAt + ",column:" + columnAt + "}");
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
    @Override
    @Transactional
    public ToeicTestResponse addTest(
            String groupId,
            ToeicTestRequest request,
            List<MultipartFile> imageFiles,
            List<MultipartFile> audioFiles) {

        log.info("➕ [ADD TEST] groupId={}, testName={}", groupId, request.getName());
        log.info("📌 Total questions group from request: {}", request.getQuestionGroups() != null ? request.getQuestionGroups().size() : 0);

        ToeicTestGroup group = toeicTestGroupRepository.findById(groupId)
                .orElseThrow(() -> {
                    log.error("❌ Group not found: {}", groupId);
                    return new NotFoundException("Group not found");
                });

        ToeicTest test = ToeicTest.builder()
                .name(request.getName())
                .createdAt(LocalDateTime.now())
                .totalCompletion(0)
                .group(group)
                .build();

        test = toeicTestRepository.save(test);
        log.info("📝 Test created: id={}, name={}", test.getId(), test.getName());

        List<ToeicTestQuestionGroup> questionGroups = toeicMapper.toQuestionGroups(request.getQuestionGroups());


        Set<String> uploadedPublicIds = new HashSet<>();
        Map<String, FileResponse> fileResponseMap = new HashMap<>();

        // ------------------------
        // UPLOAD FILES (IMAGE/AUDIO)
        // ------------------------
        try {
            if (imageFiles != null) {
                log.info("📸 Uploading {} image files...", imageFiles.size());
                for (MultipartFile img : imageFiles) {
                    log.info("➡️ Upload image: {}", img.getOriginalFilename());
                    FileResponse fileResponse = fileService.uploadImage(img);
                    log.info("   ✔ Uploaded image: name={}, url={}", img.getOriginalFilename(), fileResponse.getUrl());

                    fileResponseMap.put(img.getOriginalFilename(), fileResponse);
                    uploadedPublicIds.add(fileResponse.getPublicId());
                }
            }

            if (audioFiles != null) {
                log.info("🔊 Uploading {} audio files...", audioFiles.size());
                for (MultipartFile audio : audioFiles) {
                    log.info("➡️ Upload audio: {}", audio.getOriginalFilename());
                    FileResponse fileResponse = fileService.uploadAudio(audio);
                    log.info("   ✔ Uploaded audio: name={}, url={}", audio.getOriginalFilename(), fileResponse.getUrl());

                    fileResponseMap.put(audio.getOriginalFilename(), fileResponse);
                    uploadedPublicIds.add(fileResponse.getPublicId());
                }
            }
        } catch (Exception e) {
            log.error("❌ Error while uploading files: {}", e.getMessage());
            log.warn("🗑 Rolling back uploaded files...");
            uploadedPublicIds.forEach(pid -> {
                try {
                    fileService.deleteFile(pid);
                    log.info("   ✔ Deleted uploaded file pid={}", pid);
                } catch (Exception ignore) {}
            });
            throw new RuntimeException("Can not upload file");
        }

        // ------------------------
        // MAP IMAGE & AUDIO TO QUESTIONS
        // ------------------------
        log.info("🔗 Mapping images/audio to question groups...");

        for (int i = 0; i < questionGroups.size(); i++) {

            ToeicTestQuestionGroup qg = questionGroups.get(i);
            ToeicTestQuestionGroupRequest qgr = request.getQuestionGroups().get(i);

            qg.setTest(test);

            // Image
            if (qgr.getImageName() != null && !qgr.getImageName().isEmpty()) {
                FileResponse img = fileResponseMap.get(qgr.getImageName());
                log.info("   🖼 ImageName={} -> {}", qgr.getImageName(), img != null ? "FOUND" : "NOT FOUND");

                if (img != null) {
                    qg.setImageUrl(img.getUrl());
                    qg.setPublicImageId(img.getPublicId());
                }
            }

            // Audio
            if (qgr.getAudioName() != null && !qgr.getAudioName().isEmpty()) {
                FileResponse audio = fileResponseMap.get(qgr.getAudioName());
                log.info("   🔊 AudioName={} -> {}", qgr.getAudioName(), audio != null ? "FOUND" : "NOT FOUND");

                if (audio != null) {
                    qg.setAudioUrl(audio.getUrl());
                    qg.setPublicAudioId(audio.getPublicId());
                }
            }
        }

        // ------------------------
        // SAVE QUESTIONS INTO DB
        // ------------------------
        try {
            toeicTestQuestionGroupRepository.saveAll(questionGroups);
            log.info("💾 Saved {} question groups to DB successfully!", questionGroups.size());
        } catch (Exception e) {
            log.error("❌ Error saving questions: {}", e.getMessage());
            log.warn("🗑 Rolling back uploaded files...");

            uploadedPublicIds.forEach(pid -> {
                try {
                    fileService.deleteFile(pid);
                    log.info("   ✔ Deleted uploaded file pid={}", pid);
                } catch (Exception ignore) {}
            });

            throw e;
        }

        // ------------------------
        // CREATE RESPONSE
        // ------------------------
        ToeicTestResponse response = toeicMapper.toTestResponse(test);
        response.setQuestionGroups(toeicMapper.toQuestionGroupResponses(questionGroups));

        return response;
    }

    @Override
    @Transactional
    public void deleteTest(String id) {
        List<ToeicTestQuestionGroup> questionGroups = toeicTestQuestionGroupRepository.findByTestId(id);
        try {
            // delete files
            for (var qg : questionGroups) {
                if (qg.getAudioUrl() != null) fileService.deleteFile(qg.getPublicImageId());
                if (qg.getImageUrl() != null) fileService.deleteFile(qg.getPublicAudioId());
            }
        }catch (Exception ex){
            log.error(ex.getMessage());
        }
        toeicTestQuestionGroupRepository.deleteByTestId(id);
        toeicTestRepository.deleteById(id);
    }

    @Override
    @Transactional
    public ToeicTestResponse updateTest(
            String id,
            ToeicTestRequest request,
            List<MultipartFile> imageFiles,
            List<MultipartFile> audioFiles) {

        log.info("🔄 [UPDATE TEST] id={}, testName={}", id, request.getName());

        ToeicTest test = toeicTestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Test not found"));

        test.setName(request.getName());
        toeicTestRepository.save(test);

        // 1. Lấy danh sách các Group hiện tại trong DB
        List<ToeicTestQuestionGroup> existingGroups = toeicTestQuestionGroupRepository.findByTestId(test.getId());
        Map<String, ToeicTestQuestionGroup> groupMap = new HashMap<>();
        existingGroups.forEach(g -> groupMap.put(g.getId(), g));

        Set<String> uploadedPublicIds = new HashSet<>();
        Set<String> pidsToDelete = new HashSet<>();
        Map<String, FileResponse> fileResponseMap = new HashMap<>();

        // 2. Upload file mới (nếu có)
        try {
            if (imageFiles != null) {
                for (MultipartFile img : imageFiles) {
                    FileResponse fr = fileService.uploadImage(img);
                    fileResponseMap.put(img.getOriginalFilename(), fr);
                    uploadedPublicIds.add(fr.getPublicId());
                }
            }
            if (audioFiles != null) {
                for (MultipartFile audio : audioFiles) {
                    FileResponse fr = fileService.uploadAudio(audio);
                    fileResponseMap.put(audio.getOriginalFilename(), fr);
                    uploadedPublicIds.add(fr.getPublicId());
                }
            }
        } catch (Exception e) {
            uploadedPublicIds.forEach(pid -> { try { fileService.deleteFile(pid); } catch (Exception ignore) {} });
            throw new RuntimeException("Upload failed during update", e);
        }

        try {
            List<ToeicTestQuestionGroup> groupsToSave = new ArrayList<>();

            for (ToeicTestQuestionGroupRequest qgReq : request.getQuestionGroups()) {
                ToeicTestQuestionGroup group;

                if (qgReq.getId() != null && groupMap.containsKey(qgReq.getId())) {
                    // --- TRƯỜNG HỢP UPDATE GROUP ---
                    group = groupMap.get(qgReq.getId());
                    group.setPart(qgReq.getPart());

                    // Xử lý Image Group
                    if (qgReq.getImageName() != null && !qgReq.getImageName().isEmpty()) {
                        FileResponse fr = fileResponseMap.get(qgReq.getImageName());
                        if (fr != null) {
                            if (group.getPublicImageId() != null) pidsToDelete.add(group.getPublicImageId());
                            group.setImageUrl(fr.getUrl());
                            group.setPublicImageId(fr.getPublicId());
                        }
                    }

                    // Xử lý Audio Group
                    if (qgReq.getAudioName() != null && !qgReq.getAudioName().isEmpty()) {
                        FileResponse fr = fileResponseMap.get(qgReq.getAudioName());
                        if (fr != null) {
                            if (group.getPublicAudioId() != null) pidsToDelete.add(group.getPublicAudioId());
                            group.setAudioUrl(fr.getUrl());
                            group.setPublicAudioId(fr.getPublicId());
                        }
                    }

                    // Xử lý danh sách câu hỏi trong group (Xóa cũ thêm mới để đơn giản hóa logic update questions)
                    // Hoặc bạn có thể dùng Mapper để update từng câu nếu muốn tối ưu hơn.
                    group.getQuestions().clear();
                    List<ToeicTestQuestion> newQuestions = toeicMapper.toTestQuestions(qgReq.getQuestions());
                    for (var q : newQuestions) {
                        q.setQuestionGroup(group);
                        group.getQuestions().add(q);
                    }

                } else {
                    // --- TRƯỜNG HỢP THÊM GROUP MỚI ---
                    group = toeicMapper.toQuestionGroup(qgReq);
                    group.setTest(test);

                    if (qgReq.getImageName() != null && fileResponseMap.containsKey(qgReq.getImageName())) {
                        FileResponse fr = fileResponseMap.get(qgReq.getImageName());
                        group.setImageUrl(fr.getUrl());
                        group.setPublicImageId(fr.getPublicId());
                    }

                    if (qgReq.getAudioName() != null && fileResponseMap.containsKey(qgReq.getAudioName())) {
                        FileResponse fr = fileResponseMap.get(qgReq.getAudioName());
                        group.setAudioUrl(fr.getUrl());
                        group.setPublicAudioId(fr.getPublicId());
                    }

                    // Gán group cho từng câu hỏi mới
                    if(group.getQuestions() != null) {
                        group.getQuestions().forEach(q -> q.setQuestionGroup(group));
                    }
                }
                groupsToSave.add(group);
            }

            // 3. Xử lý xóa các Group không còn trong request
            List<String> requestGroupIds = request.getQuestionGroups().stream()
                    .map(ToeicTestQuestionGroupRequest::getId)
                    .filter(Objects::nonNull)
                    .toList();

            for (ToeicTestQuestionGroup existing : existingGroups) {
                if (!requestGroupIds.contains(existing.getId())) {
                    if (existing.getPublicImageId() != null) pidsToDelete.add(existing.getPublicImageId());
                    if (existing.getPublicAudioId() != null) pidsToDelete.add(existing.getPublicAudioId());
                    toeicTestQuestionGroupRepository.delete(existing);
                }
            }

            // 4. Lưu tất cả thay đổi
            toeicTestQuestionGroupRepository.saveAll(groupsToSave);

            // 5. Dọn dẹp file cũ trên Cloudinary/S3
            pidsToDelete.forEach(pid -> { try { fileService.deleteFile(pid); } catch (Exception ignore) {} });

        } catch (Exception e) {
            // Rollback file mới nếu DB lỗi
            uploadedPublicIds.forEach(pid -> { try { fileService.deleteFile(pid); } catch (Exception ignore) {} });
            throw e;
        }

        // 6. Trả về response
        ToeicTestResponse response = toeicMapper.toTestResponse(test);
        List<ToeicTestQuestionGroup> finalGroups = toeicTestQuestionGroupRepository.findByTestId(test.getId());
        response.setQuestionGroups(toeicMapper.toQuestionGroupResponses(finalGroups));

        return response;
    }

}
