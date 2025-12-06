package com.example.rpa.dto;

import lombok.Data;

import java.util.List;

@Data
public class XiaohongshuVideoUploadRequest {
    private String filePath;
    private String description;
    private String visibility;
    private List<String> hashtags;
}
