package com.example.cafe_manager_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.UUID;

@Service
public class UploadService {

    @Value("${app.supabase.anon-key}")
    private String supabaseAnonKey;

    private static final String SUPABASE_PROJECT_ID = "lkixxfwlqjcryeakeopc";
    private static final String BUCKET_NAME = "cafe-uploads";

    public String uploadFile(MultipartFile file, String folder) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File tải lên không được rỗng.");
        }

        // Clean file extension and name to generate a unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExt = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExt = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String uniqueFileName = UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + fileExt;
        String cleanedFolder = (folder == null || folder.trim().isEmpty()) ? "general" : folder.trim().toLowerCase();
        
        // Supabase Upload REST URL: POST /storage/v1/object/bucket/folder/filename
        String uploadUrl = String.format("https://%s.supabase.co/storage/v1/object/%s/%s/%s",
                SUPABASE_PROJECT_ID, BUCKET_NAME, cleanedFolder, uniqueFileName);

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(supabaseAnonKey);
            
            // Set contentType matching original file MIME type
            String contentType = file.getContentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = "image/jpeg";
            }
            headers.setContentType(MediaType.parseMediaType(contentType));

            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);

            // Execute raw POST binary upload
            ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                // Public access URL: https://[project].supabase.co/storage/v1/object/public/[bucket]/[folder]/[filename]
                return String.format("https://%s.supabase.co/storage/v1/object/public/%s/%s/%s",
                        SUPABASE_PROJECT_ID, BUCKET_NAME, cleanedFolder, uniqueFileName);
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Supabase trả về lỗi: " + response.getBody());
            }

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi đọc dữ liệu file tải lên: " + e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi kết nối hoặc quyền upload lên Supabase Storage: " + e.getMessage());
        }
    }
}
