package com.example.rpa.dto;

import java.util.List;
import java.util.Set;

public class IntegratedVideoUploadRequest {
    private String filePath;
    private String title;
    private String description;
    private List<String> hashtags;
    private String visibility; // "PUBLIC", "PRIVATE", "UNLISTED"

    // YouTube specific
    private String playlist;

    // Control
    private Set<String> platforms; // e.g., "YOUTUBE", "TIKTOK", "XIAOHONGSHU", "BILIBILI"

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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

    public List<String> getHashtags() {
        return hashtags;
    }

    public void setHashtags(List<String> hashtags) {
        this.hashtags = hashtags;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getPlaylist() {
        return playlist;
    }

    public void setPlaylist(String playlist) {
        this.playlist = playlist;
    }

    public Set<String> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(Set<String> platforms) {
        this.platforms = platforms;
    }
}
