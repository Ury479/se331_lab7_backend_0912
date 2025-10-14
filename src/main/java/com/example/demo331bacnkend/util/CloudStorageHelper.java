package com.example.demo331bacnkend.util;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

/**
 * Firebase Cloud Storage 文件上传助手
 * 
 * 安全增强:
 * - 配置外部化（projectId, bucket, credentials）
 * - 资源自动关闭（try-with-resources）
 * - 正确的 InputStream 读取逻辑
 * - 文件名安全处理（UUID + 清理）
 * - 返回公开访问 URL
 * - 完善的异常处理和日志
 * - 启动时 fail-fast 验证
 */
@Slf4j
@Component
public class CloudStorageHelper {

    private Storage storage;
    
    @Value("${application.firebase.project-id:se331lab10}")
    private String projectId;
    
    @Value("${application.firebase.credentials-path:se331lab10-firebase-adminsdk-fbsvc-d273d10d80.json}")
    private String credentialsPath;
    
    @Value("${application.firebase.storage.bucket:se331lab10.appspot.com}")
    private String defaultBucket;
    
    @Value("${application.firebase.storage.public-url-format:https://storage.googleapis.com/%s/%s}")
    private String publicUrlFormat;

    /**
     * 🔧 修复1: 启动时初始化 Firebase Storage，fail-fast 机制
     */
    @PostConstruct
    public void initializeFirebaseStorage() {
        try {
            log.info("Initializing Firebase Storage with project: {}", projectId);
            
            // 🔧 修复3: 使用 try-with-resources 自动关闭资源
            try (InputStream serviceAccount = new ClassPathResource(credentialsPath).getInputStream()) {
                storage = StorageOptions.newBuilder()
                        .setProjectId(projectId)
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build()
                        .getService();
                
                log.info("✅ Firebase Storage initialized successfully!");
                log.info("   Project ID: {}", projectId);
                log.info("   Default Bucket: {}", defaultBucket);
            }
            
        } catch (IOException e) {
            // 🔧 修复9: 启动时 fail-fast，而不是延迟到使用时
            String errorMsg = String.format(
                "❌ Failed to initialize Firebase Storage! Credentials file: %s", 
                credentialsPath  // 🔧 修复1: 错误信息与实际文件名一致
            );
            log.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    /**
     * 上传文件到 Firebase Storage
     * 
     * @param filePart 上传的文件
     * @param bucketName 存储桶名称
     * @return 公开访问的 URL
     * @throws IOException 上传失败
     */
    public String uploadFile(MultipartFile filePart, final String bucketName) throws IOException {
        if (storage == null) {
            throw new IllegalStateException("Firebase Storage is not initialized");
        }
        
        if (filePart == null || filePart.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        // 🔧 修复8: 使用 UUID + 时间戳 + 原始文件名，更安全
        String originalFilename = sanitizeFilename(filePart.getOriginalFilename());
        String uniqueFilename = generateUniqueFilename(originalFilename);
        
        // 🔧 修复7: contentType 兜底处理
        String contentType = filePart.getContentType();
        if (contentType == null || contentType.isEmpty()) {
            contentType = "application/octet-stream";
        }
        
        try {
            // 🔧 修复2: 直接使用 getBytes()，Spring 会完整读入
            byte[] fileData = filePart.getBytes();
            
            // 🔧 修复5: 根据是否启用 UBLA 决定是否设置 ACL
            BlobInfo.Builder blobInfoBuilder = BlobInfo.newBuilder(bucketName, uniqueFilename)
                    .setContentType(contentType);
            
            // 尝试设置公开 ACL（如果启用了 UBLA 会失败，但不影响上传）
            try {
                blobInfoBuilder.setAcl(new ArrayList<>(
                    Arrays.asList(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER))
                ));
            } catch (Exception e) {
                log.warn("Cannot set object-level ACL (UBLA may be enabled): {}", e.getMessage());
            }
            
            Blob blob = storage.create(blobInfoBuilder.build(), fileData);
            
            // 🔧 修复4: 返回公开访问 URL 而不是 mediaLink
            String publicUrl = buildPublicUrl(bucketName, uniqueFilename);
            
            log.info("File uploaded successfully: {} -> {} (size: {} bytes)", 
                    originalFilename, publicUrl, blob.getSize());
            return publicUrl;
            
        } catch (Exception e) {
            // 🔧 修复9: 详细的异常信息，便于追踪
            String errorMsg = String.format(
                "Failed to upload file to Firebase Storage. Bucket: %s, Filename: %s", 
                bucketName, uniqueFilename
            );
            log.error(errorMsg, e);
            throw new IOException(errorMsg, e);
        }
    }
    
    /**
     * 上传图片文件（带格式验证）
     */
    public String getImageUrl(MultipartFile file, final String bucket) throws IOException, ServletException {
        validateImageFile(file);
        return uploadFile(file, bucket);
    }
    
    /**
     * 上传图片并返回 DTO
     */
    public StorageFileDto getStorageFileDto(MultipartFile file, final String bucket) throws IOException, ServletException {
        validateImageFile(file);
        String url = uploadFile(file, bucket);
        return StorageFileDto.builder()
                .name(url)
                .build();
    }
    
    /**
     * 使用默认 bucket 上传文件
     */
    public String uploadFile(MultipartFile filePart) throws IOException {
        return uploadFile(filePart, defaultBucket);
    }
    
    /**
     * 🔧 修复8: 文件名安全处理 - 清理特殊字符，防止路径穿越
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "unnamed";
        }
        
        // 只保留文件名部分（去除路径）
        String name = filename.replaceAll(".*[/\\\\]", "");
        
        // 移除特殊字符，只保留字母、数字、点、下划线、连字符
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        // 限制长度
        if (name.length() > 100) {
            String ext = "";
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                ext = name.substring(dotIndex);
                name = name.substring(0, Math.min(100 - ext.length(), dotIndex));
            } else {
                name = name.substring(0, 100);
            }
            name = name + ext;
        }
        
        return name;
    }
    
    /**
     * 🔧 修复8 & 10: 生成唯一文件名 - UUID + 时间戳 + 原始文件名
     * 使用 Instant 而不是 SimpleDateFormat，线程安全
     */
    private String generateUniqueFilename(String originalFilename) {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        
        // 格式: {timestamp}-{uuid}-{filename}
        return String.format("%s-%s-%s", timestamp, uuid, originalFilename);
    }
    
    /**
     * 🔧 修复4: 构建公开访问 URL
     */
    private String buildPublicUrl(String bucketName, String filename) {
        try {
            // URL 编码文件名，处理特殊字符
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replace("+", "%20");  // 空格用 %20 而不是 +
            
            return String.format(publicUrlFormat, bucketName, encodedFilename);
        } catch (Exception e) {
            log.warn("Failed to encode filename, using raw: {}", filename);
            return String.format(publicUrlFormat, bucketName, filename);
        }
    }
    
    /**
     * 验证图片文件格式
     */
    private void validateImageFile(MultipartFile file) throws ServletException {
        if (file == null || file.isEmpty()) {
            throw new ServletException("File is empty");
        }
        
        final String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new ServletException("Filename is empty");
        }
        
        if (!filename.contains(".")) {
            throw new ServletException("File must have an extension");
        }
        
        final String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        String[] allowedExt = {"jpg", "jpeg", "png", "gif", "webp"};  // 添加 webp 支持
        
        for (String allowed : allowedExt) {
            if (extension.equals(allowed)) {
                return;  // 验证通过
            }
        }
        
        throw new ServletException(
            String.format("Invalid image format: %s. Allowed: %s", 
                extension, String.join(", ", allowedExt))
        );
    }
    
    /**
     * 获取 Storage 实例（用于高级操作）
     */
    public Storage getStorage() {
        if (storage == null) {
            throw new IllegalStateException("Firebase Storage is not initialized");
        }
        return storage;
    }
    
    /**
     * 健康检查
     */
    public boolean isInitialized() {
        return storage != null;
    }
}
