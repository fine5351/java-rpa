package com.example.rpa.dto;

import lombok.Data;

import java.util.List;

@Data
public class YoutubeVideoUploadRequest {
    private String filePath;
    private String title;
    private String description;
    private String playlist;
    private String visibility;
    private List<String> hashtags;
}
