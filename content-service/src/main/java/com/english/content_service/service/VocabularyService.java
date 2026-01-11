package com.english.content_service.service;
import java.util.List;

import com.english.content_service.dto.request.VocabTopicRequest;
import com.english.content_service.dto.request.VocabularyRequest;
import com.english.content_service.dto.request.VocabularyTestRequest;

import com.english.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface VocabularyService {
    //topic
    public Page<VocabTopicResponse> search(String query, int page, int limit);
    public Page<VocabTopicResponse> getTopics(int page, int size);
    public VocabTopicResponse addTopic(VocabTopicRequest request, MultipartFile imageFile);
    public VocabTopicResponse updateTopic(String topicId, VocabTopicRequest request, MultipartFile imageFile);
    public void deleteTopic(String topicId);
    public List<VocabTopicResponse> getTopicsByIds(List<String> ids);
    //vocab
    public VocabTopicResponse getVocabulariesByTopicId(String topicId);
    public List<VocabularyResponse> addVocabularies(String topicId,MultipartFile excelFile,List<MultipartFile> imageFiles, List<MultipartFile> audioFiles);
    public List<VocabularyResponse> addVocabularies(String topicId, List<VocabularyRequest> requests, List<MultipartFile> imageFiles, List<MultipartFile> audioFiles);
    public List<VocabularyResponse> updateVocabularies(String topicId, List<VocabularyRequest> requests, List<MultipartFile> imageFiles, List<MultipartFile> audioFiles);
    public VocabularyResponse updateVocabulary(String vocabId, VocabularyRequest request, MultipartFile imageFile, MultipartFile audioFile);
    public void deleteVocabulary(String vocabId);

    //test
    public VocabTopicResponse getTestsByTopicId(String topicId, int page, int size);
    public VocabularyTestResponse getTestQuestionsByTestId(String testId);
    public VocabularyTestResponse addTest(String topicId,VocabularyTestRequest vocabularyTestRequest, List<MultipartFile> imageFiles);
    public VocabularyTestResponse addTest(String topicId,MultipartFile excelFile, List<MultipartFile> imageFiles);
    public VocabularyTestResponse updateTest(String testId,VocabularyTestRequest vocabularyTestRequest, List<MultipartFile> imageFiles);
    public List<VocabularyTestResponse> getTestsByIds(List<String> ids);
    public void deleteTest(String testId);

}
