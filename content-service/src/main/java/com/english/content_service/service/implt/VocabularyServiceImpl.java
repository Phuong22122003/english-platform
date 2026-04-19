package com.english.content_service.service.implt;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.english.content_service.dto.request.VocabTopicRequest;
import com.english.content_service.dto.request.VocabularyRequest;
import com.english.content_service.dto.request.VocabularyTestQuestionRequest;
import com.english.content_service.dto.request.VocabularyTestRequest;
import com.english.content_service.entity.Options;
import com.english.content_service.service.TopicViewStatisticService;
import com.english.dto.response.*;
import com.english.enums.RequestType;
import com.english.enums.TopicType;
import com.english.exception.BadRequestException;
import com.english.exception.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.english.content_service.entity.Vocabulary;
import com.english.content_service.entity.VocabularyTest;
import com.english.content_service.entity.VocabularyTestQuestion;
import com.english.content_service.entity.VocabularyTopic;
import com.english.content_service.mapper.VocabularyMapper;
import com.english.content_service.repository.VocabularyRepository;
import com.english.content_service.repository.VocabularyTestQuestionRepository;
import com.english.content_service.repository.VocabularyTestRepository;
import com.english.content_service.repository.VocabularyTopicRepository;
import com.english.content_service.service.VocabularyService;
import com.english.service.FileService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class VocabularyServiceImpl implements VocabularyService {
    VocabularyTopicRepository vocabularyTopicRepository;
    VocabularyRepository vocabularyRepository;
    VocabularyTestRepository vocabularyTestRepository;
    VocabularyTestQuestionRepository vocabularyTestQuestionRepository;
    VocabularyMapper vocabularyMapper;
    FileService fileService;
//    AgentService agentService;
    TopicViewStatisticService topicViewStatisticService;
    RedisTemplate<String, Object> redisTemplate;

    @Override
    public Page<VocabTopicResponse> search(String query, int page, int limit) {
        Page<VocabularyTopic> topics;

        if (query == null || query.trim().isEmpty()) {
            topics = vocabularyTopicRepository.findAll(PageRequest.of(page, limit));
        } else {
            topics = vocabularyTopicRepository
                    .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                            query.trim(), query.trim(), PageRequest.of(page, limit)
                    );
        }

        List<VocabTopicResponse> topicResponses = vocabularyMapper.toVocabTopicResponses(topics.getContent());
        return new PageImpl<>(topicResponses, PageRequest.of(page, limit), topics.getTotalElements());
    }


    @Override
    public Page<VocabTopicResponse> getTopics(int page, int size) {
        String contentKey = "vocabulary:topics:page:content:page" + String.valueOf(page) + "size" + String.valueOf(size);
        String totalKey = "vocabulary:topics:page:total-element:page" + String.valueOf(page) + "size" + String.valueOf(size);
        List<VocabularyTopic> topicList =(List<VocabularyTopic>) redisTemplate.opsForValue().get(contentKey);
        Object totalFromRedis = redisTemplate.opsForValue().get(totalKey);
        Long totalElement = null;
        if (totalFromRedis instanceof Number) {
            totalElement = ((Number) totalFromRedis).longValue();
        }
        if(topicList == null || totalElement == null){
            Page<VocabularyTopic> topics = vocabularyTopicRepository.findAll(PageRequest.of(page, size));
            topicList = topics.getContent();
            totalElement = topics.getTotalElements();
            redisTemplate.opsForValue().set(contentKey,topicList, Duration.ofHours(24));
            redisTemplate.opsForValue().set(totalKey,totalElement, Duration.ofHours(24));
        }

        List<VocabTopicResponse> topicResponses = vocabularyMapper.toVocabTopicResponses(topicList);
        return new PageImpl<>(topicResponses, PageRequest.of(page, size), totalElement);
    }
    @Override
    @Transactional
    public VocabTopicResponse getVocabulariesByTopicId(String topicId) {
        String topicKey = "vocabulary:topics:info:"+topicId;
        String vocabulariesKey = "vocabulary:topics:items:"+topicId;

        VocabularyTopic topic = (VocabularyTopic) redisTemplate.opsForValue().get(topicKey);
        List<Vocabulary> vocabularies = (List<Vocabulary>) redisTemplate.opsForValue().get(vocabulariesKey);
        if(topic == null){
            topic = vocabularyTopicRepository.findById(topicId).orElseThrow(()-> new NotFoundException("Topic not found"));
            redisTemplate.opsForValue().set(topicKey,topic, Duration.ofHours(36));
        }
        if(vocabularies == null){
            vocabularies = vocabularyRepository.findByTopicId(topicId);
            redisTemplate.opsForValue().set(vocabulariesKey, vocabularies, Duration.ofHours(36));
        }

        VocabTopicResponse response = vocabularyMapper.toVocabTopicResponse(topic);
        response.setVocabularies(vocabularyMapper.toVocabularyResponses(vocabularies));

        topicViewStatisticService.addTopic(topicId, TopicType.VOCABULARY);
        return response;
    }

    @Override
    @Transactional
