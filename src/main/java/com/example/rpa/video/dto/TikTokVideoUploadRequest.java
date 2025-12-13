package com.example.rpa.video.dto;

import lombok.Data;

import java.util.List;

@Data
public class TikTokVideoUploadRequest {
    private String filePath;
    private String description;
    private String visibility;
    private List<String> hashtags;
}
