package com.english.content_service.mapper;

import java.util.List;

import com.english.content_service.dto.request.VocabularyTestQuestionRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.english.content_service.dto.request.VocabTopicRequest;
import com.english.content_service.dto.request.VocabularyRequest;
import com.english.dto.response.VocabTopicResponse;
import com.english.dto.response.VocabularyResponse;
import com.english.dto.response.VocabularyTestQuestionResponse;
import com.english.dto.response.VocabularyTestResponse;
import com.english.content_service.entity.Vocabulary;
import com.english.content_service.entity.VocabularyTest;
import com.english.content_service.entity.VocabularyTestQuestion;
import com.english.content_service.entity.VocabularyTopic;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VocabularyMapper {
    
    //topic
    VocabTopicResponse toVocabTopicResponse(VocabularyTopic vocabularyTopic);
    List<VocabTopicResponse> toVocabTopicResponses(List<VocabularyTopic> topics);
    @Mapping(ignore = true, target = "id")
    @Mapping(ignore = true, target = "createdAt")
    @Mapping(ignore = true, target = "imageUrl")
    VocabularyTopic toVocabTopic(VocabTopicRequest request);
    void updateTopic(@MappingTarget VocabularyTopic topic, VocabTopicRequest request);

    // vocabulary
    @Mapping(target = "topicId", source = "topic.id")
    VocabularyResponse toVocabularyResponse(Vocabulary vocabulary);
    List<VocabularyResponse> toVocabularyResponses(List<Vocabulary> vocabularies);

    @Mapping(ignore = true, target = "id")
    @Mapping(ignore = true, target = "topic")
    @Mapping(ignore = true, target = "imageUrl")
    @Mapping(ignore = true, target = "audioUrl")
    @Mapping(ignore = true, target = "createdAt")
    Vocabulary toVocabulary(VocabularyRequest request);
    List<Vocabulary> toVocabularies(List<VocabularyRequest> requests);

    void patchUpdate(@MappingTarget Vocabulary vocabulary, VocabularyRequest request);

    //test
    @Mapping(target = "topicId", source = "topic.id")
    @Mapping(target = "topicName", source = "topic.name")
    VocabularyTestResponse toVocabularyTestResponse(VocabularyTest test);
    List<VocabularyTestResponse> toVocabularyTestResponses(List<VocabularyTest> tests);


    //question
    @Mapping(target = "testId", source = "test.id")
    VocabularyTestQuestionResponse toVocabularyTestQuestionResponse(VocabularyTestQuestion question);
    List<VocabularyTestQuestionResponse> toVocabularyTestQuestionResponses(List<VocabularyTestQuestion> questions);

    List<VocabularyTestQuestion> toVocabularyTestQuestions(List<VocabularyTestQuestionRequest> requests);
    VocabularyTestQuestion toVocabularyTestQuestion(VocabularyTestQuestionRequest request);
    void updateVocabularyTestQuestion(@MappingTarget VocabularyTestQuestion question, VocabularyTestQuestionRequest request);

}
