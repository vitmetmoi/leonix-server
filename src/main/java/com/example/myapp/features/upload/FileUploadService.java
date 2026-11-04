package com.example.myapp.features.upload;

import com.example.myapp.features.upload.dto.UploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/svg+xml"
    );

    public UploadResponse uploadImage(MultipartFile file) {
        log.info("Processing image upload: originalFilename={}, size={} bytes, contentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        if (file.isEmpty()) {
            log.warn("Upload rejected: file is empty");
            throw new IllegalArgumentException("Cannot upload an empty file");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Upload rejected: unsupported content type '{}'", contentType);
            throw new IllegalArgumentException("Only image files (JPEG, PNG, WEBP, GIF, SVG) are allowed");
        }

        try {
            Path targetFolder = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(targetFolder)) {
                Files.createDirectories(targetFolder);
            }

            String originalFilename = StringUtils.cleanPath(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.png"
            );
            String extension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalFilename.substring(dotIndex);
            }

            String generatedFileName = UUID.randomUUID().toString() + extension;
            Path targetLocation = targetFolder.resolve(generatedFileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Image successfully saved to: {}", targetLocation);

            String fileUrl = "/api/uploads/" + generatedFileName;

            return UploadResponse.builder()
                    .fileName(generatedFileName)
                    .fileUrl(fileUrl)
                    .contentType(contentType)
                    .size(file.getSize())
                    .build();

        } catch (IOException ex) {
            log.error("Failed to store uploaded file: {}", ex.getMessage(), ex);
            throw new RuntimeException("Could not store image file. Please try again!", ex);
        }
    }
}