//  | word | phonetic | meaning | example | exampleMeaning | imageName | audioName |

    public List<VocabularyResponse> addVocabularies(String topicId, MultipartFile excelFile, List<MultipartFile> imageFiles, List<MultipartFile> audioFiles) {
        List<VocabularyRequest> requests = new ArrayList<>();
        try (InputStream is = excelFile.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) { // skip header row
                Row row = sheet.getRow(i);
                if (row == null) continue;
                VocabularyRequest request = new VocabularyRequest();

                request.setWord(getCellValueAsString(row.getCell(0)));
                request.setPhonetic(getCellValueAsString(row.getCell(1)));
                request.setMeaning(getCellValueAsString(row.getCell(2)));
                request.setExample(getCellValueAsString(row.getCell(3)));
                request.setExampleMeaning(getCellValueAsString(row.getCell(4)));
                request.setImageName(getCellValueAsString(row.getCell(5)));
                request.setAudioName(getCellValueAsString(row.getCell(6)));

                requests.add(request);
            }

        } catch (Exception e) {
            throw new BadRequestException("Error parsing Excel file: " + e.getMessage());
        }
        return this.addVocabularies(topicId,requests,imageFiles,audioFiles);
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
    public VocabTopicResponse getTestsByTopicId(String topicId, int page, int size) {
        VocabularyTopic topic = vocabularyTopicRepository.findById(topicId).orElseThrow(()-> new NotFoundException("Topic not found"));
        Page<VocabularyTest> tests = vocabularyTestRepository.findByTopicId(topicId, PageRequest.of(page, size));
        List<VocabularyTest> testList = tests.getContent();
        List<VocabularyTestResponse> testResponses = vocabularyMapper.toVocabularyTestResponses(testList);
        VocabTopicResponse response = vocabularyMapper.toVocabTopicResponse(topic);
        response.setTests(new PageImpl<>(testResponses, PageRequest.of(page, size), tests.getTotalElements()));
        return  response;
    }
    @Override
    public VocabularyTestResponse getTestQuestionsByTestId(String testId) {
        VocabularyTest test = vocabularyTestRepository.findById(testId).orElseThrow(()-> new NotFoundException("Test not found"));
        List<VocabularyTestQuestion> questions = vocabularyTestQuestionRepository.findByTestId(testId);
        VocabularyTestResponse response = vocabularyMapper.toVocabularyTestResponse(test);
        response.setQuestions(vocabularyMapper.toVocabularyTestQuestionResponses(questions));
        return  response;
    }
    @Override
    @Transactional
    public VocabTopicResponse addTopic(VocabTopicRequest request, MultipartFile imageFile) {

        Set<String> keys = redisTemplate.keys("vocabulary:topics:*");
        redisTemplate.delete(keys);

        VocabularyTopic topic = vocabularyMapper.toVocabTopic(request);
        FileResponse fileResponse=null;
        if (imageFile != null && !imageFile.isEmpty()) {
            fileResponse = fileService.uploadImage(imageFile);
            topic.setImageUrl(fileResponse.getUrl());
            topic.setPublicId(fileResponse.getPublicId());
        }
        topic.setCreatedAt(LocalDateTime.now());
        try{
            VocabularyTopic savedTopic = vocabularyTopicRepository.save(topic);
//            agentService.addTopicToVectorDB(savedTopic);
            return vocabularyMapper.toVocabTopicResponse(savedTopic);
        } catch (Exception e) {
            if(fileResponse!=null)
                fileService.deleteFile(fileResponse.getPublicId());
            throw new RuntimeException(e);
        }
    }
    @Override
    public VocabTopicResponse updateTopic(String topicId, VocabTopicRequest request, MultipartFile imageFile) {
        VocabularyTopic topic = this.vocabularyTopicRepository.findById(topicId).orElseThrow(()-> new NotFoundException("Topic not found"));
        vocabularyMapper.updateTopic(topic,request);

        // delete redis cache
        Set<String> keys = redisTemplate.keys("vocabulary:topics:*");
        redisTemplate.delete(keys);

        if(imageFile!=null&&!imageFile.isEmpty()){
            FileResponse fileResponse;
            if(topic.getPublicId()!=null){
                fileResponse = fileService.uploadImage(imageFile,topic.getPublicId());
            }else{
                fileResponse = fileService.uploadImage(imageFile);
            }
            topic.setPublicId(fileResponse.getPublicId());
            topic.setImageUrl(fileResponse.getUrl());
        }
        vocabularyTopicRepository.save(topic);
        return vocabularyMapper.toVocabTopicResponse(topic);
    }
    @Override
    @Transactional
    // admim
    public void deleteTopic(String topicId) {
        VocabularyTopic topic = this.vocabularyTopicRepository.findById(topicId).orElseThrow(()->{
            return new NotFoundException("Topic not found");
        });
        // delete redis cache
        Set<String> keys = redisTemplate.keys("vocabulary:topics:*");
        redisTemplate.delete(keys);

        List<String> publicIds = vocabularyTestQuestionRepository.findAllPublicIdsByTopicId(topicId);
        this.vocabularyTestQuestionRepository.deleteByTopicId(topicId);
        this.vocabularyTestRepository.deleteByTopicId(topicId);
        this.vocabularyTopicRepository.delete(topic);
//        this.agentService.deleteTopicFromVectorDB(topicId);
        try{
            this.fileService.deleteFile(topic.getPublicId());
            this.fileService.deleteFiles(publicIds);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public List<VocabTopicResponse> getTopicsByIds(List<String> ids) {
        return vocabularyMapper.toVocabTopicResponses(vocabularyTopicRepository.findAllById(ids));
    }

    @Override
    @Transactional
    public List<VocabularyResponse> addVocabularies(String topicId,
                                                    List<VocabularyRequest> requests,
                                                    List<MultipartFile> imageFiles,
                                                    List<MultipartFile> audioFiles) {

        VocabularyTopic topic = this.vocabularyTopicRepository.findById(topicId)
                .orElseThrow(() -> new NotFoundException("Topic not found"));

        // Delete old cache value
        redisTemplate.delete("vocabulary:topics:items:"+topicId);

        List<Vocabulary> vocabularies = vocabularyMapper.toVocabularies(requests);

        // Map filename -> MultipartFile for easy lookup by name from requests
        Map<String, MultipartFile> imageMap = new HashMap<>();
        if (imageFiles != null) {
            for (MultipartFile img : imageFiles) {
                if (img != null && !img.isEmpty() && img.getOriginalFilename() != null) {
                    imageMap.put(img.getOriginalFilename(), img);
                }
            }
        }

        Map<String, MultipartFile> audioMap = new HashMap<>();
        if (audioFiles != null) {
            for (MultipartFile aud : audioFiles) {
                if (aud != null && !aud.isEmpty() && aud.getOriginalFilename() != null) {
                    audioMap.put(aud.getOriginalFilename(), aud);
                }
            }
        }

        List<String> uploadedPublicIds = new ArrayList<>();
        try {
            for (int i = 0; i < vocabularies.size(); i++) {
                Vocabulary v = vocabularies.get(i);
                v.setId(null);
                v.setTopic(topic);
                v.setCreatedAt(LocalDateTime.now());

                // Get corresponding request to read imageName/audioName
                VocabularyRequest req = requests.get(i);

                // Upload image if imageName provided and file exists in imageMap
                if (req.getImageName() != null && !req.getImageName().isBlank()) {
                    MultipartFile imageFile = imageMap.get(req.getImageName());
                    if (imageFile != null && !imageFile.isEmpty()) {
                        FileResponse fileResp = fileService.uploadImage(imageFile);
                        v.setImageUrl(fileResp.getUrl());
                        v.setPublicImageId(fileResp.getPublicId());
                        uploadedPublicIds.add(fileResp.getPublicId());
                    }
                }

                // Upload audio if audioName provided and file exists in audioMap
                if (req.getAudioName() != null && !req.getAudioName().isBlank()) {
                    MultipartFile audioFile = audioMap.get(req.getAudioName());
                    if (audioFile != null && !audioFile.isEmpty()) {
                        FileResponse fileResp = fileService.uploadAudio(audioFile);
                        v.setAudioUrl(fileResp.getUrl());
                        v.setPublicAudioId(fileResp.getPublicId());
                        uploadedPublicIds.add(fileResp.getPublicId());
                    }
                }
            }

            // Save all vocabularies
            vocabularyRepository.saveAll(vocabularies);

        } catch (Exception e) {
            // Rollback any uploaded files
            for (String publicId : uploadedPublicIds) {
                try {
                    fileService.deleteFile(publicId);
                } catch (Exception ex) {
                    log.warn("Failed to delete uploaded file during rollback: {}", ex.getMessage());
                }
            }
            throw new RuntimeException(e);
        }

        return vocabularyMapper.toVocabularyResponses(vocabularies);
    }

    @Override
    @Transactional
    public List<VocabularyResponse> updateVocabularies(
            String topicId,
            List<VocabularyRequest> requests,
            List<MultipartFile> imageFiles,
            List<MultipartFile> audioFiles
    ) {
        // ✅ 1. Lấy topic
        VocabularyTopic topic = vocabularyTopicRepository.findById(topicId)
                .orElseThrow(() -> new NotFoundException("Topic not found"));

        // Delete old cache value
        redisTemplate.delete("vocabulary:topics:items:"+topicId);

        // ✅ 2. Ánh xạ file name -> file (image + audio)
        Map<String, MultipartFile> fileMap = new HashMap<>();
        if (imageFiles != null) {
            for (MultipartFile file : imageFiles) {
                if (file != null && !file.isEmpty()) {
                    fileMap.put(file.getOriginalFilename(), file);
                }
            }
        }
        if (audioFiles != null) {
            for (MultipartFile file : audioFiles) {
                if (file != null && !file.isEmpty()) {
                    fileMap.put(file.getOriginalFilename(), file);
                }
            }
        }

        // ✅ 3. Lấy các vocabulary cần update
        Map<String, Vocabulary> idToVocab = vocabularyRepository
                .findAllById(
                        requests.stream()
                                .map(VocabularyRequest::getId)
                                .filter(Objects::nonNull)
                                .toList()
                )
                .stream()
                .collect(Collectors.toMap(Vocabulary::getId, v -> v));

        // ✅ 4. Danh sách phục vụ xử lý
        List<String> deleteIds = new ArrayList<>();
        List<Vocabulary> toSave = new ArrayList<>();
        List<String> uploadedFileIds = new ArrayList<>();

        try {
            for (VocabularyRequest req : requests) {
                switch (req.getAction()) {
                    case DELETE -> {
                        String id = req.getId();
                        if (id == null) break;

                        deleteIds.add(id);

                        // ✅ Tìm vocab để xóa file (có thể không nằm trong idToVocab)
                        Vocabulary v = vocabularyRepository.findById(id).orElse(null);
                        if (v != null) {
                            try {
                                if (v.getPublicImageId() != null) {
                                    fileService.deleteFile(v.getPublicImageId());
                                }
                                if (v.getPublicAudioId() != null) {
                                    fileService.deleteFile(v.getPublicAudioId());
                                }
                            } catch (Exception ex) {
                                log.warn("⚠️ Failed to delete files for vocabulary {}: {}", id, ex.getMessage());
                            }
                        }
                    }

                    case UPDATE, ADD -> {
                        Vocabulary vocab;

                        // Nếu UPDATE thì lấy vocab hiện có, còn ADD thì tạo mới
                        if (req.getAction() == RequestType.UPDATE && idToVocab.containsKey(req.getId())) {
                            vocab = idToVocab.get(req.getId());
                            vocabularyMapper.patchUpdate(vocab, req);
                        } else {
                            vocab = vocabularyMapper.toVocabulary(req);
                            vocab.setId(null);
                            vocab.setTopic(topic);
                            vocab.setCreatedAt(LocalDateTime.now());
                        }

                        // ✅ Xử lý upload hình ảnh (nếu có)
                        if (req.getImageName() != null) {
                            MultipartFile image = fileMap.get(req.getImageName());
                            if (image != null && !image.isEmpty()) {
                                FileResponse fileRes = fileService.uploadImage(image, vocab.getPublicImageId());
                                vocab.setImageUrl(fileRes.getUrl());
                                vocab.setPublicImageId(fileRes.getPublicId());
                                uploadedFileIds.add(fileRes.getPublicId());
                            }
                        }

                        // ✅ Xử lý upload audio (nếu có)
                        if (req.getAudioName() != null) {
                            MultipartFile audio = fileMap.get(req.getAudioName());
                            if (audio != null && !audio.isEmpty()) {
                                FileResponse fileRes = fileService.uploadAudio(audio, vocab.getPublicAudioId());
                                vocab.setAudioUrl(fileRes.getUrl());
                                vocab.setPublicAudioId(fileRes.getPublicId());
                                uploadedFileIds.add(fileRes.getPublicId());
                            }
                        }

                        toSave.add(vocab);
                    }
                }
            }

            // ✅ 5. Xóa vocab được đánh dấu DELETE
            if (!deleteIds.isEmpty()) {
                vocabularyRepository.deleteAllById(deleteIds);
            }

            // ✅ 6. Lưu vocab ADD + UPDATE
            vocabularyRepository.saveAll(toSave);

        } catch (Exception e) {
            // ✅ Rollback file upload nếu lỗi
            for (String id : uploadedFileIds) {
                fileService.deleteFile(id);
            }
            throw e;
        }

        // ✅ 7. Trả về response
        return vocabularyMapper.toVocabularyResponses(toSave);
    }

    @Override
    @Transactional
    public VocabularyResponse updateVocabulary(String vocabId, VocabularyRequest request, MultipartFile imageFile,
                                               MultipartFile audioFile) {
        Vocabulary vocabulary = this.vocabularyRepository.findById(vocabId).orElseThrow(() -> new NotFoundException("Vocab not found"));

        // Delete old cache value
        redisTemplate.delete("vocabulary:topics:items:"+vocabulary.getTopic().getId());

        this.vocabularyMapper.patchUpdate(vocabulary, request);
        if (imageFile != null && !imageFile.isEmpty()) {
            FileResponse fileResponse = this.fileService.uploadImage(imageFile, vocabulary.getPublicImageId());
            vocabulary.setImageUrl(fileResponse.getUrl());
            vocabulary.setPublicImageId(fileResponse.getPublicId());
        }

        try{
            if (audioFile != null && !audioFile.isEmpty()) {
                FileResponse fileResponse = this.fileService.uploadAudio(audioFile, vocabulary.getPublicAudioId());
                vocabulary.setAudioUrl(fileResponse.getUrl());
                vocabulary.setPublicAudioId(fileResponse.getPublicId());
            }
        }catch (Exception e){
            if (imageFile != null && !imageFile.isEmpty()) {
                fileService.deleteFile(vocabulary.getPublicImageId());
            }
            throw new RuntimeException(e.getMessage());
        }

        vocabularyRepository.save(vocabulary);
        return  vocabularyMapper.toVocabularyResponse(vocabulary);

    }
    @Override
    @Transactional
    public void deleteVocabulary(String vocabId) {
        Vocabulary vocabulary = vocabularyRepository.findById(vocabId).orElseThrow(()->new NotFoundException("Vocabulary not found"));
        this.vocabularyRepository.delete(vocabulary);
        if (vocabulary.getPublicAudioId() != null) {
            fileService.deleteFile(vocabulary.getPublicAudioId());
        }
        if(vocabulary.getPublicImageId()!=null){
            fileService.deleteFile(vocabulary.getPublicImageId());
        }
    }

    @Override
    @Transactional
    public VocabularyTestResponse addTest(String topicId, VocabularyTestRequest vocabularyTestRequest, List<MultipartFile> imageFiles) {
        VocabularyTopic topic = vocabularyTopicRepository.findById(topicId).orElseThrow(()-> new RuntimeException("Topic not found"));
        VocabularyTest test =  VocabularyTest
                .builder()
                .topic(topic)
                .name(vocabularyTestRequest.getName())
                .duration(vocabularyTestRequest.getDuration())
                .createdAt(LocalDateTime.now())
                .build();
        test.setId(null);
        test = vocabularyTestRepository.save(test);
        Map<String, MultipartFile> imageMap = new HashMap<>();
        if (imageFiles != null) {
            for (MultipartFile img : imageFiles) {
                imageMap.put(img.getOriginalFilename(), img);
            }
        }
        List<VocabularyTestQuestion> questions = vocabularyMapper.toVocabularyTestQuestions(vocabularyTestRequest.getQuestions());
        List<String> publicIds = new ArrayList<>();
        VocabularyTestResponse vocabularyTestResponse;
        try{
            for(int i = 0; i< questions.size();i++){
                VocabularyTestQuestion q = questions.get(i);
                q.setId(null);
                q.setTest(test);
                String imageName = vocabularyTestRequest.getQuestions().get(i).getImageName();
                if(imageName==null) continue;;
                MultipartFile imageFile = imageMap.get(imageName);
                if(imageFile!=null && !imageFile.isEmpty()){
                    FileResponse fileResponse = fileService.uploadImage(imageFile);
                    q.setImageUrl(fileResponse.getUrl());
                    q.setPublicId(fileResponse.getPublicId());
                    publicIds.add(q.getPublicId());
                }
            }
            questions = vocabularyTestQuestionRepository.saveAll(questions);
        } catch (Exception e) {
            for(String publicId: publicIds){
                fileService.deleteFile(publicId);
            }
            throw new RuntimeException(e);
        }
        vocabularyTestResponse = vocabularyMapper.toVocabularyTestResponse(test);
        vocabularyTestResponse.setQuestions(vocabularyMapper.toVocabularyTestQuestionResponses(questions));
        return  vocabularyTestResponse;
    }

    @Override
    @Transactional
    /**
     * Excel format:
     * | name | test1 |
     * | duration | 10 |
     * | question | optionA | optionB | optionC | optionD | correctAnswer | explanation | imageName |
     */
    public VocabularyTestResponse addTest(String topicId, MultipartFile excelFile, List<MultipartFile> imageFiles) {

        try (InputStream is = excelFile.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            VocabularyTestRequest request = new VocabularyTestRequest();
            request.setName(getCellValueAsString(sheet.getRow(0).getCell(1)));
            request.setDuration(Integer.parseInt(getCellValueAsString(sheet.getRow(1).getCell(1))));

            List<VocabularyTestQuestionRequest> questionRequests = new ArrayList<>();

            for (int i = 3; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                VocabularyTestQuestionRequest q = new VocabularyTestQuestionRequest();
                Options options = new Options();
                q.setQuestion(getCellValueAsString(row.getCell(0)));
                options.setA(getCellValueAsString(row.getCell(1)));
                options.setB(getCellValueAsString(row.getCell(2)));
                options.setC(getCellValueAsString(row.getCell(3)));
                options.setD(getCellValueAsString(row.getCell(4)));
                q.setOptions(options);
                q.setCorrectAnswer(getCellValueAsString(row.getCell(5)));
                q.setExplanation(getCellValueAsString(row.getCell(6)));
                q.setImageName(getCellValueAsString(row.getCell(7)));

                questionRequests.add(q);
            }

            request.setQuestions(questionRequests);
            return addTest(topicId, request, imageFiles);

        } catch (Exception e) {
            throw new BadRequestException("Error reading Excel");
        }
    }

    @Override
    public VocabularyTestResponse updateTest(String testId, VocabularyTestRequest vocabularyTestRequest, List<MultipartFile> imageFiles) {
        // Lấy bài test hiện tại
        VocabularyTest test = vocabularyTestRepository.findById(testId)
                .orElseThrow(() -> new NotFoundException("Test not found"));

        // Cập nhật thông tin cơ bản của bài test
        test.setName(vocabularyTestRequest.getName());
        test.setDuration(vocabularyTestRequest.getDuration());

        vocabularyTestRepository.save(test);

        // Lấy danh sách câu hỏi hiện tại
        List<VocabularyTestQuestion> existingQuestions = vocabularyTestQuestionRepository.findByTestId(test.getId());
        Map<String, VocabularyTestQuestion> idToQuestion = new HashMap<>();
        for (VocabularyTestQuestion q : existingQuestions) {
            idToQuestion.put(q.getId(), q);
        }

        // Ánh xạ file ảnh theo tên file
        Map<String, MultipartFile> nameToFile = new HashMap<>();
        if (imageFiles != null) {
            for (MultipartFile f : imageFiles) {
                if (f != null && !f.isEmpty()) {
                    nameToFile.put(f.getOriginalFilename(), f);
                }
            }
        }

        List<VocabularyTestQuestion> newQuestions = new ArrayList<>();
        List<String> deleteIds = new ArrayList<>();
        List<String> uploadedPublicIds = new ArrayList<>();

        try {
            for (VocabularyTestQuestionRequest req : vocabularyTestRequest.getQuestions()) {
                switch (req.getAction()) {
                    case DELETE -> {
                        // Xóa câu hỏi
                        VocabularyTestQuestion q = idToQuestion.get(req.getId());
                        if (q != null && q.getPublicId() != null) {
                            fileService.deleteFile(q.getPublicId());
                        }
                        deleteIds.add(req.getId());
                    }
                    case UPDATE -> {
                        VocabularyTestQuestion existing = idToQuestion.get(req.getId());
                        if (existing == null) continue;

                        // Cập nhật nội dung cơ bản
                        vocabularyMapper.updateVocabularyTestQuestion(existing, req);

                        // Cập nhật ảnh nếu có
                        if (req.getImageName() != null) {
                            MultipartFile file = nameToFile.get(req.getImageName());
                            if (file != null) {
                                FileResponse fr;
                                fr = fileService.uploadImage(file, existing.getPublicId());
                                existing.setImageUrl(fr.getUrl());
                                existing.setPublicId(fr.getPublicId());
                                uploadedPublicIds.add(fr.getPublicId());
                            }
                        }

                        newQuestions.add(existing);
                    }
                    case ADD -> {
                        VocabularyTestQuestion newQ = vocabularyMapper.toVocabularyTestQuestion(req);
                        newQ.setId(null);
                        newQ.setTest(test);

                        if (req.getImageName() != null) {
                            MultipartFile file = nameToFile.get(req.getImageName());
                            if (file != null) {
                                FileResponse fr = fileService.uploadImage(file);
                                newQ.setImageUrl(fr.getUrl());
                                newQ.setPublicId(fr.getPublicId());
                                uploadedPublicIds.add(fr.getPublicId());
                            }
                        }

                        newQuestions.add(newQ);
                    }
                }
            }

            // Xóa câu hỏi bị đánh dấu DELETE
            if (!deleteIds.isEmpty()) {
                vocabularyTestQuestionRepository.deleteAllById(deleteIds);
            }

            // Lưu thay đổi cho ADD và UPDATE
            vocabularyTestQuestionRepository.saveAll(newQuestions);

        } catch (Exception e) {
            // rollback upload nếu lỗi
            for (String pid : uploadedPublicIds) {
                fileService.deleteFile(pid);
            }
            throw e;
        }

        // Trả về response
        VocabularyTestResponse response = vocabularyMapper.toVocabularyTestResponse(test);
        response.setQuestions(vocabularyMapper.toVocabularyTestQuestionResponses(newQuestions));
        return response;
    }

    @Override
    public List<VocabularyTestResponse> getTestsByIds(List<String> ids) {
        List<VocabularyTest> tests = vocabularyTestRepository.findAllById(ids);
        return vocabularyMapper.toVocabularyTestResponses(tests);
    }

    @Override
    @Transactional
    public void deleteTest(String testId) {
        List<VocabularyTestQuestion> questions = vocabularyTestQuestionRepository.findByTestId(testId);
        vocabularyTestQuestionRepository.deleteByTestId(testId);
        vocabularyTestRepository.deleteById(testId);
        for(var q: questions){
            if(q.getPublicId()!=null)
                fileService.deleteFile(q.getPublicId());
        }
    }
}
