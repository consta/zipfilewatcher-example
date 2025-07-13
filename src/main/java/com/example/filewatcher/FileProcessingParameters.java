package com.example.filewatcher;

import java.util.regex.Pattern;

import lombok.Data;

@Data
public class FileProcessingParameters {
    private Pattern pattern;
    private Integer fileCount;
    private String destination;
}
