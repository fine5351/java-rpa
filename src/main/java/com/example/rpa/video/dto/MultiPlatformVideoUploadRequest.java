package com.example.rpa.video.dto;

import lombok.Data;
import java.util.List;

@Data
public class MultiPlatformVideoUploadRequest {
    private String filePath;
    private String description;
    private String playlist;
    private List<String> hashtags;
}
