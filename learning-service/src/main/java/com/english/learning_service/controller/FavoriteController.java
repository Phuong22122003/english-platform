package com.english.learning_service.controller;

import com.english.learning_service.dto.request.FavoriteRequest;
import com.english.learning_service.dto.response.FavoriteResponse;
import com.english.learning_service.enums.FilterType;
import com.english.learning_service.service.FavoriteService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FavoriteController {
    private FavoriteService favoriteService;
    @PostMapping
    public FavoriteResponse addFavorite(@RequestBody FavoriteRequest request) {
        return favoriteService.addFavorite(request);
    }

    // Xoá mục yêu thích theo ID
    @DeleteMapping("/{favoriteId}")
    public void deleteFavorite(@PathVariable String favoriteId) {
        favoriteService.deleteFavorite(favoriteId);
    }

    // Lấy danh sách mục yêu thích (theo filterType)
    @GetMapping
    public List<FavoriteResponse> getFavorites(@RequestParam(required = false) FilterType filterType) {
        return favoriteService.getFavorites(filterType);
    }

    @GetMapping("/ids")
    public ResponseEntity<List<FavoriteResponse>> getFavoriteIds(@RequestParam(required = false) FilterType filterType){
        return ResponseEntity.ok().body(favoriteService.getFavoriteIds(filterType));
    }

}
