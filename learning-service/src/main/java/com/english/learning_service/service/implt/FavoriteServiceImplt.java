package com.english.learning_service.service.implt;

import com.english.dto.response.GrammarTopicResponse;
import com.english.dto.response.ListeningTopicResponse;
import com.english.dto.response.VocabTopicResponse;
import com.english.learning_service.dto.request.FavoriteRequest;
import com.english.learning_service.dto.response.FavoriteResponse;
import com.english.learning_service.entity.Favorite;
import com.english.learning_service.enums.FilterType;
import com.english.learning_service.enums.ItemTypeEnum;
import com.english.learning_service.httpclient.GrammarClient;
import com.english.learning_service.httpclient.ListeningClient;
import com.english.learning_service.httpclient.VocabularyClient;
import com.english.learning_service.mapper.FavoriteMapper;
import com.english.learning_service.repository.FavoriteRepository;
import com.english.learning_service.service.FavoriteService;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Data
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FavoriteServiceImplt implements FavoriteService {
    FavoriteMapper favoriteMapper;
    FavoriteRepository favoriteRepository;
    GrammarClient grammarClient;
    ListeningClient listeningClient;
    VocabularyClient vocabularyClient;
    @Override
    public FavoriteResponse addFavorite(FavoriteRequest request) {
        Favorite favorite = favoriteMapper.toFavorite(request);
        var context = SecurityContextHolder.getContext();
        String userId = context.getAuthentication().getName();
        favorite.setUserId(userId);
        favorite.setAddedAt(LocalDateTime.now());
        favorite = favoriteRepository.save(favorite);
        return favoriteMapper.toFavoriteResponse(favorite);
    }

    @Override
    public void deleteFavorite(String favoriteId) {
        favoriteRepository.deleteById(favoriteId);
    }

    @Override
    public List<FavoriteResponse> getFavorites(FilterType filterType) {
        var context = SecurityContextHolder.getContext();
        String userId = context.getAuthentication().getName();

        // 1️⃣ Lấy danh sách favorites
        List<Favorite> favorites;
        if (filterType.equals(FilterType.ALL)) {
            favorites = favoriteRepository.findByUserId(userId);
        } else {
            favorites = favoriteRepository.findByUserIdAndItemType(userId, ItemTypeEnum.map(filterType));
        }

        if (favorites.isEmpty()) {
            return List.of();
        }

        // 2️⃣ Gom ID theo loại
        List<String> grammarIds = new ArrayList<>();
        List<String> vocabIds = new ArrayList<>();
        List<String> listeningIds = new ArrayList<>();

        for (Favorite f : favorites) {
            if (f.getItemType().equals(ItemTypeEnum.GRAMMAR)) {
                grammarIds.add(f.getItemId());
            } else if (f.getItemType().equals(ItemTypeEnum.VOCABULARY)) {
                vocabIds.add(f.getItemId());
            } else if (f.getItemType().equals(ItemTypeEnum.LISTENING)) {
                listeningIds.add(f.getItemId());
            }
        }

        // 3️⃣ Gọi API để lấy thông tin chi tiết
        Map<String, GrammarTopicResponse> grammarMap = new HashMap<>();
        Map<String, VocabTopicResponse> vocabMap = new HashMap<>();
        Map<String, ListeningTopicResponse> listeningMap = new HashMap<>();

        if (!grammarIds.isEmpty()) {
            List<GrammarTopicResponse> grammarTopics = grammarClient.getTopicsByIds(grammarIds);
            for (GrammarTopicResponse g : grammarTopics) {
                grammarMap.put(g.getId(), g);
            }
        }

        if (!vocabIds.isEmpty()) {
            List<VocabTopicResponse> vocabTopics = vocabularyClient.getTopicsByIds(vocabIds);
            for (VocabTopicResponse v : vocabTopics) {
                vocabMap.put(v.getId(), v);
            }
        }

        if (!listeningIds.isEmpty()) {
            List<ListeningTopicResponse> listeningTopics = listeningClient.getTopicsByIds(listeningIds);
            for (ListeningTopicResponse l : listeningTopics) {
                listeningMap.put(l.getId(), l);
            }
        }

        // 4️⃣ Tạo danh sách FavoriteResponse
        List<FavoriteResponse> responses = new ArrayList<>();

        for (Favorite f : favorites) {
            FavoriteResponse.FavoriteResponseBuilder builder = FavoriteResponse.builder()
                    .id(f.getId())
                    .itemType(f.getItemType())
                    .addedAt(f.getAddedAt());

            switch (f.getItemType()) {
                case GRAMMAR -> builder.grammarTopic(grammarMap.get(f.getItemId()));
                case VOCABULARY -> builder.vocabTopic(vocabMap.get(f.getItemId()));
                case LISTENING -> builder.listeningTopic(listeningMap.get(f.getItemId()));
            }

            responses.add(builder.build());
        }

        return responses;
    }

    @Override
    public List<FavoriteResponse> getFavoriteIds(FilterType filterType) {
        var context = SecurityContextHolder.getContext();
        String userId = context.getAuthentication().getName();

        List<Favorite> favorites;
        if (filterType.equals(FilterType.ALL)) {
            favorites = favoriteRepository.findByUserId(userId);
        } else {
            favorites = favoriteRepository.findByUserIdAndItemType(userId, ItemTypeEnum.map(filterType));
        }

        if (favorites.isEmpty()) {
            return List.of();
        }
        List<FavoriteResponse> response = new ArrayList<>();
        for(var fav: favorites){
            FavoriteResponse favoriteResponse = favoriteMapper.toFavoriteResponse(fav);
            if(fav.getItemType().equals(ItemTypeEnum.VOCABULARY)){
                var vocabTopic = new VocabTopicResponse();
                vocabTopic.setId(fav.getItemId());
                favoriteResponse.setVocabTopic(vocabTopic);
            } else if (fav.getItemType().equals(ItemTypeEnum.GRAMMAR)) {
                var grammarTopic = new GrammarTopicResponse();
                grammarTopic.setId(fav.getItemId());
                favoriteResponse.setGrammarTopic(grammarTopic);
            }else{
                var listeningTopic = new ListeningTopicResponse();
                listeningTopic.setId(fav.getItemId());
                favoriteResponse.setListeningTopic(listeningTopic);
            }
            response.add(favoriteResponse);
        }
        return response;
    }


}
