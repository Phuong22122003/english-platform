package com.english.content_service.exception;


import com.english.dto.response.*;
import com.english.exception.AppException;
import com.english.exception.BadRequestException;
import com.english.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<?>> handlerBadRequest(BadRequestException ex){
        log.error(ex.getMessage());
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(value = RuntimeException.class)
    ResponseEntity<ApiResponse> handlingRuntimeException(RuntimeException runtimeException) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        apiResponse.setMessage("Internal service Error");
        log.error(runtimeException.getMessage());
        return  ResponseEntity.internalServerError().body(apiResponse);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handlerNotFoundException(NotFoundException ex){
        log.error(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.builder()
                        .code(HttpStatus.NOT_FOUND.value())
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<?> handelForAppException(AppException ex){
        log.error(ex.getMessage());
        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .code(ex.getCode())
                        .message(ex.getMessage())
                        .build());
    }
}
