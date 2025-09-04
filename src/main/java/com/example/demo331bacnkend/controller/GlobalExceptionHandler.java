package com.example.demo331bacnkend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now().toString());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", "请求格式错误：请确保设置 Content-Type: application/json 并提供有效的JSON请求体");
        errorResponse.put("path", request.getDescription(false).replace("uri=", ""));
        errorResponse.put("详细说明", "POST请求必须包含JSON格式的请求体，并设置正确的Content-Type头");
        
        Map<String, Object> example = new HashMap<>();
        example.put("title", "活动标题");
        example.put("description", "活动描述");
        example.put("location", "活动地点");
        example.put("date", "2023-12-25");
        example.put("time", "14:00");
        example.put("category", "活动类别");
        example.put("petAllowed", true);
        example.put("organizer", "组织者名称");
        
        errorResponse.put("正确的JSON示例", example);
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
