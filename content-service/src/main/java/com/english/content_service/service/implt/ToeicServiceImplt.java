package com.english.content_service.service.implt;

import com.english.content_service.dto.request.ToeicTestGroupRequest;
import com.english.content_service.dto.request.ToeicTestQuestionRequest;
import com.english.content_service.dto.request.ToeicTestRequest;
import com.english.content_service.entity.Options;
import com.english.content_service.entity.ToeicTest;
import com.english.content_service.entity.ToeicTestGroup;
import com.english.content_service.entity.ToeicTestQuestion;
import com.english.content_service.mapper.ToeicMapper;
import com.english.content_service.repository.ToeicTestGroupRepository;
import com.english.content_service.repository.ToeicTestQuestionRepository;
import com.english.content_service.repository.ToeicTestRepository;
import com.english.content_service.service.ToeicService;
import com.english.dto.response.FileResponse;
import com.english.dto.response.ToeicTestGroupResponse;
import com.english.dto.response.ToeicTestResponse;
import com.english.exception.AppException;
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
    public List<ToeicTestResponse> getTestByIds(List<String> ids) {
        List<ToeicTest> tests = toeicTestRepository.findAllById(ids);
        return toeicMapper.toTestResponses(tests);
    }

    @Override
    public ToeicTestResponse getTestById(String id) {
        ToeicTest test = toeicTestRepository.findById(id)
                .orElseThrow(()->new NotFoundException("Test not found"));
        List<ToeicTestQuestion> questions = toeicTestQuestionRepository.findByTestId(test.getId());
        ToeicTestResponse response = toeicMapper.toTestResponse(test);
        response.setQuestions(toeicMapper.toQuestionResponses(questions));
        return response;
    }


    // ========================================================================
    // TEST
    // ========================================================================
