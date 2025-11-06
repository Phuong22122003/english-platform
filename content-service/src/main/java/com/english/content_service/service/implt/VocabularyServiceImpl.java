package com.english.content_service.service.implt;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.english.content_service.dto.request.VocabTopicRequest;
import com.english.content_service.dto.request.VocabularyRequest;
import com.english.content_service.dto.request.VocabularyTestQuestionRequest;
import com.english.content_service.dto.request.VocabularyTestRequest;
import com.english.content_service.httpclient.AgentClient;
import com.english.content_service.service.AgentService;
import com.english.content_service.service.TopicViewStatisticService;
import com.english.dto.response.*;
import com.english.enums.RequestType;
import com.english.enums.TopicType;
import com.english.exception.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
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
    AgentService agentService;
    TopicViewStatisticService topicViewStatisticService;

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
        Page<VocabularyTopic> topics = vocabularyTopicRepository.findAll(PageRequest.of(page, size));
        List<VocabularyTopic> topicList = topics.getContent();
        List<VocabTopicResponse> topicResponses = vocabularyMapper.toVocabTopicResponses(topicList);
        return new PageImpl<>(topicResponses, PageRequest.of(page, size), topics.getTotalElements());
    }
    @Override
    @Transactional
    public GetVocabularyTopicResponse getVocabulariesByTopicId(String topicId) {
        List<Vocabulary> vocabularies = vocabularyRepository.findByTopicId(topicId);
        Optional<VocabularyTopic> topicOpt = vocabularyTopicRepository.findById(topicId);
        topicViewStatisticService.addTopic(topicId, TopicType.VOCABULARY);
        return topicOpt.map(vocabularyTopic -> GetVocabularyTopicResponse.builder()
                .name(vocabularyTopic.getName())
                .topicId(vocabularyTopic.getId())
                .vocabularies(vocabularyMapper.toVocabularyResponses(vocabularies))
                .build()).orElse(null);
    }
    @Override
    public GetTestsVocabByTopicIdResponse getTestsByTopicId(String topicId, int page, int size) {
        Page<VocabularyTest> tests = vocabularyTestRepository.findByTopicId(topicId, PageRequest.of(page, size));
        List<VocabularyTest> testList = tests.getContent();
        List<VocabularyTestResponse> testResponses = vocabularyMapper.toVocabularyTestResponses(testList);
        Optional<VocabularyTopic> topicOtp = vocabularyTopicRepository.findById(topicId);
        return topicOtp.map(topic -> GetTestsVocabByTopicIdResponse.builder()
                .vocabularyTests(new PageImpl<>(testResponses, PageRequest.of(page, size), tests.getTotalElements()))
                .topicName(topic.getName())
                .topicId(topicId)
                .build()).orElse(null);
    }
    @Override
    public GetVocabularyTestQuestionResponse getTestQuestionsByTestId(String testId) {
        List<VocabularyTestQuestion> questions = vocabularyTestQuestionRepository.findByTestId(testId);
        Optional<VocabularyTest> vocabularyTestOtp = vocabularyTestRepository.findById(testId);
        return  vocabularyTestOtp.map(vocabularyTest -> GetVocabularyTestQuestionResponse.builder()
                .topicName(vocabularyTest.getTopic().getName())
                .topicId(vocabularyTest.getTopic().getId())
                .duration(vocabularyTest.getDuration())
                .testId(vocabularyTest.getId())
                .testName(vocabularyTest.getName())
                .questions(vocabularyMapper.toVocabularyTestQuestionResponses(questions))
                .build()).orElse(null);
    }
    @Override
    @Transactional
    public VocabTopicResponse addTopic(VocabTopicRequest request, MultipartFile imageFile) {
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
            agentService.addTopicToVectorDB(savedTopic);
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
    // only delete new topic
    public void deleteTopic(String topicId) {
        VocabularyTopic topic = this.vocabularyTopicRepository.findById(topicId).orElseThrow(()->{
            return new NotFoundException("Topic not found");
        });
        this.vocabularyTestQuestionRepository.deleteByTopicId(topicId);
        this.vocabularyTestRepository.deleteByTopicId(topicId);
        this.vocabularyTopicRepository.delete(topic);
        this.fileService.deleteFile(topic.getPublicId());
        List<String> publicIds = vocabularyTestQuestionRepository.findAllPublicIdsByTopicId(topicId);
        fileService.deleteFiles(publicIds);
    }

    @Override
    public List<VocabTopicResponse> getTopicsByIds(List<String> ids) {
        return vocabularyMapper.toVocabTopicResponses(vocabularyTopicRepository.findAllById(ids));
    }

    @Override
    @Transactional
    public List<VocabularyResponse> addVocabularies(String topicId, List<VocabularyRequest> requests,
            List<MultipartFile> imageFiles, List<MultipartFile> audioFiles) {
        VocabularyTopic topic = this.vocabularyTopicRepository.findById(topicId).orElseThrow(()-> new RuntimeException("Topic not found"));
        List<Vocabulary> vocabularies = vocabularyMapper.toVocabularies(requests);
        List<String> publicIds = new ArrayList<>();
        try{
            for(int i = 0; i< vocabularies.size();i++){
                Vocabulary v = vocabularies.get(i);
                v.setId(null);
                v.setTopic(topic);
                v.setCreatedAt(LocalDateTime.now());
                FileResponse fileResponse = fileService.uploadImage(imageFiles.get(i));
                v.setImageUrl(fileResponse.getUrl());
                v.setPublicImageId(fileResponse.getPublicId());
                fileResponse = fileService.uploadAudio(audioFiles.get(i));
                v.setAudioUrl(fileResponse.getUrl());
                v.setPublicAudioId(fileResponse.getPublicId());
                publicIds.add(v.getPublicAudioId());
                publicIds.add(v.getAudioUrl());
            }
            vocabularyRepository.saveAll(vocabularies);
        } catch (Exception e) {
            for(String publicId: publicIds){
                fileService.deleteFile(publicId);
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
        List<VocabularyTestQuestion> questions = vocabularyMapper.toVocabularyTestQuestions(vocabularyTestRequest.getQuestions());
        List<String> publicIds = new ArrayList<>();
        VocabularyTestResponse vocabularyTestResponse;
        try{
            for(int i = 0; i< questions.size();i++){
                VocabularyTestQuestion q = questions.get(i);
                q.setId(null);
                q.setTest(test);
                if(imageFiles!=null && imageFiles.size()>i && imageFiles.get(i)!=null && !imageFiles.get(i).isEmpty()){
                    FileResponse fileResponse = fileService.uploadImage(imageFiles.get(i));
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
