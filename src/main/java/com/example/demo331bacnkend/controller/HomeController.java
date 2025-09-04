package com.example.demo331bacnkend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping(value = "/", produces = {MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<String> home() {
        String htmlResponse = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Demo 331 Backend API</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 40px; background: #f5f5f5; }
                    .container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    h1 { color: #333; text-align: center; margin-bottom: 30px; }
                    h2 { color: #666; border-bottom: 2px solid #4CAF50; padding-bottom: 5px; }
                    .endpoint { background: #f9f9f9; padding: 15px; margin: 10px 0; border-radius: 5px; border-left: 4px solid #4CAF50; }
                    .method { color: #2196F3; font-weight: bold; }
                    .url { font-family: monospace; background: #e8e8e8; padding: 2px 4px; border-radius: 3px; }
                    .status { color: #4CAF50; font-weight: bold; }
                    .example { background: #e3f2fd; padding: 10px; margin: 5px 0; border-radius: 4px; font-family: monospace; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🚀 Demo 331 Backend API</h1>
                    <p><strong>版本:</strong> 1.0.0 | <strong>状态:</strong> <span class="status">运行中</span></p>
                    
                    <h2>📍 可用的 API 端点</h2>
                    
                    <div class="endpoint">
                        <div><span class="method">GET</span> <span class="url">/events</span></div>
                        <div>获取所有事件列表（支持分页查询）</div>
                    </div>
                    
                    <div class="endpoint">
                        <div><span class="method">GET</span> <span class="url">/events/{id}</span></div>
                        <div>根据ID获取特定事件</div>
                    </div>
                    
                    <div class="endpoint">
                        <div><span class="method">POST</span> <span class="url">/events</span></div>
                        <div>创建新事件</div>
                    </div>
                    
                    <div class="endpoint">
                        <div><span class="method">GET</span> <span class="url">/organizers</span></div>
                        <div>获取所有组织者列表</div>
                    </div>
                    
                    <div class="endpoint">
                        <div><span class="method">GET</span> <span class="url">/organizers/{id}</span></div>
                        <div>根据ID获取特定组织者</div>
                    </div>
                    
                    <h2>💡 使用示例</h2>
                    
                    <p><strong>获取所有事件:</strong></p>
                    <div class="example">GET http://localhost:8081/events</div>
                    
                    <p><strong>分页查询事件:</strong></p>
                    <div class="example">GET http://localhost:8081/events?_limit=3&_page=1</div>
                    
                    <p><strong>获取特定事件:</strong></p>
                    <div class="example">GET http://localhost:8081/events/123</div>
                    
                    <p><strong>获取所有组织者:</strong></p>
                    <div class="example">GET http://localhost:8081/organizers</div>
                    
                    <p><strong>创建新事件:</strong></p>
                    <div class="example">
                        POST http://localhost:8081/events<br>
                        Content-Type: application/json<br><br>
                        {<br>
                        &nbsp;&nbsp;"title": "新活动",<br>
                        &nbsp;&nbsp;"description": "活动描述",<br>
                        &nbsp;&nbsp;"location": "活动地点",<br>
                        &nbsp;&nbsp;"date": "2023-12-25",<br>
                        &nbsp;&nbsp;"time": "14:00",<br>
                        &nbsp;&nbsp;"category": "社区活动",<br>
                        &nbsp;&nbsp;"petAllowed": true,<br>
                        &nbsp;&nbsp;"organizer": "组织者名称"<br>
                        }
                    </div>
                    
                    <hr style="margin: 30px 0; border: none; border-top: 1px solid #ddd;">
                    <p style="text-align: center; color: #666; font-size: 14px;">
                        🌟 Spring Boot Backend API | 支持 CORS | RESTful 设计
                    </p>
                </div>
            </body>
            </html>
            """;
            
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(htmlResponse);
    }
    
    @GetMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> apiInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", "Demo 331 Backend API");
        response.put("version", "1.0.0");
        response.put("status", "running");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("GET /events", "获取所有事件列表");
        endpoints.put("GET /events/{id}", "根据ID获取特定事件");
        endpoints.put("POST /events", "创建新事件");
        endpoints.put("GET /organizers", "获取所有组织者列表");
        endpoints.put("GET /organizers/{id}", "根据ID获取特定组织者");
        response.put("available_endpoints", endpoints);
        
        Map<String, String> examples = new HashMap<>();
        examples.put("获取事件", "GET http://localhost:8081/events");
        examples.put("分页查询", "GET http://localhost:8081/events?_limit=2&_page=1");
        examples.put("获取组织者", "GET http://localhost:8081/organizers");
        response.put("usage_examples", examples);
        
        return ResponseEntity.ok(response);
    }
}
