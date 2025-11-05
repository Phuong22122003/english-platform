package com.english.user_service.controller;

import com.english.user_service.dto.request.UserProfileUpdateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.english.user_service.dto.response.UserResponse;
import com.english.user_service.service.UserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class UserController {

    UserService userService;

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteUserAccount(){
        userService.deleteUserAccount();
        return ResponseEntity.ok().body("User deleted successfully");
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(){
        return ResponseEntity.ok().body(userService.getProfile());
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(@RequestBody UserProfileUpdateRequest request) {
        return  ResponseEntity.ok().body(userService.updateUserProfile(request));
    }

    @PostMapping("/avatar")
    public ResponseEntity<String> uploadAvatar(@RequestPart MultipartFile avatar){
        return ResponseEntity.ok().body(userService.updateAvatar(avatar));
    }
}
