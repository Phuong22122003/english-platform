package com.english.content_service.service.implt;

import com.english.content_service.dto.response.TopicViewSummaryResponse;
import com.english.content_service.entity.TopicViewStatistic;
import com.english.content_service.entity.VocabularyTopic;
import com.english.content_service.repository.*;
import com.english.content_service.service.GrammarService;
import com.english.content_service.service.ListeningService;
import com.english.content_service.service.StatisticService;
import com.english.content_service.service.VocabularyService;
import com.english.dto.response.GrammarTopicResponse;
import com.english.dto.response.ListeningTopicResponse;
import com.english.dto.response.StatisticResponse;
import com.english.dto.response.VocabTopicResponse;
import com.english.enums.TimeRange;
import com.english.enums.TopicType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatisticServiceImplt implements StatisticService {

    TopicViewStatisticRepository topicViewStatisticRepository;
    GrammarService grammarService;
    VocabularyService vocabularyService;
    ListeningService listeningService;
    @Override
    public StatisticResponse getTopicViews(TimeRange timeRange) {
        LocalDate now = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate = now;

        // Xác định khoảng thời gian bắt đầu
        switch (timeRange) {
            case TODAY -> startDate = now;
            case ONE_WEEK -> startDate = now.minusWeeks(1);
            case ONE_MONTH -> startDate = now.minusMonths(1);
            case TWELVE_MONTHS, ALL -> startDate = now.minusYears(1);
            default -> throw new IllegalArgumentException("Invalid time range: " + timeRange);
        }

        // Lấy danh sách thống kê lượt xem trong khoảng thời gian đó
        List<TopicViewStatistic> statistics =
                topicViewStatisticRepository.findByViewDateBetween(startDate, endDate);

        // Group dữ liệu theo mốc thời gian phù hợp
        Map<String, Integer> groupedData;
        DateTimeFormatter formatter;

        switch (timeRange) {
            case TODAY -> { // group theo giờ
                groupedData = statistics.stream()
                        .collect(Collectors.groupingBy(
                                s -> String.format("%02d:00", s.getViewHour()), // "00:00", "01:00"
                                TreeMap::new,
                                Collectors.summingInt(TopicViewStatistic::getViewCount)
                        ));
            }
            case ONE_WEEK, ONE_MONTH -> { // group theo ngày
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                groupedData = statistics.stream()
                        .collect(Collectors.groupingBy(
                                s -> s.getViewDate().format(formatter),
                                TreeMap::new,
                                Collectors.summingInt(TopicViewStatistic::getViewCount)
                        ));
            }
            case TWELVE_MONTHS, ALL -> { // group theo tháng
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                groupedData = statistics.stream()
                        .collect(Collectors.groupingBy(
                                s -> YearMonth.from(s.getViewDate()).format(formatter),
                                TreeMap::new,
                                Collectors.summingInt(TopicViewStatistic::getViewCount)
                        ));
            }
            default -> throw new IllegalArgumentException("Invalid time range: " + timeRange);
        }

        //
        int totalViews = statistics.stream()
                .mapToInt(TopicViewStatistic::getViewCount)
                .sum();

        // Trả về DTO
        StatisticResponse response = new StatisticResponse();
        response.setTotalCount(totalViews);
        response.setNewElementsByPeriod(groupedData);

        return response;
    }

    /*
    *  Get top n topics with the most views
    * */
    @Override
    public List<TopicViewSummaryResponse> getTopNTopics(int n) {
        List<TopicViewSummary> topicViewSummaries = topicViewStatisticRepository.findTopTopics(PageRequest.of(0, n));
        List<String> vocabIds = new ArrayList<>();
        List<String> listeningIds = new ArrayList<>();
        List<String> grammarIds = new ArrayList<>();
        Map<String, String> idToName = new HashMap<>();
        for(var summary : topicViewSummaries) {
            switch (summary.getTopicType()) {
                case VOCABULARY -> {
                    vocabIds.add(summary.getTopicId());
                }
                case GRAMMAR -> {
                    grammarIds.add(summary.getTopicId());
                }
                case LISTENING ->  {
                    listeningIds.add(summary.getTopicId());
                }
            }
        }
        if (!vocabIds.isEmpty()) {
            List<VocabTopicResponse> vocabTopics = vocabularyService.getTopicsByIds(vocabIds);
            for (VocabTopicResponse v : vocabTopics) {
                idToName.put(v.getId(), v.getName());
            }
        }

        if (!grammarIds.isEmpty()) {
            List<GrammarTopicResponse> grammarTopics = grammarService.getTopicsByIds(grammarIds);
            for (GrammarTopicResponse g : grammarTopics) {
                idToName.put(g.getId(), g.getName());
            }
        }

        if (!listeningIds.isEmpty()) {
            List<ListeningTopicResponse> listeningTopics = listeningService.getTopicsByIds(listeningIds);
            for (ListeningTopicResponse l : listeningTopics) {
                idToName.put(l.getId(), l.getName());
            }
        }
        List<TopicViewSummaryResponse> responses = new ArrayList<>();
        for(var summary : topicViewSummaries) {
            TopicViewSummaryResponse response = new TopicViewSummaryResponse();
            response.setTopicId(summary.getTopicId());
            response.setTopicType(summary.getTopicType());
            String name = idToName.get(summary.getTopicId());
            if(name == null) continue;
            response.setTotalViews(summary.getTotalViews().intValue());
            response.setTopicName(name);
            responses.add(response);
        }

        return responses;
    }
}
