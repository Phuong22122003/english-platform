package com.english.content_service.mapper;

import com.english.content_service.dto.request.ListeningRequest;
import com.english.content_service.dto.request.ListeningTestQuestionRequest;
import com.english.content_service.dto.request.ListeningTopicRequest;
import com.english.dto.response.ListeningResponse;
import com.english.dto.response.ListeningTestQuestionResponse;
import com.english.dto.response.ListeningTestReponse;
import com.english.dto.response.ListeningTopicResponse;
import com.english.content_service.entity.Listening;
import com.english.content_service.entity.ListeningTest;
import com.english.content_service.entity.ListeningTestQuestion;
import com.english.content_service.entity.ListeningTopic;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ListeningMapper {

    //topic
    public ListeningTopicResponse toTopicResponse(ListeningTopic listeningTopic);
    public List<ListeningTopicResponse> toTopicResponses(List<ListeningTopic> listeningTopics);
    public ListeningTopic toTopicEntity(ListeningTopicRequest request);
    public void updateTopic(@MappingTarget  ListeningTopic topic,ListeningTopicRequest request);
    // listening
    @Mapping(target = "topic", ignore = true)
    public ListeningResponse toListeningResponse(Listening listening);
    public List<ListeningResponse> toListeningResponse(List<Listening> listeningList);
    public List<Listening> toListeningEntities(List<ListeningRequest> requests);
    public Listening toListeningEnty(ListeningRequest request);
    public void patchUpdateListening(@MappingTarget Listening listening, ListeningRequest request);
    //test
    public ListeningTestReponse toTestResponse(ListeningTest test);
    public List<ListeningTestReponse> toTestResponses(List<ListeningTest> tests);
    public List<ListeningTestQuestion> toTestQuestions(List<ListeningTestQuestionRequest> requests);
    public ListeningTestQuestion toTestQuestion(ListeningTestQuestionRequest request);
    public List<ListeningTestQuestionResponse> toTestQuestionResponses(List<ListeningTestQuestion> requests);
    public void updateListeningTestQuestion(@MappingTarget ListeningTestQuestion q, ListeningTestQuestionRequest request);
}