//    Toeic Test
//    NAME	ETS 2024
//    PART 1
//    QUESTION	A	B	C	D	CORRECT ANSWER	EXPLANATION	IMAGE NAME	AUDIO NAME
//    PART 2
//    QUESTION	A	B	C	D	CORRECT ANSWER	EXPLANATION	IMAGE NAME	AUDIO NAME
//    PART 3
//    QUESTION	A	B	C	D	CORRECT ANSWER	EXPLANATION	IMAGE NAME	AUDIO NAME
//    PART 4
//    QUESTION	A	B	C	D	CORRECT ANSWER	EXPLANATION	IMAGE NAME	AUDIO NAME
//    PART 5
//    QUESTION	A	B	C	D	CORRECT ANSWER	EXPLANATION	IMAGE NAME	AUDIO NAME
//    PART 6
//    QUESTION	A	B	C	D	CORRECT ANSWER	EXPLANATION	IMAGE NAME	AUDIO NAME
//    PART 7
//    QUESTION	A	B	C	D	CORRECT ANSWER	EXPLANATION	IMAGE NAME	AUDIO NAME

    @Override
    public ToeicTestResponse addTest(
            String groupId,
            MultipartFile excelFile,
            List<MultipartFile> imageFiles,
            List<MultipartFile> audioFiles) {
        int rowAt = 0, columnAt = 0;
        try(InputStream is = excelFile.getInputStream();
            Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            ToeicTestRequest request = new ToeicTestRequest();
            request.setName(getCellValueAsString(sheet.getRow(1).getCell(1)));
            request.setQuestions(new ArrayList<>());
            for (int i = 2; i < sheet.getPhysicalNumberOfRows(); i++){
                rowAt = i;
                if(i == 2|| i == 9||i==35||i==75||i==106||i==137||i==154) continue;
                Row row = sheet.getRow(i);
                ToeicTestQuestionRequest question = new ToeicTestQuestionRequest();
                columnAt=0;
                question.setQuestion(getCellValueAsString(row.getCell(0)));
                Options options = new Options();
                columnAt=1;
                options.setA(getCellValueAsString(row.getCell(1)));
                columnAt=2;
                options.setB(getCellValueAsString(row.getCell(2)));
                columnAt=3;
                options.setC(getCellValueAsString(row.getCell(3)));
                columnAt=4;
                options.setD(getCellValueAsString(row.getCell(4)));
                question.setOptions(options);
                columnAt=5;
                question.setCorrectAnswer(getCellValueAsString(row.getCell(5)));
                columnAt=6;
                question.setExplanation(getCellValueAsString(row.getCell(6)));
                columnAt=7;
                question.setImageName(getCellValueAsString(row.getCell(7)));
                columnAt=8;
                question.setAudioName(getCellValueAsString(row.getCell(8)));
                columnAt=9;
                if(i<9) question.setPart(1);
                else if (i<35) {
                    question.setPart(2);
                }
                else if (i<75){
                    question.setPart(3);
                } else if (i<106) {
                    question.setPart(4);
                } else if (i<137) {
                    question.setPart(5);
                } else if (i<154) {
                    question.setPart(6);
                }
                else{
                    question.setPart(7);
                }
                request.getQuestions().add(question);
            }
            return addTest(groupId,request,imageFiles,audioFiles);
        } catch (Exception e) {
            throw new RuntimeException("{row:"+rowAt+",column:"+columnAt+"}");
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

        ToeicTestGroup group = toeicTestGroupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found"));

        ToeicTest test = ToeicTest.builder()
                .name(request.getName())
                .createdAt(LocalDateTime.now())
                .totalCompletion(0)
                .group(group)
                .build();

        test = toeicTestRepository.save(test);

        List<ToeicTestQuestion> questions = toeicMapper.toTestQuestions(request.getQuestions());

        List<String> uploadedPublicIds = new ArrayList<>();

        Map<String, MultipartFile> imageMap = new HashMap<>();
        if (imageFiles != null) {
            for (MultipartFile img : imageFiles) {
                imageMap.put(img.getOriginalFilename(), img);
            }
        }
        Map<String, MultipartFile> audioMap = new HashMap<>();
        if (audioFiles != null) {
            for (MultipartFile audio : audioFiles) {
                audioMap.put(audio.getOriginalFilename(), audio);
            }
        }


        try {
            for (int i = 0; i < questions.size(); i++) {
                ToeicTestQuestion q = questions.get(i);
                q.setTest(test);
                ToeicTestQuestionRequest qr = request.getQuestions().get(i);
                // image upload
                if (qr.getImageName()!=null && !qr.getImageName().isEmpty()) {
                    FileResponse img = fileService.uploadImage(imageMap.get(qr.getImageName()));
                    q.setImageUrl(img.getUrl());
                    uploadedPublicIds.add(img.getPublicId());
                }

                // audio upload
                if (qr.getAudioName()!=null && !qr.getAudioName().isEmpty()) {
                    FileResponse audio = fileService.uploadAudio(audioMap.get(qr.getAudioName()));
                    q.setAudioUrl(audio.getUrl());
                    uploadedPublicIds.add(audio.getPublicId());
                }
            }

            toeicTestQuestionRepository.saveAll(questions);

        } catch (Exception e) {
            uploadedPublicIds.forEach(pid -> {
                try { fileService.deleteFile(pid); } catch (Exception ignore) {}
            });
            throw e;
        }

        ToeicTestResponse response = toeicMapper.toTestResponse(test);
        response.setQuestions(toeicMapper.toQuestionResponses(questions));
        return response;
    }

    @Override
    @Transactional
    public void deleteTest(String id) {
        List<ToeicTestQuestion> questions = toeicTestQuestionRepository.findByTestId(id);
        try {
            // delete files
            for (var q : questions) {
                if (q.getAudioUrl() != null) fileService.deleteFile(q.getPublicImageId());
                if (q.getImageUrl() != null) fileService.deleteFile(q.getPublicAudioId());
            }
        }catch (Exception ex){
            log.error(ex.getMessage());
        }

        toeicTestQuestionRepository.deleteByTestId(id);
        toeicTestRepository.deleteById(id);
    }

    @Override
    @Transactional
    public ToeicTestResponse updateTest(
            String id,
            ToeicTestRequest request,
            List<MultipartFile> imageFiles,
            List<MultipartFile> audioFiles) {

        ToeicTest test = toeicTestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Test not found"));

        test.setName(request.getName());
        toeicTestRepository.save(test);

        // map existing questions
        List<ToeicTestQuestion> existingQuestions = toeicTestQuestionRepository.findByTestId(test.getId());
        Map<String, ToeicTestQuestion> idMap = new HashMap<>();
        existingQuestions.forEach(q -> idMap.put(q.getId(), q));

        Map<String, MultipartFile> imageMap = new HashMap<>();
        if (imageFiles != null) {
            for (MultipartFile img : imageFiles) {
                imageMap.put(img.getOriginalFilename(), img);
            }
        }

        Map<String, MultipartFile> audioMap = new HashMap<>();
        if (audioFiles != null) {
            for (MultipartFile audio : audioFiles) {
                audioMap.put(audio.getOriginalFilename(), audio);
            }
        }

        List<ToeicTestQuestion> toSave = new ArrayList<>();
        List<String> toDeleteIds = new ArrayList<>();
        List<String> uploadedPublicIds = new ArrayList<>();

        try {
            for (ToeicTestQuestionRequest qReq : request.getQuestions()) {

                switch (qReq.getAction()) {

                    // ============================
                    // DELETE
                    // ============================
                    case DELETE -> {
                        if (idMap.containsKey(qReq.getId())) {
                            toDeleteIds.add(qReq.getId());
                            var deletedQuestion = idMap.get(qReq.getId());
                            if(deletedQuestion==null) break;
                            if(deletedQuestion.getPublicAudioId()!=null) fileService.deleteFile(deletedQuestion.getPublicAudioId());
                            if(deletedQuestion.getPublicImageId()!=null) fileService.deleteFile(deletedQuestion.getPublicImageId());
                        }
                    }

                    // ============================
                    // UPDATE
                    // ============================
                    case UPDATE -> {
                        ToeicTestQuestion q = idMap.get(qReq.getId());
                        if (q == null) continue;

                        // copy text + option fields
                        toeicMapper.updateTestQuestion(q, qReq);

                        // IMAGE
                        if (qReq.getImageName() != null && !qReq.getImageName().isEmpty()) {
                            MultipartFile imgFile = imageMap.get(qReq.getImageName());
                            if (imgFile != null && !imgFile.isEmpty()) {
                                FileResponse fr = fileService.uploadImage(imgFile);
                                q.setImageUrl(fr.getUrl());
                                uploadedPublicIds.add(fr.getPublicId());
                            }
                        }

                        // AUDIO
                        if (qReq.getAudioName() != null && !qReq.getAudioName().isEmpty()) {
                            MultipartFile audioFile = audioMap.get(qReq.getAudioName());
                            if (audioFile != null && !audioFile.isEmpty()) {
                                FileResponse fr = fileService.uploadAudio(audioFile);
                                q.setAudioUrl(fr.getUrl());
                                uploadedPublicIds.add(fr.getPublicId());
                            }
                        }

                        toSave.add(q);
                    }

                    // ============================
                    // ADD
                    // ============================
                    case ADD -> {
                        ToeicTestQuestion newQ = toeicMapper.toTestQuestion(qReq);
                        newQ.setTest(test);

                        // IMAGE
                        if (qReq.getImageName() != null && !qReq.getImageName().isEmpty()) {
                            MultipartFile imgFile = imageMap.get(qReq.getImageName());
                            if (imgFile != null && !imgFile.isEmpty()) {
                                FileResponse fr = fileService.uploadImage(imgFile);
                                newQ.setImageUrl(fr.getUrl());
                                uploadedPublicIds.add(fr.getPublicId());
                            }
                        }

                        // AUDIO
                        if (qReq.getAudioName() != null && !qReq.getAudioName().isEmpty()) {
                            MultipartFile audioFile = audioMap.get(qReq.getAudioName());
                            if (audioFile != null && !audioFile.isEmpty()) {
                                FileResponse fr = fileService.uploadAudio(audioFile);
                                newQ.setAudioUrl(fr.getUrl());
                                uploadedPublicIds.add(fr.getPublicId());
                            }
                        }

                        toSave.add(newQ);
                    }
                }
            }

            if (!toDeleteIds.isEmpty()) {
                toeicTestQuestionRepository.deleteAllById(toDeleteIds);
            }

            toeicTestQuestionRepository.saveAll(toSave);

        } catch (Exception e) {
            // rollback uploaded files
            for (String pid : uploadedPublicIds) {
                try { fileService.deleteFile(pid); } catch (Exception ignored) {}
            }
            throw e;
        }

        ToeicTestResponse res = toeicMapper.toTestResponse(test);
        res.setQuestions(toeicMapper.toQuestionResponses(toSave));

        return res;
    }

}
