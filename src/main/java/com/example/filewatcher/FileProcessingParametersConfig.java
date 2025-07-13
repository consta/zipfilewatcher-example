package com.example.filewatcher;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "filewatcher")
@Data
public class FileProcessingParametersConfig {
    private String uploadDir;
    private String destinationBase;
    private String rejectedDir;
    private Integer staleFileWaitMs;

    List<FileProcessingParameters> parameters;
}
