package com.english.learning_service.service;

import com.english.learning_service.dto.request.FavoriteRequest;
import com.english.learning_service.dto.response.FavoriteResponse;
import com.english.learning_service.enums.FilterType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface FavoriteService {
    public FavoriteResponse addFavorite(FavoriteRequest request);
    public void deleteFavorite(String favoriteId);
    public List<FavoriteResponse> getFavorites(FilterType filterType);
    public List<FavoriteResponse> getFavoriteIds(FilterType filterType);
}
