package com.example.cafe_manager_api.controller;

import com.example.cafe_manager_api.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {

    @Autowired
    private UploadService uploadService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder) {
        
        String publicUrl = uploadService.uploadFile(file, folder);
        return ResponseEntity.ok(Map.of("url", publicUrl));
    }
}
