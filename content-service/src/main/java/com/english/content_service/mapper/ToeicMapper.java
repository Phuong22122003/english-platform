package com.english.content_service.mapper;

import com.english.content_service.dto.request.ToeicTestQuestionRequest;
import com.english.content_service.entity.ToeicTest;
import com.english.content_service.entity.ToeicTestGroup;
import com.english.content_service.entity.ToeicTestQuestion;
import com.english.dto.response.ToeicTestGroupResponse;
import com.english.dto.response.ToeicTestQuestionResponse;
import com.english.dto.response.ToeicTestResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ToeicMapper {
    // ============================== GROUP ==========================
    ToeicTestGroupResponse toGroupResponse(ToeicTestGroup group);

    List<ToeicTestGroupResponse> toGroupResponses(List<ToeicTestGroup> groups);


    // ============================== TEST ==========================
    ToeicTestResponse toTestResponse(ToeicTest test);

    List<ToeicTestResponse> toTestResponses(List<ToeicTest> tests);


    // ============================== QUESTION ==========================
    ToeicTestQuestion toTestQuestion(ToeicTestQuestionRequest request);

    List<ToeicTestQuestion> toTestQuestions(List<ToeicTestQuestionRequest> requests);

    ToeicTestQuestionResponse toQuestionResponse(ToeicTestQuestion q);

    List<ToeicTestQuestionResponse> toQuestionResponses(List<ToeicTestQuestion> questions);


    // ============================== UPDATE QUESTION ==========================
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateTestQuestion(@MappingTarget ToeicTestQuestion entity, ToeicTestQuestionRequest request);
}
