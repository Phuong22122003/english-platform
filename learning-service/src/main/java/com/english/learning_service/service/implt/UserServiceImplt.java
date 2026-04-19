package com.english.learning_service.service.implt;

import com.english.exception.BadRequestException;
import com.english.learning_service.dto.request.UserRequest;
import com.english.learning_service.dto.response.UserResponse;
import com.english.learning_service.entity.User;
import com.english.learning_service.httpclient.UserClient;
import com.english.learning_service.repository.UserRepository;
import com.english.learning_service.service.UserService;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@Data
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImplt implements UserService {
    UserRepository userRepository;
    UserClient userClient;
    @Override
    public void updateUser(UserRequest request) {
        User user = userRepository.findById(request.getId()).orElse(null);
        if(user == null){
            user = User.builder()
                    .id(request.getId())
                    .username(request.getUsername())
                    .updatedAt(request.getRequestTime())
                    .createdAt(LocalDateTime.now())
                    .avatarUrl(request.getAvatarUrl())
                    .build();
        }
        else {
            // Prevent wrong info is updated
            if(user.getUpdatedAt().isAfter(request.getRequestTime())){
                log.info("User is not updated due to optimistic lock");
                return;
            }
            user.setAvatarUrl(request.getAvatarUrl());
            user.setUsername(request.getUsername());
            user.setUpdatedAt(request.getRequestTime());
        }
        log.info("Save user {}", user.getUsername());
        userRepository.save(user);
    }

    @Override
    public List<User> getUsers(List<String> ids) {
        List<User> users =  userRepository.findAllById(ids);
        if(users.size() != ids.size()){
            Set<String> missingUserIds = new HashSet<>();
            Set<String> setUserIds = new HashSet<>(users.stream().map(User::getId).toList());
            for(String id: ids){
                if(!setUserIds.contains(id)){
                    missingUserIds.add(id);
                }
            }
            for(String missingId: missingUserIds){
                User missingUser = User.builder()
                        .id(missingId)
                        .avatarUrl(null)
                        .username("anonymous").build();
                users.add(missingUser);
            }
           log.error("Missing some users {}",missingUserIds);
        }
        return  users;
    }

    @Override
    public User getUserById(String id) {
        return userRepository.findById(id).orElseGet(()->
        {

            return User.builder()
                    .id(id)
                    .avatarUrl(null)
                    .username("anonymous").build();
        }
        );
    }
}
