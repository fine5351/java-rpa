package com.example.rpa.dto;

public class XPostRequest {
    private String videoUrl;
    private String title;
    private String description;
    private java.util.List<String> hashtags;

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public java.util.List<String> getHashtags() {
        return hashtags;
    }

    public void setHashtags(java.util.List<String> hashtags) {
        this.hashtags = hashtags;
    }
}
