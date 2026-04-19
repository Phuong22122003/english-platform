package com.english.user_service.mapper;

import com.english.user_service.dto.request.UserProfileUpdateRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;

import com.english.user_service.dto.response.UserResponse;
import com.english.user_service.entity.User;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    public UserResponse toUserResponse(User user);
    public List<UserResponse> toUserResponses(List<User> users);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromDto(UserProfileUpdateRequest dto, @MappingTarget User user);
}
