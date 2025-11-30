package com.example.rpa.controller;

import com.example.rpa.dto.BilibiliVideoUploadRequest;
import com.example.rpa.dto.TikTokVideoUploadRequest;
import com.example.rpa.dto.XiaohongshuVideoUploadRequest;
import com.example.rpa.dto.YoutubeVideoUploadRequest;
import com.example.rpa.service.BilibiliService;
import com.example.rpa.service.TikTokService;
import com.example.rpa.service.XiaohongshuService;
import com.example.rpa.service.YouTubeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api")
public class UploadController {

    @Autowired
    private YouTubeService youTubeService;

    @Autowired
    private TikTokService tikTokService;

    @Autowired
    private XiaohongshuService xiaohongshuService;

    @Autowired
    private BilibiliService bilibiliService;

    @PostMapping("/youtube/upload")
    public String uploadToYoutube(@RequestBody YoutubeVideoUploadRequest request) {
        log.info("Received YouTube upload request: {}", request);
        new Thread(() -> {
            try {
                youTubeService.uploadVideo(
                        request.getFilePath(),
                        request.getTitle(),
                        request.getDescription(),
                        request.getPlaylist(),
                        request.getVisibility(),
                        request.getPlaylist(),
                        request.getVisibility(),
                        request.getHashtags(),
                        true);
            } catch (Exception e) {
                log.error("Error uploading to YouTube", e);
            }
        }).start();

        return "Upload started! Check the browser window.";
    }

    @PostMapping("/tiktok/upload")
    public String uploadToTikTok(@RequestBody TikTokVideoUploadRequest request) {
        log.info("Received TikTok upload request: {}", request);
        new Thread(() -> {
            try {
                tikTokService.uploadVideo(
                        request.getFilePath(),
                        request.getTitle(),
                        request.getDescription(),
                        request.getVisibility(),
                        request.getVisibility(),
                        request.getHashtags(),
                        true);
            } catch (Exception e) {
                log.error("Error uploading to TikTok", e);
            }
        }).start();

        return "TikTok Upload started! Check the browser window.";
    }

    @PostMapping("/xiaohongshu/upload")
    public String uploadToXiaohongshu(@RequestBody XiaohongshuVideoUploadRequest request) {
        log.info("Received Xiaohongshu upload request: {}", request);
        new Thread(() -> {
            try {
                xiaohongshuService.uploadVideo(
                        request.getFilePath(),
                        request.getTitle(),
                        request.getDescription(),
                        request.getTitle(),
                        request.getDescription(),
                        request.getHashtags(),
                        true);
            } catch (Exception e) {
                log.error("Error uploading to Xiaohongshu", e);
            }
        }).start();

        return "Xiaohongshu Upload started! Check the browser window.";
    }

    @PostMapping("/bilibili/upload")
    public String uploadToBilibili(@RequestBody BilibiliVideoUploadRequest request) {
        log.info("Received Bilibili upload request: {}", request);
        new Thread(() -> {
            try {
                bilibiliService.uploadVideo(
                        request.getFilePath(),
                        request.getTitle(),
                        request.getDescription(),
                        request.getTitle(),
                        request.getDescription(),
                        request.getHashtags(),
                        true);
            } catch (Exception e) {
                log.error("Error uploading to Bilibili", e);
            }
        }).start();

        return "Bilibili Upload started! Check the browser window.";
    }

    @Autowired
    private com.example.rpa.service.MultiPlatformUploadService multiPlatformUploadService;

    @PostMapping("/batch/upload")
    public String batchUpload(@RequestBody com.example.rpa.dto.BatchUploadRequest request) {
        log.info("Received batch upload request: {}", request);
        new Thread(() -> {
            try {
                multiPlatformUploadService.uploadFromFolder(request);
            } catch (Exception e) {
                log.error("Error during batch upload", e);
            }
        }).start();

        return "Batch Upload started! Check the logs for progress.";
    }

}
