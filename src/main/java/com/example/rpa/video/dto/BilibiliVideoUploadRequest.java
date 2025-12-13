package com.example.rpa.video.dto;

import lombok.Data;

import java.util.List;

@Data
public class BilibiliVideoUploadRequest {
    private String filePath;
    private String description;
    private String visibility;
    private List<String> hashtags;
}
