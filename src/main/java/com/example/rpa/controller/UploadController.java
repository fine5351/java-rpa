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

import com.example.rpa.dto.MultiPlatformVideoUploadRequest;

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
                        getFileNameWithoutExtension(request.getFilePath()),
                        request.getDescription(),
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
                        getFileNameWithoutExtension(request.getFilePath()),
                        request.getDescription(),
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
                        getFileNameWithoutExtension(request.getFilePath()),
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
                        getFileNameWithoutExtension(request.getFilePath()),
                        request.getDescription(),
                        request.getHashtags(),
                        true);
            } catch (Exception e) {
                log.error("Error uploading to Bilibili", e);
            }
        }).start();

        return "Bilibili Upload started! Check the browser window.";
    }

    @PostMapping("/multi/upload")
    public String uploadMultiPlatform(@RequestBody MultiPlatformVideoUploadRequest request) {
        log.info("Received Multi-Platform upload request: {}", request);
        new Thread(() -> {
            try {
                String title = getFileNameWithoutExtension(request.getFilePath());

                // 1. YouTube
                log.info("Starting YouTube upload...");
                boolean ytSuccess = youTubeService.uploadVideo(
                        request.getFilePath(),
                        title,
                        request.getDescription(),
                        request.getPlaylist(),
                        "PUBLIC", // Forced Public
                        request.getHashtags(),
                        false); // Close on success, keeping debugging simple for now or strictly follow
                                // sequential?
                                // "Manual verification" usually implies needed visibility, but "RPA" implies
                                // automation.
                                // If fail, we stop.

                if (!ytSuccess) {
                    log.error("YouTube upload failed. Stopping sequence.");
                    return;
                }

                // 2. Bilibili
                log.info("Starting Bilibili upload...");
                boolean biliSuccess = bilibiliService.uploadVideo(
                        request.getFilePath(),
                        title,
                        request.getDescription(),
                        request.getHashtags(),
                        false);

                if (!biliSuccess) {
                    log.error("Bilibili upload failed. Stopping sequence.");
                    return;
                }

                // 3. Xiaohongshu
                log.info("Starting Xiaohongshu upload...");
                boolean xhsSuccess = xiaohongshuService.uploadVideo(
                        request.getFilePath(),
                        title,
                        request.getDescription(),
                        request.getHashtags(),
                        false);

                if (!xhsSuccess) {
                    log.error("Xiaohongshu upload failed. Stopping sequence.");
                    return;
                }

                // 4. TikTok
                log.info("Starting TikTok upload...");
                boolean tiktokSuccess = tikTokService.uploadVideo(
                        request.getFilePath(),
                        title,
                        request.getDescription(),
                        "PUBLIC", // Forced Public
                        request.getHashtags(),
                        false);

                if (!tiktokSuccess) {
                    log.error("TikTok upload failed.");
                    return;
                }

                log.info("All platforms uploaded successfully!");

            } catch (Exception e) {
                log.error("Error during multi-platform upload sequence", e);
            }
        }).start();

        return "Multi-Platform Upload started! Check the console for progress.";
    }

    private String getFileNameWithoutExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        java.io.File file = new java.io.File(filePath);
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

}
