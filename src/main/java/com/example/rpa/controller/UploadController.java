package com.example.rpa.controller;

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
    public String upload(@org.springframework.web.bind.annotation.RequestBody VideoUploadRequest request) {

        // Run in a separate thread to not block the HTTP response
        new Thread(() -> {
            youTubeService.uploadVideo(
                    request.getFilePath(),
                    request.getTitle(),
                    request.getDescription(),
                    request.getPlaylist(),
                    request.getVisibility());
        }).start();

        return "Upload started! Check the browser window.";
    }
}
