package com.english.content_service.service;
import com.english.content_service.dto.request.ListeningRequest;
import com.english.content_service.dto.request.ListeningTestRequest;
import com.english.content_service.dto.request.ListeningTopicRequest;
import com.english.dto.response.ListeningResponse;
import com.english.dto.response.ListeningTestReponse;
import com.english.dto.response.ListeningTopicResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public interface ListeningService {
    //topic
    public Page<ListeningTopicResponse> search(String query, int page, int limit);
    public Page<ListeningTopicResponse> getTopics(int page, int size);
    public ListeningTopicResponse addTopic(ListeningTopicRequest request, MultipartFile imageFile);
    public List<ListeningTopicResponse> getTopicsByIds(List<String> ids);
    public ListeningTopicResponse updateTopic(String topicId, ListeningTopicRequest request, MultipartFile multipartFile);
    public void deleteTopic(String topicId);
    //listening
    public ListeningTopicResponse getListeningByTopic(String topicId);
    public List<ListeningResponse> addListeningList(String topicId, List<ListeningRequest> requests, List<MultipartFile> imageFiles, List<MultipartFile> audioFiles);
    public List<ListeningResponse> addListeningList(String topicId, MultipartFile excelFile, List<MultipartFile> imageFiles, List<MultipartFile> audioFiles);
    public List<ListeningResponse> updateListening(List<ListeningRequest> requests, List<MultipartFile> imageFiles, List<MultipartFile> audioFiles);
    public void deleteListening(String id);
    // test
    public ListeningTopicResponse getTestsByTopic(String topic,int page, int size);
    public ListeningTestReponse addTest(String topicId, ListeningTestRequest request, List<MultipartFile> imageFiles,List<MultipartFile> audioFiles);
    public ListeningTestReponse addTest(String topicId, MultipartFile excelFile, List<MultipartFile> imageFiles,List<MultipartFile> audioFiles);
    public ListeningTestReponse getTestDetail(String testId);
    public List<ListeningTestReponse> getTestByIds(List<String> ids);
    public ListeningTestReponse updateTest(String testId, ListeningTestRequest request,List<MultipartFile> imageFiles,List<MultipartFile> audioFiles);
    public void deleteTest(String testId);
}
