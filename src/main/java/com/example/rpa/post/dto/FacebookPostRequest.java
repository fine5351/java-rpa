package com.example.rpa.post.dto;

import lombok.Data;

import java.util.List;

@Data
public class FacebookPostRequest {
    private String videoUrl;
    private String title;
    private String description;
    private List<String> hashtags;
}
