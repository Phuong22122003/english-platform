package com.english.content_service.mapper;

import com.english.content_service.dto.request.ToeicTestQuestionGroupRequest;
import com.english.content_service.dto.request.ToeicTestQuestionRequest;
import com.english.content_service.entity.ToeicTest;
import com.english.content_service.entity.ToeicTestGroup;
import com.english.content_service.entity.ToeicTestQuestion;
import com.english.content_service.entity.ToeicTestQuestionGroup;
import com.english.dto.response.ToeicTestGroupResponse;
import com.english.dto.response.ToeicTestQuestionGroupResponse;
import com.english.dto.response.ToeicTestQuestionResponse;
import com.english.dto.response.ToeicTestResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ToeicMapper {
    // ============================== GROUP ==========================
    ToeicTestGroupResponse toGroupResponse(ToeicTestGroup group);

    List<ToeicTestGroupResponse> toGroupResponses(List<ToeicTestGroup> groups);


    // ============================== TEST ==========================
    ToeicTestResponse toTestResponse(ToeicTest test);

    List<ToeicTestResponse> toTestResponses(List<ToeicTest> tests);


    // ============================ QUESTION GROUP ========================

    List<ToeicTestQuestionGroup> toQuestionGroups(List<ToeicTestQuestionGroupRequest> requests);

    ToeicTestQuestionGroup toQuestionGroup(ToeicTestQuestionGroupRequest request);

    List<ToeicTestQuestionGroupResponse> toQuestionGroupResponses(List<ToeicTestQuestionGroup> requests);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "questions", source = "questions", ignore = true)
    void updateGroupQuestion(@MappingTarget ToeicTestQuestionGroup target, ToeicTestQuestionGroupRequest request);
    // ============================== QUESTION ==========================
    ToeicTestQuestion toTestQuestion(ToeicTestQuestionRequest request);

    List<ToeicTestQuestion> toTestQuestions(List<ToeicTestQuestionRequest> requests);

    ToeicTestQuestionResponse toQuestionResponse(ToeicTestQuestion q);

    List<ToeicTestQuestionResponse> toQuestionResponses(List<ToeicTestQuestion> questions);


    // ============================== UPDATE QUESTION ==========================
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateTestQuestion(@MappingTarget ToeicTestQuestion entity, ToeicTestQuestionRequest request);
}
