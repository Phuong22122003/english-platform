package com.english.learning_service.service.implt;

import com.english.exception.NotFoundException;
import com.english.exception.UnauthorizedException;
import com.english.learning_service.dto.request.CommentRequest;
import com.english.learning_service.dto.response.CommentResponse;
import com.english.learning_service.dto.response.UserResponse;
import com.english.learning_service.entity.Comment;
import com.english.learning_service.entity.User;
import com.english.learning_service.enums.ItemTypeEnum;
import com.english.learning_service.httpclient.UserClient;
import com.english.learning_service.repository.CommentRepository;
import com.english.learning_service.service.CommentService;
import com.english.learning_service.service.UserService;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;


@Slf4j
@Service
@Data
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentServiceImplt implements CommentService {
    CommentRepository commentRepository;
    UserClient userClient;
    UserService userService;
    ConcurrentHashMap<String, List<SseEmitter>> groupSse = new ConcurrentHashMap<>();
    private static int MAX_SSE_CONNECTION = 100;
    @Override
    public List<CommentResponse> getCommentsOfTest(String testId) {
        List<Comment> comments = commentRepository.findByTestIdOrderByCommentedAtDesc(testId);
        Set<String> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        List<User> users = userService.getUsers(userIds.stream().toList());
        Map<String, User> userMap = new HashMap<>();
        users.forEach(u-> userMap.put(u.getId(),u));

        List<CommentResponse> responses = new ArrayList<>();
        Map<String, List<CommentResponse>> replyCommentMap = new HashMap<>();
        for(Comment comment: comments){
            User user = userMap.get(comment.getUserId());

            CommentResponse commentResponse = CommentResponse.builder()
                    .id(comment.getId())
                    .replyComments(new ArrayList<>())
                    .avatarUrl(user!=null?user.getAvatarUrl(): null)
                    .username(user!=null?user.getUsername():null)
                    .content(comment.getContent())
                    .commentedAt(comment.getCommentedAt())
                    .build();
            if(comment.getParentId()!=null){
                List<CommentResponse> replyComments = replyCommentMap.computeIfAbsent(comment.getParentId(), k -> new ArrayList<>());
                replyComments.addFirst(commentResponse);
            }
            else {
                responses.add(commentResponse);
                commentResponse.setReplyComments(new ArrayList<>());
            }
        }
        for(CommentResponse comment: responses){
            List<CommentResponse> replyComments = replyCommentMap.get(comment.getId());
            if(replyComments == null || replyComments.isEmpty()) continue;
            comment.getReplyComments().addAll(replyCommentMap.get(comment.getId()));
        }

        return responses;
    }

    @Override
    public CommentResponse addComment(CommentRequest comment) {
        var context = SecurityContextHolder.getContext();
        String userId = context.getAuthentication().getName();
        User user = userService.getUserById(userId);
        Comment entity = Comment.builder()
                                .content(comment.getContent())
                                .replyId(comment.getReplyId())
                                .commentedAt(LocalDateTime.now())
                                .testId(comment.getTestId())
                                .testType(ItemTypeEnum.FULL_TEST)
                                .userId(userId)
                                .parentId(comment.getParentId())
                                .build();
        commentRepository.save(entity);
        CommentResponse commentResponse =  CommentResponse.builder()
                .id(entity.getId())
                .username(user.getUsername())
                .avatarUrl(user.getAvatarUrl())
                .content(entity.getContent())
                .commentedAt(entity.getCommentedAt())
                .parentId(entity.getParentId())
                .build();
        sendCommentNotification(comment.getTestId(),commentResponse);
        return commentResponse;
    }
    private void sendCommentNotification(String testId,CommentResponse comment){
        List<SseEmitter> sses = groupSse.get(testId);
        if(sses == null || sses.isEmpty()) return;
        sses.forEach(sseEmitter -> {
            try {
                if(sseEmitter == null ) return;
                sseEmitter.send(comment);
            } catch (IOException e) {
                sses.remove(sseEmitter);
            }
        });
    }
    @Override
    public SseEmitter getSse(String testId) {
        SseEmitter sse = new SseEmitter(5* 60 * 1000L);
        List<SseEmitter> sses = groupSse.computeIfAbsent(testId, k -> new CopyOnWriteArrayList<>());
        if(sses.size() > MAX_SSE_CONNECTION){
            sses.removeFirst();
        }

        sses.add(sse);
        sse.onTimeout(()->{
            sses.remove(sse);
        });
        sse.onError((e)->{
            sses.remove(sse);
        });
        sse.onCompletion(()->{
            sses.remove(sse);
        });
        try {
            sse.send(SseEmitter.event().name("INIT").data("Connected!"));
        } catch (IOException e) {
            sse.completeWithError(e);
        }
        return sse;
    }

    @Override
    public void deleteComment(String commentId) {
        var context = SecurityContextHolder.getContext();
        String userId = context.getAuthentication().getName();
        Comment comment = commentRepository.findById(commentId).orElseThrow(()->new NotFoundException("Comment with id: "+commentId+" not found"));
        if(!comment.getUserId().equals(userId)) throw new UnauthorizedException("Comment is not belonging to user with id " + userId);
        commentRepository.deleteById(commentId);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCommentByAdmin(String commentId) {
        commentRepository.deleteById(commentId);
    }
}
