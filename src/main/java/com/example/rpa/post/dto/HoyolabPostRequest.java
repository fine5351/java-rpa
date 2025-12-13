package com.example.rpa.post.dto;

import lombok.Data;

import java.util.List;

@Data
public class HoyolabPostRequest {
    private String videoLink;
    private String title;
    private String description;
    private String circleName;
    private String categoryName;
    private List<String> topics;
}
