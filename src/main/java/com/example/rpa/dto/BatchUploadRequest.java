package com.example.rpa.dto;

import lombok.Data;

@Data
public class BatchUploadRequest {
    private String folderPath;
    private String description;
    private String playlist;
}
