package com.english.learning_service.service.implt;

import com.english.dto.response.StatisticResponse;
import com.english.enums.TimeRange;
import com.english.enums.TopicType;
import com.english.learning_service.dto.response.UserScore;
import com.english.learning_service.entity.ExamHistory;
import com.english.learning_service.repository.ExamHistoryRepository;
import com.english.learning_service.service.StatisticService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class StatisticServiceImplt implements StatisticService {
    ExamHistoryRepository examHistoryRepository;

    @Override
    public StatisticResponse getNumberOfTestIsTaken(TimeRange timeRange) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;
        LocalDateTime endDate = now;

        // 🧭 1️⃣ Xác định khoảng thời gian bắt đầu
        switch (timeRange) {
            case TODAY -> startDate = now.toLocalDate().atStartOfDay();
            case ONE_WEEK -> startDate = now.minusWeeks(1).toLocalDate().atStartOfDay();
            case ONE_MONTH -> startDate = now.minusMonths(1).toLocalDate().atStartOfDay();
            case TWELVE_MONTHS, ALL -> startDate = now.minusYears(1).toLocalDate().atStartOfDay();
            default -> throw new IllegalArgumentException("Invalid time range: " + timeRange);
        }

        // 🧭 2️⃣ Lấy danh sách bài test được làm trong khoảng thời gian đó
        List<ExamHistory> examHistories =
                examHistoryRepository.findByTakenAtBetween(startDate, endDate);

        // 🧭 3️⃣ Group dữ liệu theo mốc thời gian phù hợp
        Map<String, Integer> groupedData;
        DateTimeFormatter formatter;

        switch (timeRange) {
            case TODAY -> { // group theo giờ
                formatter = DateTimeFormatter.ofPattern("HH:00");
                groupedData = examHistories.stream()
                        .collect(Collectors.groupingBy(
                                e -> e.getTakenAt().format(formatter),
                                TreeMap::new, // giữ thứ tự tăng dần
                                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                        ));
            }
            case ONE_WEEK, ONE_MONTH -> { // group theo ngày
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                groupedData = examHistories.stream()
                        .collect(Collectors.groupingBy(
                                e -> e.getTakenAt().toLocalDate().format(formatter),
                                TreeMap::new,
                                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                        ));
            }
            case TWELVE_MONTHS, ALL -> { // group theo tháng
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                groupedData = examHistories.stream()
                        .collect(Collectors.groupingBy(
                                e -> YearMonth.from(e.getTakenAt()).format(formatter),
                                TreeMap::new,
                                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                        ));
            }
            default -> throw new IllegalArgumentException("Invalid time range: " + timeRange);
        }

        // 🧭 4️⃣ Tổng số bài test được làm trong time range
        int totalTestsTaken = examHistories.size();

        // 🧭 5️⃣ Trả về DTO
        StatisticResponse response = new StatisticResponse();
        response.setTotalCount(totalTestsTaken);
        response.setNewElementsByPeriod(groupedData);

        return response;
    }

    @Override
    public UserScore getUserScores(TimeRange timeRange, TopicType topicType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;
        LocalDateTime endDate = now;

        // 1️⃣ Xác định khoảng thời gian
        switch (timeRange) {
            case TODAY -> startDate = now.toLocalDate().atStartOfDay();
            case ONE_WEEK -> startDate = now.minusWeeks(1).toLocalDate().atStartOfDay();
            case ONE_MONTH -> startDate = now.minusMonths(1).toLocalDate().atStartOfDay();
            case TWELVE_MONTHS, ALL -> startDate = now.minusYears(1).toLocalDate().atStartOfDay();
            default -> throw new IllegalArgumentException("Invalid time range: " + timeRange);
        }

        // 2️⃣ Lấy dữ liệu trong khoảng thời gian đó
        List<ExamHistory> examHistories =
                examHistoryRepository.findByTakenAtBetween(startDate, endDate);

        // 3️⃣ Nếu có lọc theo topicType
        if (topicType != null) {
            examHistories = examHistories.stream()
                    .filter(e -> e.getTestType().name().equalsIgnoreCase(topicType.name()))
                    .collect(Collectors.toList());
        }

        // 4️⃣ Nhóm dữ liệu và tính trung bình điểm
        Map<String, Float> groupedScores;
        DateTimeFormatter formatter;

        switch (timeRange) {
            case TODAY -> { // nhóm theo giờ
                formatter = DateTimeFormatter.ofPattern("HH:00");
                groupedScores = examHistories.stream()
                        .collect(Collectors.groupingBy(
                                e -> e.getTakenAt().format(formatter),
                                TreeMap::new,
                                Collectors.averagingInt(ExamHistory::getScore)
                        ))
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().floatValue(), (a,b)->a, TreeMap::new));
            }

            case ONE_WEEK, ONE_MONTH -> { // nhóm theo ngày
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                groupedScores = examHistories.stream()
                        .collect(Collectors.groupingBy(
                                e -> e.getTakenAt().toLocalDate().format(formatter),
                                TreeMap::new,
                                Collectors.averagingInt(ExamHistory::getScore)
                        ))
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().floatValue(), (a,b)->a, TreeMap::new));
            }

            case TWELVE_MONTHS, ALL -> { // nhóm theo tháng
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                groupedScores = examHistories.stream()
                        .collect(Collectors.groupingBy(
                                e -> YearMonth.from(e.getTakenAt()).format(formatter),
                                TreeMap::new,
                                Collectors.averagingInt(ExamHistory::getScore)
                        ))
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().floatValue(), (a,b)->a, TreeMap::new));
            }

            default -> throw new IllegalArgumentException("Invalid time range: " + timeRange);
        }

        // 5️⃣ Tạo DTO trả về
        UserScore response = new UserScore();
        response.setScores(groupedScores);

        return response;
    }

}
