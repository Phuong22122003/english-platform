package com.english.learning_service.repository;

import com.english.learning_service.entity.Favorite;
import com.english.learning_service.enums.ItemTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, String> {
    public List<Favorite> findByUserIdAndItemType(String userId, ItemTypeEnum testType);
    public List<Favorite> findByUserId(String userId);
    public Favorite findByUserIdAndItemId(String userId, String itemId);
}
