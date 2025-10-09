package com.example.demo331bacnkend.controller;

import com.example.demo331bacnkend.util.CloudStorageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.ServletException;
import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class BucketController {

    private final CloudStorageHelper cloudStorageHelper;

    @PostMapping("/uploadFile")
    public ResponseEntity<?> uploadFile(@RequestPart(value = "file") MultipartFile file)
            throws IOException, ServletException {
        return ResponseEntity.ok(
                this.cloudStorageHelper.getImageUrl(file, "se331lab10.firebasestorage.app")
        );
    }

    @PostMapping("/uploadImage")
    public ResponseEntity<?> uploadFileComponent(@RequestPart(value = "image") MultipartFile file)
            throws IOException, ServletException {
        return ResponseEntity.ok(
                this.cloudStorageHelper.getStorageFileDto(file, "se331lab10.firebasestorage.app")
        );
    }
}


