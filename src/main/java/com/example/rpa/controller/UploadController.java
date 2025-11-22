package com.example.rpa.controller;

import com.example.rpa.dto.YoutubeVideoUploadRequest;
import com.example.rpa.service.YouTubeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UploadController {

    @Autowired
    private YouTubeService youTubeService;

    @PostMapping("/youtube/upload")
    public String uploadToYoutube(
            @org.springframework.web.bind.annotation.RequestBody YoutubeVideoUploadRequest request) {

        // Run in a separate thread to not block the HTTP response
        new Thread(() -> {
            youTubeService.uploadVideo(
                    request.getFilePath(),
                    request.getTitle(),
                    request.getDescription(),
                    request.getPlaylist(),
                    request.getVisibility(),
                    request.getHashtags());
        }).start();

        return "Upload started! Check the browser window.";
    }

    @Autowired
    private com.example.rpa.service.TikTokService tikTokService;

    @PostMapping("/tiktok/upload")
    public String uploadToTikTok(
            @org.springframework.web.bind.annotation.RequestBody com.example.rpa.dto.TikTokVideoUploadRequest request) {

        new Thread(() -> {
            tikTokService.uploadVideo(
                    request.getFilePath(),
                    request.getTitle(),
                    request.getDescription(),
                    request.getVisibility(),
                    request.getHashtags());
        }).start();

        return "TikTok Upload started! Check the browser window.";
    }

    @Autowired
    private com.example.rpa.service.XiaohongshuService xiaohongshuService;

    @PostMapping("/xiaohongshu/upload")
    public String uploadToXiaohongshu(
            @org.springframework.web.bind.annotation.RequestBody com.example.rpa.dto.XiaohongshuVideoUploadRequest request) {

        new Thread(() -> {
            xiaohongshuService.uploadVideo(
                    request.getFilePath(),
                    request.getTitle(),
                    request.getDescription(),
                    request.getVisibility(),
                    request.getHashtags());
        }).start();

        return "Xiaohongshu Upload started! Check the browser window.";
    }
}
