package com.example.rpa.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class IntegratedVideoUploadRequest {
    private String filePath;
    private String title;
    private String description;
    private String playlist;
    private String visibility;
    private List<String> hashtags;
    private Set<String> platforms;
}
