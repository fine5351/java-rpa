package com.example.rpa.post.dto;

import lombok.Data;

import java.util.List;

@Data
public class ThreadsPostRequest {
    private String content;
    private List<String> hashtags;
}
