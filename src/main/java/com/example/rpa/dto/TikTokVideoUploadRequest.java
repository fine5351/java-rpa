package com.example.rpa.dto;

public class TikTokVideoUploadRequest {
    private String filePath;
    private String caption;
    private String visibility; // PUBLIC, FRIENDS, PRIVATE

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }
}
