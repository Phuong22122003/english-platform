package com.english.content_service.service.implt;

import com.english.content_service.dto.request.ListeningRequest;
import com.english.content_service.dto.request.ListeningTestRequest;
import com.english.content_service.dto.request.ListeningTopicRequest;
import com.english.content_service.entity.ListeningTestQuestion;
import com.english.content_service.service.AgentService;
import com.english.content_service.service.TopicViewStatisticService;
import com.english.dto.response.FileResponse;
import com.english.dto.response.ListeningResponse;
import com.english.dto.response.ListeningTestReponse;
import com.english.dto.response.ListeningTopicResponse;
import com.english.content_service.entity.Listening;
import com.english.content_service.entity.ListeningTest;
import com.english.content_service.entity.ListeningTopic;
import com.english.content_service.mapper.ListeningMapper;
import com.english.content_service.repository.ListeningRepository;
import com.english.content_service.repository.ListeningTestQuestionRepository;
import com.english.content_service.repository.ListeningTestRepository;
import com.english.content_service.repository.ListeningTopicRepository;
import com.english.enums.RequestType;
import com.english.enums.TopicType;
import com.english.exception.NotFoundException;
import com.english.service.FileService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.listener.Topic;
import org.springframework.stereotype.Service;
import com.english.content_service.service.ListeningService;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ListeningServiceImpl implements ListeningService {
    ListeningMapper listeningMapper;
    FileService fileService;
    ListeningTopicRepository listeningTopicRepository;
    ListeningRepository listeningRepository;
    ListeningTestRepository listeningTestRepository;
    ListeningTestQuestionRepository listeningTestQuestionRepository;
    AgentService agentService;
    TopicViewStatisticService topicViewStatisticService;

    @Override
    public Page<ListeningTopicResponse> search(String query, int page, int limit) {
        Page<ListeningTopic> topics;

        if (query == null || query.trim().isEmpty()) {
            topics = listeningTopicRepository.findAll(PageRequest.of(page, limit));
        } else {
            topics = listeningTopicRepository
                    .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                            query.trim(), query.trim(), PageRequest.of(page, limit)
                    );
        }

        List<ListeningTopicResponse> responses = listeningMapper.toTopicResponses(topics.getContent());
        return new PageImpl<>(responses, PageRequest.of(page, limit), topics.getTotalElements());
    }


    @Override
    public Page<ListeningTopicResponse> getTopics(int page, int size) {
        Page<ListeningTopic> topics = listeningTopicRepository.findAll(PageRequest.of(page, size));
        List<ListeningTopic> topicList = topics.getContent();
        List<ListeningTopicResponse> topicResponses = listeningMapper.toTopicResponses(topicList);
        return new PageImpl<>(topicResponses, PageRequest.of(page, size), topics.getTotalElements());
    }

    @Override
    @Transactional
    public ListeningTopicResponse addTopic(ListeningTopicRequest request, MultipartFile imageFile) {
        ListeningTopic topic = listeningMapper.toTopicEntity(request);
        if(imageFile!=null&&!imageFile.isEmpty()){
            var fileResponse = fileService.uploadImage(imageFile);
            topic.setImageUrl(fileResponse.getUrl());
            topic.setPublicId(fileResponse.getPublicId());
        }
        topic.setCreatedAt(LocalDateTime.now());

        try {
            topic = listeningTopicRepository.save(topic);
            agentService.addTopicToVectorDB(topic);
        } catch (Exception e) {
            if(topic.getPublicId()!=null) fileService.deleteFile(topic.getPublicId());
            throw new RuntimeException(e);
        }

        return listeningMapper.toTopicResponse(topic);
    }

    @Override
    public List<ListeningTopicResponse> getTopicsByIds(List<String> ids) {
        List<ListeningTopic> topics = listeningTopicRepository.findAllById(ids);
        return listeningMapper.toTopicResponses(topics);
    }

    @Override
    public ListeningTopicResponse updateTopic(String topicId, ListeningTopicRequest request, MultipartFile imageFile){
        ListeningTopic topic = listeningTopicRepository.findById(topicId).orElseThrow(()->new NotFoundException("Topic not found"));
        listeningMapper.updateTopic(topic,request);
        if(imageFile!=null&&!imageFile.isEmpty()){
            FileResponse fileResponse = fileService.uploadImage(imageFile,topic.getPublicId());
            topic.setImageUrl(fileResponse.getUrl());
            topic.setPublicId(fileResponse.getPublicId());
        }
        listeningTopicRepository.save(topic);
        return listeningMapper.toTopicResponse(topic);
    }

    @Override
    @Transactional
    public void deleteTopic(String topicId) {
        List<ListeningTest> tests = listeningTestRepository.findAllByTopicId(topicId);
        for(var t: tests){
            deleteTest(t.getId());
        }
        ListeningTopic topic = listeningTopicRepository.findById(topicId).orElseThrow(()-> new NotFoundException("Topic not found"));
        listeningTopicRepository.delete(topic);
        if(topic.getPublicId()!=null) fileService.deleteFile(topic.getPublicId());
    }

    @Override
    @Transactional
    public ListeningTopicResponse getListeningByTopic(String topicId) {
        var topic = listeningTopicRepository.findById(topicId).orElseThrow(()->new RuntimeException("Topic Not found"));
        List<Listening> listeningList = listeningRepository.findByTopicId(topicId);
        var listeningListResponses = listeningMapper.toListeningResponse(listeningList);
        var topicReponse = listeningMapper.toTopicResponse(topic);
        topicReponse.setListenings(listeningListResponses);
        topicViewStatisticService.addTopic(topicId, TopicType.LISTENING);
        return topicReponse;
    }

    @Override
    @Transactional
    public List<ListeningResponse> addListeningList(String topicId,
                                                    List<ListeningRequest> requests,
                                                    List<MultipartFile> imageFiles,
                                                    List<MultipartFile> audioFiles) {
        // tìm topic theo id
        ListeningTopic topic = listeningTopicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found"));

        // map request -> entity
        List<Listening> listenings = listeningMapper.toListeningEntities(requests);
        List<String> uploadedPublicIds = new ArrayList<>();

        try {
            for (int i = 0; i < listenings.size(); i++) {
                Listening listening = listenings.get(i);
                listening.setTopic(topic);
                listening.setCreatedAt(LocalDateTime.now());

                // upload image nếu có
                if (imageFiles != null && imageFiles.size() > i && imageFiles.get(i) != null && !imageFiles.get(i).isEmpty()) {
                    var imageResponse = fileService.uploadImage(imageFiles.get(i));
                    listening.setImageUrl(imageResponse.getUrl());
                    listening.setPublicImageId(imageResponse.getPublicId());
                    uploadedPublicIds.add(imageResponse.getPublicId());
                }

                // upload audio nếu có
                if (audioFiles != null && audioFiles.size() > i && audioFiles.get(i) != null && !audioFiles.get(i).isEmpty()) {
                    var audioResponse = fileService.uploadAudio(audioFiles.get(i));
                    listening.setAudioUrl(audioResponse.getUrl());
                    listening.setPublicAudioId(audioResponse.getPublicId());
                    uploadedPublicIds.add(audioResponse.getPublicId());
                }
            }

            // save hết vào db
            listeningRepository.saveAll(listenings);

        } catch (Exception e) {
            // rollback file đã upload
            for (String publicId : uploadedPublicIds) {
                fileService.deleteFile(publicId);
            }
            throw new RuntimeException("Failed to save listening list", e);
        }

        // trả về response
        return listeningMapper.toListeningResponse(listenings);
    }

    @Override
    @Transactional
    public List<ListeningResponse> updateListening(
            List<ListeningRequest> requests,
            List<MultipartFile> imageFiles,
            List<MultipartFile> audioFiles) {

        // 1️⃣ Ánh xạ file theo tên file
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

        // 2️⃣ Lấy danh sách các listening hiện có (nếu có id)
        Map<String, Listening> idToListening = listeningRepository
                .findAllById(
                        requests.stream()
                                .map(ListeningRequest::getId)
                                .filter(Objects::nonNull)
                                .toList()
                )
                .stream()
                .collect(Collectors.toMap(Listening::getId, l -> l));

        // 3️⃣ Chuẩn bị danh sách thao tác
        List<String> deleteIds = new ArrayList<>();
        List<Listening> toSave = new ArrayList<>();
        List<String> uploadedFileIds = new ArrayList<>();

        try {
            for (ListeningRequest req : requests) {
                switch (req.getAction()) {
                    case DELETE -> {
                        String id = req.getId();
                        if (id == null) break;

                        deleteIds.add(id);
                        Listening listening = idToListening.get(id);
                        if (listening != null) {
                            if (listening.getPublicImageId() != null)
                                fileService.deleteFile(listening.getPublicImageId());
                            if (listening.getPublicAudioId() != null)
                                fileService.deleteFile(listening.getPublicAudioId());
                        }
                    }

                    case UPDATE, ADD -> {
                        Listening entity;

                        if (req.getAction() == RequestType.UPDATE && idToListening.containsKey(req.getId())) {
                            entity = idToListening.get(req.getId());
                            listeningMapper.patchUpdateListening(entity, req);
                        } else {
                            entity = listeningMapper.toListeningEnty(req);
                            entity.setCreatedAt(LocalDateTime.now());
                        }

                        // ✅ Upload image nếu có
                        if (req.getImageName() != null) {
                            MultipartFile img = fileMap.get(req.getImageName());
                            if (img != null) {
                                FileResponse fr = fileService.uploadImage(img, entity.getPublicImageId());
                                entity.setImageUrl(fr.getUrl());
                                entity.setPublicImageId(fr.getPublicId());
                                uploadedFileIds.add(fr.getPublicId());
                            }
                        }

                        // ✅ Upload audio nếu có
                        if (req.getAudioName() != null) {
                            MultipartFile audio = fileMap.get(req.getAudioName());
                            if (audio != null) {
                                FileResponse fr = fileService.uploadAudio(audio, entity.getPublicAudioId());
                                entity.setAudioUrl(fr.getUrl());
                                entity.setPublicAudioId(fr.getPublicId());
                                uploadedFileIds.add(fr.getPublicId());
                            }
                        }

                        toSave.add(entity);
                    }
                }
            }

            // 4️⃣ Xóa các bản ghi DELETE
            if (!deleteIds.isEmpty()) {
                listeningRepository.deleteAllById(deleteIds);
            }

            // 5️⃣ Lưu các bản ghi ADD/UPDATE
            listeningRepository.saveAll(toSave);

        } catch (Exception e) {
            for (String pid : uploadedFileIds) {
                fileService.deleteFile(pid);
            }
            throw e;
        }

        return listeningMapper.toListeningResponse(toSave);
    }


    @Override
    public void deleteListening(String id) {
        Listening listening = listeningRepository.findById(id).orElseThrow(()->new NotFoundException("Listening not found"));
        if(listening.getPublicAudioId()!=null){
            fileService.deleteFile(listening.getPublicAudioId());
        }
        if(listening.getPublicImageId()!=null){
            fileService.deleteFile(listening.getPublicImageId());
        }
    }

    @Override
    public ListeningTopicResponse getTestsByTopic(String topicId, int page, int size) {
        var topic = listeningTopicRepository.findById(topicId).orElseThrow(()->new RuntimeException("Topic Not found"));
        Page<ListeningTest> listeningTestPage = listeningTestRepository.findTestsByTopicId(topicId, PageRequest.of(page,size));

        var listeningTests = listeningMapper.toTestResponses(listeningTestPage.getContent());
        Page<ListeningTestReponse> testReponses = new PageImpl<>(listeningTests,PageRequest.of(page,size),listeningTestPage.getTotalElements());
        var topicReponse = listeningMapper.toTopicResponse(topic);
        topicReponse.setTests(testReponses);
        return topicReponse;
    }

    @Override
    @Transactional
    public ListeningTestReponse addTest(String topicId, ListeningTestRequest request, List<MultipartFile> imageFiles, List<MultipartFile> audioFiles) {
        // 1. tìm topic
        ListeningTopic topic = listeningTopicRepository.findById(topicId)
                .orElseThrow(() -> new NotFoundException("Topic not found"));

        // 2. tạo test
        ListeningTest test = ListeningTest.builder()
                .topic(topic)
                .name(request.getName())
                .duration(request.getDuration())
                .createdAt(LocalDateTime.now())
                .build();

        test = listeningTestRepository.save(test);

        // 3. map question request -> entity
        var questions = listeningMapper.toTestQuestions(request.getQuestions());
        List<String> uploadedPublicIds = new ArrayList<>();

        try {
            for (int i = 0; i < questions.size(); i++) {
                var q = questions.get(i);
                q.setTest(test);

                // upload image nếu có
                if (imageFiles != null && imageFiles.size() > i && imageFiles.get(i) != null && !imageFiles.get(i).isEmpty()) {
                    var imageResponse = fileService.uploadImage(imageFiles.get(i));
                    q.setImageUrl(imageResponse.getUrl());
                    q.setPublicImageId(imageResponse.getPublicId());
                    uploadedPublicIds.add(imageResponse.getPublicId());
                }

                // upload audio nếu có
                if (audioFiles != null && audioFiles.size() > i && audioFiles.get(i) != null && !audioFiles.get(i).isEmpty()) {
                    var audioResponse = fileService.uploadAudio(audioFiles.get(i));
                    q.setAudioUrl(audioResponse.getUrl());
                    q.setPublicAudioId(audioResponse.getPublicId());
                    uploadedPublicIds.add(audioResponse.getPublicId());
                }
            }

            // 4. lưu toàn bộ question
            // (bạn cần repository riêng cho ListeningTestQuestion, giống VocabularyTestQuestionRepository)
            // giả sử tên repo là listeningTestQuestionRepository
            questions = listeningTestQuestionRepository.saveAll(questions);

        } catch (Exception e) {
            // rollback các file đã upload
            for (String publicId : uploadedPublicIds) {
                fileService.deleteFile(publicId);
            }
            throw new RuntimeException("Failed to save listening test", e);
        }

        // 5. map về response
        ListeningTestReponse response = listeningMapper.toTestResponse(test);
        response.setQuestions(listeningMapper.toTestQuestionResponses(questions));

        return response;
    }

    @Override
    public ListeningTestReponse getTestDetail(String testId) {
        // 1. lấy test từ DB
        ListeningTest test = listeningTestRepository.findById(testId)
                .orElseThrow(() -> new NotFoundException("Test not found"));

        // 2. lấy các câu hỏi theo testId
        var questions = listeningTestQuestionRepository.findByTestId(testId);

        // 3. map sang response
        ListeningTestReponse response = listeningMapper.toTestResponse(test);
        response.setQuestions(listeningMapper.toTestQuestionResponses(questions));

        return response;
    }

    @Override
    public List<ListeningTestReponse> getTestByIds(List<String> ids) {
        List<ListeningTest> tests = listeningTestRepository.findAllById(ids);
        return listeningMapper.toTestResponses(tests);
    }

    @Override
    @Transactional
    public ListeningTestReponse updateTest(
            String testId,
            ListeningTestRequest request,
            List<MultipartFile> imageFiles,
            List<MultipartFile> audioFiles) {

        // 1️⃣ Lấy test hiện tại
        ListeningTest test = listeningTestRepository.findById(testId)
                .orElseThrow(() -> new NotFoundException("Test not found"));

        test.setName(request.getName());
        test.setDuration(request.getDuration());
        listeningTestRepository.save(test);

        // 2️⃣ Lấy các câu hỏi hiện tại
        List<ListeningTestQuestion> existingQuestions = listeningTestQuestionRepository.findByTestId(testId);
        Map<String, ListeningTestQuestion> idToQuestion = existingQuestions.stream()
                .collect(Collectors.toMap(ListeningTestQuestion::getId, q -> q));

        // 3️⃣ Ánh xạ file
        Map<String, MultipartFile> fileMap = new HashMap<>();
        if (imageFiles != null) {
            for (MultipartFile file : imageFiles) {
                if (file != null && !file.isEmpty())
                    fileMap.put(file.getOriginalFilename(), file);
            }
        }
        if (audioFiles != null) {
            for (MultipartFile file : audioFiles) {
                if (file != null && !file.isEmpty())
                    fileMap.put(file.getOriginalFilename(), file);
            }
        }

        List<ListeningTestQuestion> toSave = new ArrayList<>();
        List<String> deleteIds = new ArrayList<>();
        List<String> uploadedPublicIds = new ArrayList<>();

        try {
            for (var req : request.getQuestions()) {
                switch (req.getAction()) {
                    case DELETE -> {
                        ListeningTestQuestion q = idToQuestion.get(req.getId());
                        if (q != null) {
                            if (q.getPublicImageId() != null) fileService.deleteFile(q.getPublicImageId());
                            if (q.getPublicAudioId() != null) fileService.deleteFile(q.getPublicAudioId());
                            deleteIds.add(req.getId());
                        }
                    }

                    case UPDATE -> {
                        ListeningTestQuestion q = idToQuestion.get(req.getId());
                        if (q == null) continue;

                        listeningMapper.updateListeningTestQuestion(q, req);

                        if (req.getImageName() != null) {
                            MultipartFile image = fileMap.get(req.getImageName());
                            if (image != null) {
                                FileResponse fr = fileService.uploadImage(image, q.getPublicImageId());
                                q.setImageUrl(fr.getUrl());
                                q.setPublicImageId(fr.getPublicId());
                                uploadedPublicIds.add(fr.getPublicId());
                            }
                        }

                        if (req.getAudioName() != null) {
                            MultipartFile audio = fileMap.get(req.getAudioName());
                            if (audio != null) {
                                FileResponse fr = fileService.uploadAudio(audio, q.getPublicAudioId());
                                q.setAudioUrl(fr.getUrl());
                                q.setPublicAudioId(fr.getPublicId());
                                uploadedPublicIds.add(fr.getPublicId());
                            }
                        }

                        toSave.add(q);
                    }

                    case ADD -> {
                        ListeningTestQuestion newQ = listeningMapper.toTestQuestion(req);
                        newQ.setId(null);
                        newQ.setTest(test);

                        if (req.getImageName() != null) {
                            MultipartFile image = fileMap.get(req.getImageName());
                            if (image != null) {
                                FileResponse fr = fileService.uploadImage(image);
                                newQ.setImageUrl(fr.getUrl());
                                newQ.setPublicImageId(fr.getPublicId());
                                uploadedPublicIds.add(fr.getPublicId());
                            }
                        }

                        if (req.getAudioName() != null) {
                            MultipartFile audio = fileMap.get(req.getAudioName());
                            if (audio != null) {
                                FileResponse fr = fileService.uploadAudio(audio);
                                newQ.setAudioUrl(fr.getUrl());
                                newQ.setPublicAudioId(fr.getPublicId());
                                uploadedPublicIds.add(fr.getPublicId());
                            }
                        }

                        toSave.add(newQ);
                    }
                }
            }

            if (!deleteIds.isEmpty()) {
                listeningTestQuestionRepository.deleteAllById(deleteIds);
            }

            listeningTestQuestionRepository.saveAll(toSave);

        } catch (Exception e) {
            for (String pid : uploadedPublicIds) {
                fileService.deleteFile(pid);
            }
            throw e;
        }

        // 4️⃣ Map response
        ListeningTestReponse response = listeningMapper.toTestResponse(test);
        response.setQuestions(listeningMapper.toTestQuestionResponses(toSave));
        return response;
    }


    @Override
    @Transactional
    public void deleteTest(String testId) {
        List<ListeningTestQuestion> questions = listeningTestQuestionRepository.findByTestId(testId);
        try{
            for(var q: questions){
                if(q.getPublicAudioId()!=null)
                    fileService.deleteFile(q.getPublicAudioId());
                if(q.getPublicImageId()!=null)
                    fileService.deleteFile(q.getPublicImageId());
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        listeningTestQuestionRepository.deleteByTestId(testId);
        listeningTestRepository.deleteById(testId);
    }

}
