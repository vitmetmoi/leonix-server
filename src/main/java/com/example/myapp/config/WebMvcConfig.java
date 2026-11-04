package com.example.myapp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        log.info("Mapping static resource handler /uploads/** to location: {}", uploadPath);

        // Ensure upload directory exists
        File uploadFolder = new File(uploadDir);
        if (!uploadFolder.exists()) {
            boolean created = uploadFolder.mkdirs();
            log.info("Upload directory created at {}: {}", uploadFolder.getAbsolutePath(), created);
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);

        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
