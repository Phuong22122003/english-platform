package com.english.learning_service.service;

import com.english.learning_service.dto.request.UserRequest;
import com.english.learning_service.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService {
    public void updateUser(UserRequest user);
    public List<User> getUsers(List<String> ids);
    public User getUserById(String id);
}
