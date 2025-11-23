package com.example.rpa.dto;

import lombok.Data;

import java.util.List;

@Data
public class ThreadsPostRequest {
    private String content;
    private List<String> hashtags;
}
