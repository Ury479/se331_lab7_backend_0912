package com.example.demo331bacnkend.util;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.ServletException;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class CloudStorageHelper {

    private static Storage storage = null;

    static {
        InputStream serviceAccount = null;
        try {
            serviceAccount = new ClassPathResource("se331lab10-firebase-adminsdk-fbsvc-d273d10d80.json").getInputStream();
            storage = StorageOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId("se331lab10")
                    .build()
                    .getService();
            System.out.println("✅ Firebase Storage initialized successfully!");
        } catch (IOException e) {
            System.err.println("❌ ERROR: Failed to initialize Firebase Storage!");
            System.err.println("Please ensure 'se331lab10-firebase-adminsdk-fbsvc-d0dc24989f.json' exists in src/main/resources/");
            e.printStackTrace();
            // 不抛出异常，但 storage 将保持为 null
        }
    }

    public String uploadFile(MultipartFile filePart, final String bucketName) throws IOException {
        if (storage == null) {
            throw new IOException("Firebase Storage is not initialized. Please check your Firebase credentials file.");
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HHmmssSSS");
        String dtString = sdf.format(new Date());
        final String fileName = dtString + "-" + filePart.getOriginalFilename();

        InputStream is = filePart.getInputStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] readBuf = new byte[4096];
        while (is.available() > 0) {
            int bytesRead = is.read(readBuf);
            os.write(readBuf, 0, bytesRead);
        }

        BlobInfo blobInfo = storage.create(
                BlobInfo.newBuilder(bucketName, fileName)
                        .setAcl(new ArrayList<>(Arrays.asList(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER))))
                        .setContentType(filePart.getContentType())
                        .build(),
                os.toByteArray()
        );

        return blobInfo.getMediaLink();
    }

    public String getImageUrl(MultipartFile file, final String bucket) throws IOException, ServletException {
        final String fileName = file.getOriginalFilename();

        if (fileName != null && !fileName.isEmpty() && fileName.contains(".")) {
            final String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
            String[] allowedExt = {"jpg", "jpeg", "png", "gif"};
            for (String s : allowedExt) {
                if (extension.equalsIgnoreCase(s)) {
                    return this.uploadFile(file, bucket);
                }
            }
        }

        throw new ServletException("file must be an image");
    }

    public StorageFileDto getStorageFileDto(MultipartFile file, final String bucket) throws IOException, ServletException {
        final String fileName = file.getOriginalFilename();
        // Check extension of file
        if (fileName != null && !fileName.isEmpty() && fileName.contains(".")) {
            final String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
            String[] allowedExt = {"jpg", "jpeg", "png", "gif"};
            for (String s : allowedExt) {
                if (extension.equals(s)) {
                    String urlName = this.uploadFile(file, bucket);
                    return StorageFileDto.builder()
                            .name(urlName)
                            .build();
                }
            }
        }
        throw new ServletException("file must be an image");
    }
}


