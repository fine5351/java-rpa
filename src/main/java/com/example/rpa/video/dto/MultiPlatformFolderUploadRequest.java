package com.example.rpa.video.dto;

import lombok.Data;
import java.util.List;

@Data
public class MultiPlatformFolderUploadRequest {
    private String folderPath;
    private String description;
    private String playlist;
    private List<String> hashtags;
}
