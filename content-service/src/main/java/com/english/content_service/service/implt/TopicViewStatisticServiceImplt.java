package com.english.content_service.service.implt;

import com.english.content_service.entity.TopicViewStatistic;
import com.english.content_service.repository.TopicViewStatisticRepository;
import com.english.content_service.service.TopicViewStatisticService;
import com.english.enums.TopicType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TopicViewStatisticServiceImplt implements TopicViewStatisticService {

    private final TopicViewStatisticRepository topicViewStatisticRepository;

    @Override
    public void addTopic(String topicId, TopicType topicType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate date = now.toLocalDate();
        int hour = now.getHour();

        topicViewStatisticRepository.findByTopicIdAndTopicTypeAndViewDateAndViewHour(
                topicId, topicType, date, hour
        ).ifPresentOrElse(statistic -> {
            // Nếu đã tồn tại, tăng view_count
            statistic.setViewCount(statistic.getViewCount() + 1);
            topicViewStatisticRepository.save(statistic);
        }, () -> {
            // Nếu chưa có, tạo mới
            TopicViewStatistic newStat = TopicViewStatistic.builder()
                    .topicId(topicId)
                    .topicType(topicType)
                    .viewDate(date)
                    .viewHour(hour)
                    .viewCount(1)
                    .build();
            topicViewStatisticRepository.save(newStat);
        });
    }
}
