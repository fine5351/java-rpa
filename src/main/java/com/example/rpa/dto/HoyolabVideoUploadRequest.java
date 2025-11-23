package com.example.rpa.dto;

import lombok.Data;

import java.util.List;

@Data
public class HoyolabVideoUploadRequest {
    private String videoLink;
    private String title;
    private String description;
    private String category;
    private List<String> hashtags;
}
